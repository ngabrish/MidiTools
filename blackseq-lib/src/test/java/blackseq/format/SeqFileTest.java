package blackseq.format;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blackseq.model.Pattern;
import blackseq.model.Step;
import blackseq.model.Track;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Codec tests against the files from the module. NN.SEQ in RETEST is pattern NN + 1 of
 * samples/blackseq/README.md.
 */
class SeqFileTest {

    private static final Path SAMPLES = Path.of("..", "samples", "blackseq", "PROJECTS");
    private static final Path RETEST = SAMPLES.resolve("RETEST").resolve("00");

    private static Path sample(int fileNumber) {
        return RETEST.resolve(String.format("%02d.SEQ", fileNumber));
    }

    private static List<Path> allModuleFiles() throws IOException {
        try (var files = Files.walk(SAMPLES)) {
            return files.filter(p -> p.toString().endsWith(".SEQ")).sorted().toList();
        }
    }

    /** A copy of the pattern with one step of one channel changed. Channel and step start at 1. */
    private static Pattern withStep(Pattern p, int channel, int step, UnaryOperator<Step> change) {
        List<Track> tracks = new ArrayList<>(p.tracks());
        Track track = tracks.get(channel - 1);
        List<Step> steps = new ArrayList<>(track.steps());
        steps.set(step - 1, change.apply(steps.get(step - 1)));
        tracks.set(channel - 1, new Track(track.length(), steps));
        return new Pattern(tracks);
    }

    private static Pattern withLength(Pattern p, int channel, int length) {
        List<Track> tracks = new ArrayList<>(p.tracks());
        tracks.set(channel - 1, new Track(length, tracks.get(channel - 1).steps()));
        return new Pattern(tracks);
    }

    private static UnaryOperator<Step> note(int midiNote) {
        return s -> new Step(s.gateOn(), midiNote, s.gateLength());
    }

    private static UnaryOperator<Step> gateLength(double fraction) {
        return s -> new Step(s.gateOn(), s.midiNote(), fraction);
    }

    private static UnaryOperator<Step> merged() {
        return s -> new Step(s.gateOn(), s.midiNote(), s.gateLength(), true);
    }

    private static void assertSameBytes(int fileNumber, SeqFile made) throws IOException {
        assertArrayEquals(Files.readAllBytes(sample(fileNumber)), made.toBytes(),
                "bytes of sample " + fileNumber);
    }

    // ----- read and write back -----

    @Test
    void eachModuleFileReadsAndWritesBackByteForByte() throws Exception {
        List<Path> files = allModuleFiles();
        assertEquals(22, files.size(), "15 RETEST files and 7 FACTORY files");
        for (Path f : files) {
            byte[] original = Files.readAllBytes(f);
            SeqFile file = SeqFile.read(f);
            assertArrayEquals(original, file.toBytes(), f.toString());
            assertArrayEquals(original, file.withPattern(file.pattern()).toBytes(),
                    "decode then encode: " + f);
        }
    }

    @Test
    void writeMakesAFileWithTheSameBytes(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("00.SEQ");
        SeqFile.read(sample(3)).write(out);
        assertArrayEquals(Files.readAllBytes(sample(3)), Files.readAllBytes(out));
    }

