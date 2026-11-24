package blackseq.card;

import blackseq.format.SeqFile;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * One project folder of the Black Sequencer: {@code PROJECTS/<name>} on the SD card, or a
 * copy of it.
 *
 * <p>This class writes pattern files only. It writes only inside the project folder, it
 * makes no bank folder, and it never deletes a file.
 */
public final class ProjectFolder {

    /** The module makes this file in each project folder. */
    static final String PROJECT_FILE = "INFO.DAT";
    /** The module makes this file in each bank folder. */
    static final String BANK_FILE = "BANK.DAT";
    /** Folder for the old content of replaced pattern files. */
    static final String BACKUP_FOLDER = "BACKUP";
    private static final int MAX_BACKUPS = 999;
    /** Number of banks in a project. */
    public static final int BANKS = 16;
    /** Number of patterns in a bank. */
    public static final int PATTERNS_PER_BANK = 16;

    private final Path dir;

    private ProjectFolder(Path dir) {
        this.dir = dir;
    }

    /**
     * Opens a project folder that the module made.
     *
     * @throws IOException if the folder does not exist or has no {@code INFO.DAT}
     */
    public static ProjectFolder open(Path projectDir) throws IOException {
        if (!Files.isDirectory(projectDir)) {
            throw new NotDirectoryException(projectDir.toString());
        }
        if (!Files.isRegularFile(projectDir.resolve(PROJECT_FILE))) {
            throw new IOException("Not a Black Sequencer project folder (no " + PROJECT_FILE + "): "
                    + projectDir + ". Make the project on the module first.");
        }
        return new ProjectFolder(projectDir);
    }

    /**
     * The file of a pattern. Bank 1 is the folder {@code 00} and pattern 1 is the file
     * {@code 00.SEQ}.
     *
     * @param bank    1 to 16
     * @param pattern 1 to 16
     */
    public Path patternPath(int bank, int pattern) {
        check("bank", bank, BANKS);
        check("pattern", pattern, PATTERNS_PER_BANK);
        return bankDir(bank).resolve(patternFile(pattern));
    }

    /**
     * The name of a pattern file in its project folder, with {@code /} between the bank
     * folder and the file: {@code 01/03.SEQ} for bank 2, pattern 4. A program that does not
     * write to a project folder, for example one that makes a zip file, uses this name.
     *
     * @param bank    1 to 16
     * @param pattern 1 to 16
     */
    public static String patternName(int bank, int pattern) {
        check("bank", bank, BANKS);
        check("pattern", pattern, PATTERNS_PER_BANK);
        return twoDigits(bank - 1) + "/" + patternFile(pattern);
    }

    private static String patternFile(int pattern) {
        return twoDigits(pattern - 1) + ".SEQ";
    }

    /**
     * Writes a pattern file and reads it back to make sure that the bytes on the disk are
     * correct.
     *
     * @throws FileAlreadyExistsException with {@link WriteMode#FAIL_IF_EXISTS}, if the file exists
     * @throws IOException                if the bank folder was not made by the module, if a
     *                                    backup cannot be made, or if the write fails
     */
    public void writePattern(int bank, int pattern, SeqFile file, WriteMode mode) throws IOException {
        Path target = patternPath(bank, pattern);
        Path bankDir = bankDir(bank);
        if (!Files.isRegularFile(bankDir.resolve(BANK_FILE))) {
            throw new NoSuchFileException(bankDir.resolve(BANK_FILE).toString(), null,
                    "The module did not make the folder of bank " + bank
                            + ". Select the bank on the module one time, then try again.");
        }
        byte[] bytes = file.toBytes();
        if (Files.exists(target)) {
            if (mode == WriteMode.FAIL_IF_EXISTS) {
                throw new FileAlreadyExistsException(target.toString());
            }
            backup(target, bank, pattern);
            Files.write(target, bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        if (!Arrays.equals(bytes, Files.readAllBytes(target))) {
            throw new IOException("The file on the disk is not equal to the data that was written: " + target);
        }
    }

    /**
     * Copies a pattern file to the backup folder under a new name. The name has 8
     * characters, for example {@code 0003_001.SEQ}: bank folder 00, pattern file 03, copy 1.
     */
    private void backup(Path target, int bank, int pattern) throws IOException {
        Path folder = dir.resolve(BACKUP_FOLDER);
        if (!Files.isDirectory(folder)) {
            Files.createDirectory(folder);
        }
        for (int n = 1; n <= MAX_BACKUPS; n++) {
            Path copy = folder.resolve(
                    twoDigits(bank - 1) + twoDigits(pattern - 1) + "_" + String.format("%03d", n) + ".SEQ");
            if (!Files.exists(copy)) {
                Files.copy(target, copy, StandardCopyOption.COPY_ATTRIBUTES);
                if (!Arrays.equals(Files.readAllBytes(target), Files.readAllBytes(copy))) {
                    throw new IOException("The backup is not equal to the file: " + copy);
                }
                return;
            }
        }
        throw new IOException("The backup folder has " + MAX_BACKUPS + " copies of this pattern: " + folder);
    }

    private Path bankDir(int bank) {
        return dir.resolve(twoDigits(bank - 1));
    }

    private static String twoDigits(int n) {
        return String.format("%02d", n);
    }

    private static void check(String what, int value, int max) {
        if (value < 1 || value > max) {
            throw new IllegalArgumentException(what + " must be 1 to " + max + ": " + value);
        }
    }
}
