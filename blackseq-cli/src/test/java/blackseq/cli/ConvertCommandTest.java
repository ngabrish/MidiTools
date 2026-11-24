package blackseq.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blackseq.format.SeqFile;
import blackseq.model.Step;
import blackseq.model.Track;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConvertCommandTest {

    private static final int PPQ = 480;
    private static final int SIXTEENTH = PPQ / 4;

    @TempDir
    Path dir;

    private Path project;
    private Path midi;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    /** A project as the module makes it, and a MIDI file with two tracks of one bar. */
    @BeforeEach
    void setUp() throws Exception {
        project = Files.createDirectories(dir.resolve("PROJECTS").resolve("SONG"));
        Files.write(project.resolve("INFO.DAT"), new byte[] {7});
        Files.createDirectory(project.resolve("00"));
        Files.write(project.resolve("00").resolve("BANK.DAT"), new byte[] {1});

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        javax.sound.midi.Track lead = sequence.createTrack();
        note(lead, 0, 48, 0, SIXTEENTH / 2);              // step 1, C-4, half a step
        note(lead, 0, 50, 4 * SIXTEENTH, 3 * SIXTEENTH);  // steps 5 to 7, D-4
        note(lead, 0, 52, 15 * SIXTEENTH, SIXTEENTH);     // step 16, E-4
        javax.sound.midi.Track bass = sequence.createTrack();
        note(bass, 1, 36, 0, SIXTEENTH);                  // step 1, C-3
        note(bass, 1, 40, 0, SIXTEENTH);                  // a chord note on step 1, E-3
        midi = dir.resolve("song.mid");
        MidiSystem.write(sequence, 1, midi.toFile());
    }

    private static void note(javax.sound.midi.Track track, int channel, int number, long tick, long ticks)
            throws Exception {
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, number, 100), tick));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, number, 0), tick + ticks));
    }

    private int run(String... args) {
        return Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private long fileCount() throws IOException {
        try (var files = Files.walk(dir)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private int convert(String... more) {
        String[] args = new String[8 + more.length];
        System.arraycopy(new String[] {"convert", midi.toString(), "--project", project.toString(),
                "--bank", "1", "--pattern", "2"}, 0, args, 0, 8);
        System.arraycopy(more, 0, args, 8, more.length);
        return run(args);
    }

    @Test
    void convertWritesAPatternFileThatHoldsTheNotes() throws Exception {
        assertEquals(Main.EXIT_OK, convert(), err());
        Path target = project.resolve("00").resolve("01.SEQ");
        assertTrue(out().contains("Wrote " + target), out());

        SeqFile file = SeqFile.read(target);
        assertEquals(16, file.masterTrackLength());
        Track ch1 = file.pattern().tracks().get(0);
        assertEquals(16, ch1.length());
        assertEquals(new Step(true, 48, 0.5, false), ch1.steps().get(0));
        assertEquals(new Step(false, 48, 0.5, false), ch1.steps().get(1));
        assertEquals(new Step(true, 50, 0.99, true), ch1.steps().get(4));
        assertEquals(new Step(true, 50, 0.99, true), ch1.steps().get(5));
        assertEquals(new Step(true, 50, 0.99, false), ch1.steps().get(6));
        assertEquals(new Step(true, 52, 0.99, false), ch1.steps().get(15));

        Track ch2 = file.pattern().tracks().get(1);
        assertEquals(new Step(true, 40, 0.99, false), ch2.steps().get(0), "the highest note of the chord");
        assertTrue(ch2.steps().subList(1, 16).stream().noneMatch(Step::gateOn));
        assertTrue(file.pattern().tracks().get(2).steps().subList(0, 16).stream().noneMatch(Step::gateOn));

        assertTrue(out().contains("Result: 4 notes in 1 pattern, 1 dropped, 1 warning"), out());
        assertTrue(out().contains("CH2 beat 1: Note 36 dropped"), out());
        assertTrue(out().contains("CH1 (16 steps): C-4 . . . D-4 ~ ~ . . . . . . . . E-4"), out());
        assertTrue(out().contains("CH3 (16 steps): no notes"), out());
    }

    @Test
    void dryRunWritesNothing() throws Exception {
        long before = fileCount();
        assertEquals(Main.EXIT_OK, convert("--dry-run"), err());
        assertEquals(before, fileCount());
        assertTrue(out().contains("Dry run: would write " + project.resolve("00").resolve("01.SEQ")), out());
        assertTrue(out().endsWith("Dry run: no file was written.\n"), out());

        assertEquals(Main.EXIT_OK, run("convert", midi.toString(), "--dry-run"), err());
        assertEquals(before, fileCount());
    }

    @Test
    void existingPatternIsNotReplacedWithoutTheReplaceOption() throws Exception {
        assertEquals(Main.EXIT_OK, convert());
        long before = fileCount();
        byte[] first = Files.readAllBytes(project.resolve("00").resolve("01.SEQ"));

        assertEquals(Main.EXIT_ERROR, convert("--transpose", "12"));
        assertTrue(err().startsWith("File exists, not replaced: "), err());
        assertTrue(err().contains("use --replace"), err());
        assertEquals(before, fileCount());
        assertEquals(SeqFile.fromBytes(first), SeqFile.read(project.resolve("00").resolve("01.SEQ")));

        assertEquals(Main.EXIT_OK, convert("--transpose", "12", "--replace"), err());
        assertEquals(60, SeqFile.read(project.resolve("00").resolve("01.SEQ")).pattern().tracks().get(0)
                .steps().get(0).midiNote());
        assertEquals(SeqFile.fromBytes(first), SeqFile.read(project.resolve("BACKUP").resolve("0001_001.SEQ")));
    }

    @Test
    void mapSelectsTheClipForEachChannel() throws Exception {
        assertEquals(Main.EXIT_OK, convert("--map", "2,0,1", "--chords", "lowest"), err());
        assertTrue(out().contains("CH1 <- Track 2"), out());
        assertTrue(out().contains("CH2 <- (no clip)"), out());
        assertTrue(out().contains("CH3 <- Track 1"), out());
        List<Track> tracks = SeqFile.read(project.resolve("00").resolve("01.SEQ")).pattern().tracks();
        assertEquals(new Step(true, 36, 0.99, false), tracks.get(0).steps().get(0));
        assertTrue(tracks.get(1).steps().stream().limit(16).noneMatch(Step::gateOn));
        assertEquals(new Step(true, 48, 0.5, false), tracks.get(2).steps().get(0));

        assertEquals(Main.EXIT_ERROR, convert("--map", "3"));
        assertTrue(err().contains("the file has no clip 3"), err());
    }

    @Test
    void longFileGoesIntoTheNextPatterns() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        javax.sound.midi.Track track = sequence.createTrack();
        note(track, 0, 60, 0, SIXTEENTH);
        note(track, 0, 62, 64L * SIXTEENTH, SIXTEENTH);    // step 65: bar 5
        MidiSystem.write(sequence, 1, midi.toFile());

        assertEquals(Main.EXIT_OK, convert(), err());
        assertEquals(64, SeqFile.read(project.resolve("00").resolve("01.SEQ")).pattern().tracks().get(0).length());
        SeqFile second = SeqFile.read(project.resolve("00").resolve("02.SEQ"));
        assertEquals(16, second.pattern().tracks().get(0).length());
        assertEquals(62, second.pattern().tracks().get(0).steps().get(0).midiNote());
        assertEquals(64, SeqFile.read(project.resolve("00").resolve("01.SEQ")).masterTrackLength());

        long before = fileCount();
        assertEquals(Main.EXIT_ERROR, run("convert", midi.toString(), "--project", project.toString(),
                "--bank", "1", "--pattern", "16"));
        assertTrue(err().contains("does not fit in the bank"), err());
        assertEquals(before, fileCount());

        assertEquals(Main.EXIT_ERROR, convert("--overflow", "fail", "--replace"));
        assertTrue(err().contains("80 steps"), err());
    }

    @Test
    void secondPatternThatExistsStopsTheCommandBeforeTheFirstWrite() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        javax.sound.midi.Track track = sequence.createTrack();
        note(track, 0, 60, 0, SIXTEENTH);
        note(track, 0, 62, 64L * SIXTEENTH, SIXTEENTH);
        MidiSystem.write(sequence, 1, midi.toFile());
        Files.write(project.resolve("00").resolve("02.SEQ"), new byte[] {9});

        assertEquals(Main.EXIT_ERROR, convert());
        assertFalse(Files.exists(project.resolve("00").resolve("01.SEQ")), "all files or none");
    }

    @Test
    void projectThatTheModuleDidNotMakeIsRefused() throws Exception {
        Path notAProject = Files.createDirectory(dir.resolve("other"));
        assertEquals(Main.EXIT_ERROR, run("convert", midi.toString(), "--project", notAProject.toString(),
                "--bank", "1", "--pattern", "1"));
        assertTrue(err().contains("Not a Black Sequencer project folder"), err());

        assertEquals(Main.EXIT_ERROR, run("convert", midi.toString(), "--project", project.toString(),
                "--bank", "2", "--pattern", "1"));
        assertTrue(err().contains("Select the bank on the module"), err());
        assertFalse(Files.exists(project.resolve("01")));
    }

    @Test
    void incorrectArgumentsAreErrors() throws Exception {
        assertEquals(Main.EXIT_ERROR, run("convert"));
        assertEquals(Main.EXIT_ERROR, run("convert", midi.toString()));
        assertEquals(Main.EXIT_ERROR, convert("--bank"));
        assertEquals(Main.EXIT_ERROR, run("convert", midi.toString(), "--project", project.toString(),
                "--bank", "17", "--pattern", "1"));
        assertEquals(Main.EXIT_ERROR, convert("--chords", "loudest"));
        assertEquals(Main.EXIT_ERROR, convert("--bogus"));
        assertEquals(Main.EXIT_ERROR, run("convert", dir.resolve("none.mid").toString(), "--dry-run"));
        Path text = Files.writeString(dir.resolve("text.mid"), "not midi");
        assertEquals(Main.EXIT_ERROR, run("convert", text.toString(), "--dry-run"));
        assertEquals(1, fileCount() - 3, "only the text file is new: the MIDI file, INFO.DAT and BANK.DAT were there");
    }

    /** A MIDI file with 12 notes off the grid on track 1, and 3 chords of two notes on track 2. */
    private void writeFileWithManyWarnings() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        javax.sound.midi.Track played = sequence.createTrack();
        for (int i = 0; i < 12; i++) {
            note(played, 0, 60 + i, i * SIXTEENTH + 20, SIXTEENTH / 2);
        }
        javax.sound.midi.Track chords = sequence.createTrack();
        for (int i = 0; i < 3; i++) {
            note(chords, 1, 40, i * 4L * SIXTEENTH, SIXTEENTH);
            note(chords, 1, 47, i * 4L * SIXTEENTH, SIXTEENTH);
        }
        MidiSystem.write(sequence, 1, midi.toFile());
    }

    @Test
    void manyWarningsArePrintedAsCountsForEachKind() throws Exception {
        writeFileWithManyWarnings();
        assertEquals(Main.EXIT_OK, run("convert", midi.toString(), "--dry-run"), err());
        assertTrue(out().contains("Result: 15 notes in 1 pattern, 3 dropped, 15 warnings\n"
                + "     3 dropped (CH2: 3)\n"
                + "    12 moved (CH1: 12)\n"
                + "  Use --verbose to print each warning.\n"), out());
        assertFalse(out().contains("Note 60 moved"), out());
        assertTrue(out().contains("Pattern 1:"), "the pattern view is printed in the two modes");
    }

    @Test
    void verbosePrintsEachWarning() throws Exception {
        writeFileWithManyWarnings();
        assertEquals(Main.EXIT_OK, run("convert", midi.toString(), "--dry-run", "--verbose"), err());
        assertTrue(out().contains("CH1 beat 1.042: Note 60 moved: the note starts at step position 1.17 and goes to step 1."),
                out());
        assertTrue(out().contains("CH2 beat 3: Note 40 dropped"), out());
        assertFalse(out().contains("Use --verbose"), out());
        long lines = out().lines().filter(l -> l.contains(" moved: ") || l.contains(" dropped: ")).count();
        assertEquals(15, lines);
    }

    @Test
    void tenOrFewerWarningsArePrintedWithoutTheVerboseOption() throws Exception {
        assertEquals(Main.EXIT_OK, run("convert", midi.toString(), "--dry-run"), err());
        assertTrue(out().contains("CH2 beat 1: Note 36 dropped"), out());
        assertFalse(out().contains("Use --verbose"), out());
    }
}
