package blackseq.format;

import static blackseq.format.SeqLayout.CENTS_PER_SEMITONE;
import static blackseq.format.SeqLayout.FILE_HEADER;
import static blackseq.format.SeqLayout.FILE_SIZE;
import static blackseq.format.SeqLayout.FIRST_TRACK;
import static blackseq.format.SeqLayout.GATE_ON_BIT;
import static blackseq.format.SeqLayout.MASTER_TRACK_LENGTH;
import static blackseq.format.SeqLayout.MAX_NOTE_CENTS;
import static blackseq.format.SeqLayout.STEP_COUNT;
import static blackseq.format.SeqLayout.STEP_GATE_FLAGS;
import static blackseq.format.SeqLayout.STEP_GATE_LENGTH;
import static blackseq.format.SeqLayout.STEP_NOTE;
import static blackseq.format.SeqLayout.TRACK_FRAME;
import static blackseq.format.SeqLayout.TRACK_STRIDE;

import blackseq.model.Pattern;
import blackseq.model.Step;
import blackseq.model.Track;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One pattern file of the Black Sequencer.
 *
 * <p>An instance keeps the full bytes of a file that the module made. {@link #withPattern}
 * returns a copy in which only the bytes of known fields are different. All other bytes
 * stay as they are. A field is written only when its new value is different from the
 * value that the bytes decode to, so {@code file.withPattern(file.pattern())} is equal to
 * {@code file}.
 *
 * <p>Instances are immutable.
 */
public final class SeqFile {

    private static final String BLANK_RESOURCE = "blank.SEQ";

    private final byte[] bytes;

    private SeqFile(byte[] bytes) {
        this.bytes = bytes;
    }

    /** Reads and checks a pattern file. */
    public static SeqFile read(Path file) throws IOException, SeqFormatException {
        return fromBytes(Files.readAllBytes(file));
    }

    /**
     * Checks the bytes of a pattern file. The check refuses each file that is different
     * from the samples in a constant part, and each value that the model cannot hold.
     */
    public static SeqFile fromBytes(byte[] data) throws SeqFormatException {
        check(data);
        return new SeqFile(data.clone());
    }

    /**
     * A new blank pattern: the bundled file that Nate's module (firmware 1.06) saved for a
     * pattern with no edits.
     */
    public static SeqFile blank() {
        try (InputStream in = SeqFile.class.getResourceAsStream(BLANK_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("resource not found: " + BLANK_RESOURCE);
            }
            return fromBytes(in.readAllBytes());
        } catch (IOException | SeqFormatException e) {
            throw new IllegalStateException("the bundled blank pattern is not valid", e);
        }
    }

    /** Decodes the known fields. Each track has 64 steps. */
    public Pattern pattern() {
        List<Track> tracks = new ArrayList<>();
        for (int channel = 0; channel < Pattern.CHANNELS; channel++) {
            List<Step> steps = new ArrayList<>();
            for (int s = 0; s < STEP_COUNT; s++) {
                steps.add(decodeStep(SeqLayout.step(channel, s)));
            }
            tracks.add(new Track(u8(SeqLayout.lastStep(channel)) + 1, steps));
        }
        return new Pattern(tracks);
    }

    /**
     * Returns a copy with the known fields of {@code p}. A track in {@code p} with fewer
     * than 64 steps changes only those steps.
     */
    public SeqFile withPattern(Pattern p) {
        byte[] out = bytes.clone();
        Pattern current = pattern();
        for (int channel = 0; channel < Pattern.CHANNELS; channel++) {
            Track oldTrack = current.tracks().get(channel);
            Track newTrack = p.tracks().get(channel);
            if (newTrack.length() != oldTrack.length()) {
                out[SeqLayout.lastStep(channel)] = (byte) (newTrack.length() - 1);
            }
            for (int s = 0; s < newTrack.steps().size(); s++) {
                encodeStep(out, SeqLayout.step(channel, s), oldTrack.steps().get(s),
                        newTrack.steps().get(s));
            }
        }
        return new SeqFile(out);
    }

    /**
     * The master track length, 1 to 64: the number of steps after which the module goes to
     * the next pattern. It is one value for the pattern, not for a track.
     */
    public int masterTrackLength() {
        return u8(MASTER_TRACK_LENGTH);
    }

    /** Returns a copy with a different master track length, 1 to 64. */
    public SeqFile withMasterTrackLength(int length) {
        if (length < 1 || length > STEP_COUNT) {
            throw new IllegalArgumentException("master track length must be 1 to 64: " + length);
        }
        byte[] out = bytes.clone();
        out[MASTER_TRACK_LENGTH] = (byte) length;
        return new SeqFile(out);
    }

    /** A copy of the file bytes. */
    public byte[] toBytes() {
        return bytes.clone();
    }

    /**
     * Writes a new file. This method never replaces a file: it throws
     * {@link java.nio.file.FileAlreadyExistsException} if the file exists.
     */
    public void write(Path file) throws IOException {
        Files.write(file, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SeqFile f && Arrays.equals(bytes, f.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    private Step decodeStep(int offset) {
        return new Step(
                (u8(offset + STEP_GATE_FLAGS) & GATE_ON_BIT) != 0,
                centsToMidi(i32(bytes, offset + STEP_NOTE)),
                u8(offset + STEP_GATE_LENGTH) / 100.0);
    }

    private static void encodeStep(byte[] out, int offset, Step oldStep, Step newStep) {
        if (newStep.midiNote() != oldStep.midiNote()) {
            int cents = newStep.midiNote() * CENTS_PER_SEMITONE;
            out[offset + STEP_NOTE] = (byte) cents;
            out[offset + STEP_NOTE + 1] = (byte) (cents >> 8);
            out[offset + STEP_NOTE + 2] = (byte) (cents >> 16);
            out[offset + STEP_NOTE + 3] = (byte) (cents >> 24);
        }
        if (percent(newStep.gateLength()) != percent(oldStep.gateLength())) {
            out[offset + STEP_GATE_LENGTH] = (byte) percent(newStep.gateLength());
        }
        if (newStep.gateOn() != oldStep.gateOn()) {
            int flags = out[offset + STEP_GATE_FLAGS] & 0xFF;
            out[offset + STEP_GATE_FLAGS] =
                    (byte) (newStep.gateOn() ? flags | GATE_ON_BIT : flags & ~GATE_ON_BIT);
        }
    }

    private static int percent(double gateLength) {
        return (int) Math.round(gateLength * 100.0);
    }

    /** A value that is not on a semitone (a microtonal note) goes to the nearest note. */
    private static int centsToMidi(int cents) {
        return Math.round(cents / (float) CENTS_PER_SEMITONE);
    }

    private int u8(int offset) {
        return bytes[offset] & 0xFF;
    }

    private static int i32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | (data[offset + 1] & 0xFF) << 8
                | (data[offset + 2] & 0xFF) << 16
                | (data[offset + 3] & 0xFF) << 24;
    }

    private static void check(byte[] data) throws SeqFormatException {
        if (data.length != FILE_SIZE) {
            throw new SeqFormatException(
                    "file size is " + data.length + " bytes; a pattern file has " + FILE_SIZE);
        }
        expect(data, 0, FILE_HEADER, "file header (length prefix and format version 5)");
        int master = data[MASTER_TRACK_LENGTH] & 0xFF;
        if (master < 1 || master > STEP_COUNT) {
            throw new SeqFormatException("master track length is " + master + "; it must be 1 to 64");
        }
        for (int channel = 0; channel < Pattern.CHANNELS; channel++) {
            String name = "CH" + (channel + 1);
            expect(data, FIRST_TRACK + TRACK_STRIDE * channel, TRACK_FRAME, "track header of " + name);
            int last = data[SeqLayout.lastStep(channel)] & 0xFF;
            if (last + 1 < Track.MIN_LENGTH || last + 1 > STEP_COUNT) {
                throw new SeqFormatException(
                        name + ": track length is " + (last + 1) + "; it must be 2 to 64");
            }
            for (int s = 0; s < STEP_COUNT; s++) {
                int offset = SeqLayout.step(channel, s);
                int cents = i32(data, offset + STEP_NOTE);
                if (cents < 0 || cents > MAX_NOTE_CENTS) {
                    throw new SeqFormatException(name + " step " + (s + 1) + ": note value " + cents
                            + " is outside 0 to " + MAX_NOTE_CENTS);
                }
                int gate = data[offset + STEP_GATE_LENGTH] & 0xFF;
                if (gate > 100) {
                    throw new SeqFormatException(
                            name + " step " + (s + 1) + ": gate length " + gate + " is more than 100");
                }
            }
        }
    }

    private static void expect(byte[] data, int offset, int[] expected, String what)
            throws SeqFormatException {
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) {
                throw new SeqFormatException("unknown " + what + " at offset " + offset + ": "
                        + HexDump.hex(data, offset, expected.length));
            }
        }
    }
}
