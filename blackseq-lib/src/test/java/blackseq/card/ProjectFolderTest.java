package blackseq.card;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blackseq.format.SeqFile;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class ProjectFolderTest {

    private static final Path SAMPLE_PROJECT = Path.of("..", "samples", "blackseq", "PROJECTS", "RETEST");

    @TempDir
    Path card;

    private Path projectDir;
    private SeqFile blank;
    private SeqFile other;

    /** A project as the module makes it: INFO.DAT and one bank folder with BANK.DAT. */
    @BeforeEach
    void makeProject() throws IOException {
        projectDir = Files.createDirectories(card.resolve("PROJECTS").resolve("MY SONG"));
        Files.write(projectDir.resolve("INFO.DAT"), new byte[] {7});
        Files.createDirectory(projectDir.resolve("00"));
        Files.write(projectDir.resolve("00").resolve("BANK.DAT"), new byte[] {1});
        blank = SeqFile.blank();
        other = blank.withMasterTrackLength(32);
    }

    private List<Path> allFiles() throws IOException {
        try (var files = Files.walk(card)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        }
    }

    @Test
    void openNeedsAFolderThatTheModuleMade() throws IOException {
        assertThrows(IOException.class, () -> ProjectFolder.open(card.resolve("none")));
        IOException e = assertThrows(IOException.class, () -> ProjectFolder.open(card.resolve("PROJECTS")));
        assertTrue(e.getMessage().contains("no INFO.DAT"), e.getMessage());
        Path file = Files.write(card.resolve("file"), new byte[0]);
        assertThrows(IOException.class, () -> ProjectFolder.open(file));
    }

    @Test
    void patternPathUsesTwoDigitNamesThatStartAtZero() throws IOException {
        ProjectFolder project = ProjectFolder.open(projectDir);
        assertEquals(projectDir.resolve("00").resolve("00.SEQ"), project.patternPath(1, 1));
        assertEquals(projectDir.resolve("09").resolve("10.SEQ"), project.patternPath(10, 11));
        assertEquals(projectDir.resolve("15").resolve("15.SEQ"), project.patternPath(16, 16));
        assertThrows(IllegalArgumentException.class, () -> project.patternPath(0, 1));
        assertThrows(IllegalArgumentException.class, () -> project.patternPath(17, 1));
        assertThrows(IllegalArgumentException.class, () -> project.patternPath(1, 0));
        assertThrows(IllegalArgumentException.class, () -> project.patternPath(1, 17));
    }

    @Test
    void patternPathAgreesWithTheSampleProjectFromTheModule() throws Exception {
        ProjectFolder project = ProjectFolder.open(SAMPLE_PROJECT);
        for (int pattern = 1; pattern <= 15; pattern++) {
            assertTrue(Files.isRegularFile(project.patternPath(1, pattern)), "pattern " + pattern);
        }
        assertFalse(Files.exists(project.patternPath(1, 16)));
        assertEquals(SeqFile.blank(), SeqFile.read(project.patternPath(1, 1)));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void patternPathKeepsAWindowsDriveLetter() throws IOException {
        ProjectFolder project = ProjectFolder.open(projectDir);
        String path = project.patternPath(2, 3).toString();
        assertTrue(path.matches("^[A-Za-z]:\\\\.*\\\\PROJECTS\\\\MY SONG\\\\01\\\\02\\.SEQ$"), path);
        assertEquals(projectDir.getRoot(), project.patternPath(2, 3).getRoot());
    }

    @Test
    void writeMakesANewPatternFile() throws Exception {
        ProjectFolder project = ProjectFolder.open(projectDir);
        project.writePattern(1, 3, other, WriteMode.FAIL_IF_EXISTS);
        assertEquals(other, SeqFile.read(projectDir.resolve("00").resolve("02.SEQ")));
        assertEquals(3, allFiles().size(), "INFO.DAT, BANK.DAT and the new pattern");
    }

    @Test
    void failIfExistsChangesNothing() throws Exception {
        ProjectFolder project = ProjectFolder.open(projectDir);
        project.writePattern(1, 1, blank, WriteMode.FAIL_IF_EXISTS);
        List<Path> before = allFiles();
        FileAlreadyExistsException e = assertThrows(FileAlreadyExistsException.class,
                () -> project.writePattern(1, 1, other, WriteMode.FAIL_IF_EXISTS));
        assertTrue(e.getMessage().endsWith("00.SEQ"), e.getMessage());
        assertEquals(before, allFiles());
        assertEquals(blank, SeqFile.read(project.patternPath(1, 1)));
    }

    @Test
    void backupThenReplaceKeepsTheOldContentInTheBackupFolder() throws Exception {
        ProjectFolder project = ProjectFolder.open(projectDir);
        project.writePattern(1, 4, blank, WriteMode.BACKUP_THEN_REPLACE);
        assertFalse(Files.exists(projectDir.resolve("BACKUP")), "no backup for a new file");

        project.writePattern(1, 4, other, WriteMode.BACKUP_THEN_REPLACE);
        assertEquals(other, SeqFile.read(project.patternPath(1, 4)));
        assertEquals(blank, SeqFile.read(projectDir.resolve("BACKUP").resolve("0003_001.SEQ")));

        project.writePattern(1, 4, blank, WriteMode.BACKUP_THEN_REPLACE);
        assertEquals(blank, SeqFile.read(project.patternPath(1, 4)));
        assertEquals(blank, SeqFile.read(projectDir.resolve("BACKUP").resolve("0003_001.SEQ")),
                "an earlier backup is never replaced");
        assertEquals(other, SeqFile.read(projectDir.resolve("BACKUP").resolve("0003_002.SEQ")));
    }

    @Test
    void writeDoesNotMakeABankFolder() throws IOException {
        ProjectFolder project = ProjectFolder.open(projectDir);
        List<Path> before = allFiles();
        NoSuchFileException e = assertThrows(NoSuchFileException.class,
                () -> project.writePattern(2, 1, blank, WriteMode.FAIL_IF_EXISTS));
        assertTrue(e.getMessage().contains("Select the bank on the module"), e.getMessage());
        assertFalse(Files.exists(projectDir.resolve("01")));

        Files.createDirectory(projectDir.resolve("02"));     // a folder with no BANK.DAT
        assertThrows(NoSuchFileException.class,
                () -> project.writePattern(3, 1, blank, WriteMode.BACKUP_THEN_REPLACE));
        assertEquals(before, allFiles());
    }

    @Test
    void writeNeverChangesOtherFilesAndNeverDeletes() throws Exception {
        ProjectFolder project = ProjectFolder.open(projectDir);
        project.writePattern(1, 1, blank, WriteMode.FAIL_IF_EXISTS);
        project.writePattern(1, 2, blank, WriteMode.FAIL_IF_EXISTS);
        project.writePattern(1, 2, other, WriteMode.BACKUP_THEN_REPLACE);
        assertArrayEquals(new byte[] {7}, Files.readAllBytes(projectDir.resolve("INFO.DAT")));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(projectDir.resolve("00").resolve("BANK.DAT")));
        assertEquals(blank, SeqFile.read(project.patternPath(1, 1)));
        assertEquals(5, allFiles().size(), "INFO.DAT, BANK.DAT, two patterns, one backup");
        for (Path f : allFiles()) {
            assertTrue(f.startsWith(projectDir), "a file outside the project folder: " + f);
        }
    }
}
