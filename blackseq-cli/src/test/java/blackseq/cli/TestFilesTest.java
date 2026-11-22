package blackseq.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blackseq.format.SeqFile;
import blackseq.model.Pattern;
import blackseq.model.Step;
import blackseq.model.Track;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestFilesTest {

    private static final Path COMMITTED = Path.of("..", "samples", "blackseq", "generated", "phase1");
    private static final Path SAMPLE_C4 =
            Path.of("..", "samples", "blackseq", "PROJECTS", "RETEST", "00", "01.SEQ");

    private static int run(ByteArrayOutputStream out, ByteArrayOutputStream err, String... args) {
        return Main.run(args, new PrintStream(out, true), new PrintStream(err, true));
    }

    @Test
    void oneNoteFileIsEqualToTheFileThatTheModuleSavedForTheSameEdit() throws Exception {
        assertArrayEquals(Files.readAllBytes(SAMPLE_C4), TestFiles.phase1().get("00.SEQ").toBytes());
    }

    @Test
    void scaleFileHasSixteenNotesOfCMajorOnChannelOne() {
        Pattern p = TestFiles.phase1().get("01.SEQ").pattern();
        Track ch1 = p.tracks().get(0);
        assertEquals(16, ch1.length());
        int[] expected = {36, 38, 40, 41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60, 62};
        for (int i = 0; i < 16; i++) {
            assertEquals(new Step(true, expected[i], 0.5), ch1.steps().get(i), "step " + (i + 1));
        }
        assertEquals(SeqFile.blank().pattern().tracks().subList(1, 4), p.tracks().subList(1, 4));
    }

    @Test
    void longFileHasSixtyFourStepsThatGoUpThenDown() {
        Track ch1 = TestFiles.phase1().get("02.SEQ").pattern().tracks().get(0);
        assertEquals(64, ch1.length());
        assertEquals(36, ch1.steps().get(0).midiNote());
        assertEquals(67, ch1.steps().get(31).midiNote());
        assertEquals(68, ch1.steps().get(32).midiNote());
        assertEquals(37, ch1.steps().get(63).midiNote());
        for (Step s : ch1.steps()) {
            assertTrue(s.midiNote() >= 36 && s.midiNote() <= 72, "inside the note range of the samples");
        }
    }

    @Test
    void committedFilesAreEqualToTheGeneratedFiles() throws Exception {
        for (Map.Entry<String, SeqFile> e : TestFiles.phase1().entrySet()) {
            assertArrayEquals(e.getValue().toBytes(), Files.readAllBytes(COMMITTED.resolve(e.getKey())),
                    e.getKey());
        }
    }

    @Test
    void commandWritesThreeFiles(@TempDir Path dir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(Main.EXIT_OK, run(out, new ByteArrayOutputStream(), "testfiles", dir.toString()));
        for (Map.Entry<String, SeqFile> e : TestFiles.phase1().entrySet()) {
            assertEquals(e.getValue(), SeqFile.read(dir.resolve(e.getKey())));
        }
        assertTrue(out.toString().contains("One note: CH1 step 1 is C-4"));
    }

    @Test
    void commandReplacesNoFileAndWritesNothingIfOneFileExists(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("01.SEQ"), new byte[] {9});
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(Main.EXIT_ERROR, run(new ByteArrayOutputStream(), err, "testfiles", dir.toString()));
        assertTrue(err.toString().startsWith("File exists, not replaced: "), err.toString());
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(dir.resolve("01.SEQ")));
        assertFalse(Files.exists(dir.resolve("00.SEQ")));
        assertFalse(Files.exists(dir.resolve("02.SEQ")));
    }

    @Test
    void commandNeedsAnExistingDirectory(@TempDir Path dir) {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(Main.EXIT_ERROR,
                run(new ByteArrayOutputStream(), err, "testfiles", dir.resolve("none").toString()));
        assertTrue(err.toString().startsWith("Not a directory: "), err.toString());
    }
}