    @Test
    void writeNeverReplacesAFile(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("00.SEQ");
        Files.write(out, new byte[] {1, 2, 3});
        assertThrows(FileAlreadyExistsException.class, () -> SeqFile.blank().write(out));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(out));
    }

    // ----- blank() plus one change is equal to the sample -----

    @Test
    void blankIsTheBlankSample() throws Exception {
        assertSameBytes(0, SeqFile.blank());
        assertSameBytes(13, SeqFile.blank());
    }

    @Test
    void blankWithOneNoteIsEqualToTheNoteSamples() throws Exception {
        SeqFile blank = SeqFile.blank();
        Pattern p = blank.pattern();
        assertSameBytes(1, blank.withPattern(withStep(p, 1, 1, note(48))));    // C-4
        assertSameBytes(2, blank.withPattern(withStep(p, 1, 1, note(72))));    // C-6
        assertSameBytes(3, blank.withPattern(withStep(p, 1, 16, note(50))));   // D-4
        assertSameBytes(4, blank.withPattern(withStep(p, 2, 1, note(52))));    // E-4
    }

    @Test
    void blankWithOneGateChangeIsEqualToTheGateSamples() throws Exception {
        SeqFile blank = SeqFile.blank();
        Pattern p = blank.pattern();
        assertSameBytes(5, blank.withPattern(withStep(p, 1, 1, gateLength(0.8))));
        assertSameBytes(6, blank.withPattern(
                withStep(p, 1, 1, s -> new Step(false, s.midiNote(), s.gateLength()))));
        assertSameBytes(7, blank.withPattern(withStep(p, 1, 1, gateLength(0.0))));
        assertSameBytes(8, blank.withPattern(withStep(p, 1, 1, gateLength(1.0))));
    }

    @Test
    void blankWithMergedStepsIsEqualToTheMergeSamples() throws Exception {
        SeqFile blank = SeqFile.blank();
        Pattern p = blank.pattern();
        // Steps 1 and 2 merged: only step 1 has the merge bit.
        assertSameBytes(9, blank.withPattern(withStep(p, 1, 1, merged())));
        // Steps 49 to 51 merged: steps 49 and 50 have the merge bit.
        assertSameBytes(14, blank.withPattern(withStep(withStep(p, 1, 49, merged()), 1, 50, merged())));
    }

    @Test
    void blankWithATrackLengthIsEqualToTheLengthSamples() throws Exception {
        SeqFile blank = SeqFile.blank();
        Pattern p = blank.pattern();
        assertSameBytes(10, blank.withPattern(withLength(p, 1, 8)));
        assertSameBytes(11, blank.withPattern(withStep(withLength(p, 1, 64), 1, 64, note(53))));  // F-4
    }

    @Test
    void blankWithAMasterTrackLengthIsEqualToTheSample() throws Exception {
        assertEquals(16, SeqFile.blank().masterTrackLength());
        assertSameBytes(12, SeqFile.blank().withMasterTrackLength(32));
        assertThrows(IllegalArgumentException.class, () -> SeqFile.blank().withMasterTrackLength(0));
        assertThrows(IllegalArgumentException.class, () -> SeqFile.blank().withMasterTrackLength(65));
    }

    // ----- decode -----

    @Test
    void blankDecodesToTheValuesThatTheModuleShows() {
        Pattern p = SeqFile.blank().pattern();
        assertEquals(4, p.tracks().size());
        for (Track t : p.tracks()) {
            assertEquals(16, t.length());
            assertEquals(64, t.steps().size());
            for (Step s : t.steps()) {
                assertEquals(new Step(true, 36, 0.5), s);   // C-3, 50 %, gate on
            }
        }
    }

    @Test
    void samplesDecodeToTheirChanges() throws Exception {
        assertEquals(48, SeqFile.read(sample(1)).pattern().tracks().get(0).steps().get(0).midiNote());
        assertEquals(50, SeqFile.read(sample(3)).pattern().tracks().get(0).steps().get(15).midiNote());
        assertEquals(52, SeqFile.read(sample(4)).pattern().tracks().get(1).steps().get(0).midiNote());
        assertEquals(0.8, SeqFile.read(sample(5)).pattern().tracks().get(0).steps().get(0).gateLength());
        assertFalse(SeqFile.read(sample(6)).pattern().tracks().get(0).steps().get(0).gateOn());
        assertTrue(SeqFile.read(sample(7)).pattern().tracks().get(0).steps().get(0).gateOn());
        assertEquals(8, SeqFile.read(sample(10)).pattern().tracks().get(0).length());
        Track long64 = SeqFile.read(sample(11)).pattern().tracks().get(0);
        assertEquals(64, long64.length());
        assertEquals(53, long64.steps().get(63).midiNote());
        assertEquals(32, SeqFile.read(sample(12)).masterTrackLength());
        List<Step> merged3 = SeqFile.read(sample(14)).pattern().tracks().get(0).steps();
        assertFalse(merged3.get(47).mergeNext());
        assertTrue(merged3.get(48).mergeNext());
        assertTrue(merged3.get(49).mergeNext());
        assertFalse(merged3.get(50).mergeNext());
    }

    // ----- unknown bytes stay -----

    @Test
    void aChangeToOneStepKeepsTheMergeBitOfAnotherStepAndAllOtherBytes() throws Exception {
        SeqFile merged = SeqFile.read(sample(9));       // steps 1 and 2 merged
        byte[] before = merged.toBytes();
        byte[] after = merged.withPattern(withStep(merged.pattern(), 1, 2, note(60))).toBytes();
        List<ByteDiff.Range> ranges = ByteDiff.diff(before, after);
        assertEquals(1, ranges.size());
        assertEquals(12 + 34, ranges.get(0).offset());
        assertEquals(2, ranges.get(0).length());        // 3600 to 6000: two low bytes
        assertEquals(3, after[22]);                     // gate on + merged, as in the sample
    }

    @Test
    void aShortTrackChangesOnlyItsOwnSteps() throws Exception {
        SeqFile factory = SeqFile.read(SAMPLES.resolve("FACTORY").resolve("00").resolve("01.SEQ"));
        List<Track> tracks = new ArrayList<>(factory.pattern().tracks());
        tracks.set(0, new Track(4, List.of(new Step(true, 40, 0.25), new Step(true, 41, 0.25),
                new Step(false, 42, 0.25), new Step(true, 43, 1.0))));
        SeqFile made = factory.withPattern(new Pattern(tracks));
        Track t = made.pattern().tracks().get(0);
        assertEquals(4, t.length());
        assertEquals(new Step(false, 42, 0.25), t.steps().get(2));
        assertEquals(factory.pattern().tracks().get(0).steps().subList(4, 64), t.steps().subList(4, 64));
        assertEquals(factory.pattern().tracks().subList(1, 4), made.pattern().tracks().subList(1, 4));
        for (ByteDiff.Range r : ByteDiff.diff(factory.toBytes(), made.toBytes())) {
            boolean inFirstFourSteps = r.offset() >= 12 && r.lastOffset() < 12 + 34 * 4;
            boolean isLastStep = r.offset() == 12 + 2177 && r.length() == 1;
            assertTrue(inFirstFourSteps || isLastStep, "change at " + r.offset());
        }
    }

    @Test
    void instancesAreImmutable() {
        SeqFile blank = SeqFile.blank();
        byte[] copy = blank.toBytes();
        copy[12] = 0;
        assertEquals(blank, SeqFile.blank());
        blank.withMasterTrackLength(8);
        assertEquals(16, blank.masterTrackLength());
    }

    // ----- refuse what is not known -----

    private static void assertRefused(byte[] data, String messagePart) {
        SeqFormatException e = assertThrows(SeqFormatException.class, () -> SeqFile.fromBytes(data));
        assertTrue(e.getMessage().contains(messagePart), e.getMessage());
    }

    @Test
    void filesThatAreDifferentInAConstantPartAreRefused() {
        byte[] blank = SeqFile.blank().toBytes();

        assertRefused(new byte[0], "file size");
        assertRefused(java.util.Arrays.copyOf(blank, blank.length + 1), "file size");

        byte[] version = blank.clone();
        version[3] = 6;
        assertRefused(version, "file header");

        byte[] prefix = blank.clone();
        prefix[0] = 0;
        assertRefused(prefix, "file header");

        byte[] frame = blank.clone();
        frame[6 + 2298 * 2] = 0;
        assertRefused(frame, "track header of CH3");
    }

    @Test
    void valuesThatTheModelCannotHoldAreRefused() {
        byte[] blank = SeqFile.blank().toBytes();

        byte[] gate = blank.clone();
        gate[16] = 101;
        assertRefused(gate, "CH1 step 1: gate length 101");

        byte[] noteHigh = blank.clone();
        noteHigh[12 + 34 + 2] = 1;                        // 3600 + 65536
        assertRefused(noteHigh, "CH1 step 2: note value");

        byte[] noteNegative = blank.clone();
        noteNegative[12 + 3] = (byte) 0x80;
        assertRefused(noteNegative, "CH1 step 1: note value");

        byte[] length = blank.clone();
        length[12 + 2177] = 0;
        assertRefused(length, "CH1: track length is 1");

        byte[] lengthHigh = blank.clone();
        lengthHigh[12 + 2298 + 2177] = 64;
        assertRefused(lengthHigh, "CH2: track length is 65");

        byte[] master = blank.clone();
        master[5] = 0;
        assertRefused(master, "master track length");
    }

    @Test
    void readOfAFileThatIsNotAPatternIsRefused(@TempDir Path dir) throws Exception {
        Path f = Files.write(dir.resolve("x.SEQ"), new byte[] {1, 2, 3});
        assertThrows(SeqFormatException.class, () -> SeqFile.read(f));
    }
}
