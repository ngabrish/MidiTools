package miditools.core.midi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import miditools.core.note.NoteClip;
import miditools.core.note.NoteEvent;

/** Reads the notes of a Standard MIDI File. */
public final class MidiFileReader {

    private static final int META_TRACK_NAME = 0x03;
    private static final int META_TIME_SIGNATURE = 0x58;

    private MidiFileReader() {}

    /**
     * Reads one clip for each track and channel pair that has notes, in track sequence and
     * then in channel sequence.
     *
     * <p>All clips have the same length: the length of the file, increased to the end of
     * its last bar. The bar length is from the first time signature of the file, or 4/4 if
     * the file has none. One beat is a quarter note.
     *
     * @throws IOException if the file cannot be read, is not a MIDI file, or uses SMPTE
     *                     time, which has no beats
     */
    public static List<NoteClip> read(Path midiFile) throws IOException {
        try {
            return clips(MidiSystem.getSequence(midiFile.toFile()), midiFile.toString());
        } catch (InvalidMidiDataException e) {
            throw new IOException("Not a MIDI file that Java can read: " + midiFile + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Reads the clips of MIDI file data that is not in a file, for example an upload. The
     * result is the same as {@link #read(Path)} gives for a file with these bytes.
     *
     * @param data the bytes of a Standard MIDI File
     * @param name a name for the data in error messages, for example the file name
     * @throws IOException if the data is not a MIDI file, or uses SMPTE time
     */
    public static List<NoteClip> read(byte[] data, String name) throws IOException {
        try {
            return clips(MidiSystem.getSequence(new ByteArrayInputStream(data)), name);
        } catch (InvalidMidiDataException e) {
            throw new IOException("Not a MIDI file that Java can read: " + name + " (" + e.getMessage() + ")", e);
        }
    }

    private static List<NoteClip> clips(Sequence sequence, String source) throws IOException {
        if (sequence.getDivisionType() != Sequence.PPQ) {
            throw new IOException("The MIDI file uses SMPTE time and has no beats: " + source);
        }
        double ticksPerBeat = sequence.getResolution();
        Track[] tracks = sequence.getTracks();

        double barBeats = 4.0;
        long signatureTick = Long.MAX_VALUE;
        for (Track track : tracks) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof MetaMessage meta && meta.getType() == META_TIME_SIGNATURE
                        && meta.getData().length >= 2 && event.getTick() < signatureTick) {
                    signatureTick = event.getTick();
                    barBeats = (meta.getData()[0] & 0xFF) * 4.0 / (1 << (meta.getData()[1] & 0xFF));
                }
            }
        }
        double fileBeats = sequence.getTickLength() / ticksPerBeat;
        double lengthBeats = barBeats > 0 ? Math.ceil(fileBeats / barBeats - 1e-9) * barBeats : fileBeats;

        List<NoteClip> clips = new ArrayList<>();
        for (int t = 0; t < tracks.length; t++) {
            Track track = tracks[t];
            String trackName = null;
            // channel -> notes
            Map<Integer, List<NoteEvent>> notes = new TreeMap<>();
            // start tick and velocity of the note that is on, for each channel and note number
            long[][] startTick = new long[16][128];
            int[][] velocity = new int[16][128];

            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage meta) {
                    if (meta.getType() == META_TRACK_NAME && trackName == null) {
                        trackName = new String(meta.getData(), StandardCharsets.ISO_8859_1).trim();
                    }
                    continue;
                }
                if (!(message instanceof ShortMessage sm)) {
                    continue;
                }
                int command = sm.getCommand();
                boolean on = command == ShortMessage.NOTE_ON && sm.getData2() > 0;
                boolean off = command == ShortMessage.NOTE_OFF
                        || (command == ShortMessage.NOTE_ON && sm.getData2() == 0);
                if (!on && !off) {
                    continue;
                }
                int channel = sm.getChannel();
                int number = sm.getData1();
                if (velocity[channel][number] > 0) {
                    // A note off, or a new note on for a note that is on, ends the note.
                    notes.computeIfAbsent(channel, k -> new ArrayList<>()).add(note(
                            startTick[channel][number], event.getTick(), number, velocity[channel][number], ticksPerBeat));
                    velocity[channel][number] = 0;
                }
                if (on) {
                    startTick[channel][number] = event.getTick();
                    velocity[channel][number] = sm.getData2();
                }
            }
            // A note with no note off ends at the end of the track.
            for (int channel = 0; channel < 16; channel++) {
                for (int number = 0; number < 128; number++) {
                    if (velocity[channel][number] > 0) {
                        notes.computeIfAbsent(channel, k -> new ArrayList<>()).add(note(
                                startTick[channel][number], track.ticks(), number, velocity[channel][number], ticksPerBeat));
                    }
                }
            }

            for (Map.Entry<Integer, List<NoteEvent>> e : notes.entrySet()) {
                List<NoteEvent> list = e.getValue();
                list.sort((a, b) -> Double.compare(a.startBeats(), b.startBeats()));
                String name = (trackName == null || trackName.isEmpty() ? "Track " + (t + 1) : trackName)
                        + (notes.size() > 1 ? " (MIDI channel " + (e.getKey() + 1) + ")" : "");
                clips.add(new NoteClip(name, lengthBeats, list));
            }
        }
        return clips;
    }

    private static NoteEvent note(long startTick, long endTick, int number, int velocity, double ticksPerBeat) {
        // A note of zero ticks gets one tick, because a note must have a length.
        long ticks = Math.max(1, endTick - startTick);
        return new NoteEvent(startTick / ticksPerBeat, ticks / ticksPerBeat, number, velocity);
    }
}
