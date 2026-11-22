package blackseq.cli;

import blackseq.format.ByteDiff;
import blackseq.format.HexDump;
import blackseq.format.SeqFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

/**
 * The {@code blackseq} command. {@code dump} and {@code diff} only read files.
 * {@code testfiles} writes new files into a directory that the user gives and replaces no file.
 */
public final class Main {

    /** The command completed. For {@code diff}: the files are equal. */
    public static final int EXIT_OK = 0;
    /** {@code diff} only: the files are different. */
    public static final int EXIT_DIFFERENT = 1;
    /** Incorrect arguments, or a file that cannot be read. */
    public static final int EXIT_ERROR = 2;

    private static final String USAGE = """
            Usage:
              blackseq dump <file> [--offset <n>] [--length <n>]
                  Hex view with offsets. <n> is decimal, or hexadecimal with the prefix 0x.
              blackseq diff <a> <b>
                  Changed byte ranges between two files.
                  Exit status: 0 = equal, 1 = different, 2 = error.
              blackseq testfiles <dir>
                  Write the three test patterns of the phase 1 checkpoint (00.SEQ, 01.SEQ,
                  02.SEQ) into <dir>. The directory must exist. No file is replaced.
            """;

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs one command and returns the exit status. Tests call this method. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            (args.length == 0 ? err : out).print(USAGE);
            return args.length == 0 ? EXIT_ERROR : EXIT_OK;
        }
        try {
            return switch (args[0]) {
                case "dump" -> dump(args, out);
                case "diff" -> diff(args, out);
                case "testfiles" -> testFiles(args, out);
                default -> throw new UsageException("Unknown command: " + args[0]);
            };
        } catch (UsageException e) {
            err.println(e.getMessage());
            err.print(USAGE);
            return EXIT_ERROR;
        } catch (FileAlreadyExistsException e) {
            err.println("File exists, not replaced: " + e.getFile());
            return EXIT_ERROR;
        } catch (NoSuchFileException e) {
            err.println("File not found: " + e.getFile());
            return EXIT_ERROR;
        } catch (IOException e) {
            err.println("File error: " + e);
            return EXIT_ERROR;
        }
    }

    private static int dump(String[] args, PrintStream out) throws IOException {
        Path file = null;
        Integer offset = null;
        Integer length = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--offset" -> offset = number(args, ++i, "--offset");
                case "--length" -> length = number(args, ++i, "--length");
                default -> {
                    if (args[i].startsWith("--")) {
                        throw new UsageException("Unknown option: " + args[i]);
                    }
                    if (file != null) {
                        throw new UsageException("dump takes one file");
                    }
                    file = path(args[i]);
                }
            }
        }
        if (file == null) {
            throw new UsageException("dump: the file is missing");
        }
        byte[] data = Files.readAllBytes(file);
        int from = offset == null ? 0 : offset;
        if (from > data.length) {
            throw new UsageException(
                    "--offset " + from + " is after the end of the file (" + data.length + " bytes)");
        }
        int count = length == null ? data.length - from : Math.min(length, data.length - from);
        out.print(HexDump.format(data, from, count));
        out.println(data.length + " bytes total");
        return EXIT_OK;
    }

    private static int diff(String[] args, PrintStream out) throws IOException {
        if (args.length != 3) {
            throw new UsageException("diff takes two files");
        }
        byte[] a = Files.readAllBytes(path(args[1]));
        byte[] b = Files.readAllBytes(path(args[2]));
        out.println("a: " + args[1]);
        out.println("b: " + args[2]);
        String report = ByteDiff.format(a, b);
        out.print(report);
        return ByteDiff.diff(a, b).isEmpty() ? EXIT_OK : EXIT_DIFFERENT;
    }

    private static int testFiles(String[] args, PrintStream out) throws IOException {
        if (args.length != 2) {
            throw new UsageException("testfiles takes one directory");
        }
        Path dir = path(args[1]);
        if (!Files.isDirectory(dir)) {
            throw new UsageException("Not a directory: " + dir);
        }
        Map<String, SeqFile> files = TestFiles.phase1();
        // Check all names first, so that the command writes all three files or none.
        for (String name : files.keySet()) {
            if (Files.exists(dir.resolve(name))) {
                throw new FileAlreadyExistsException(dir.resolve(name).toString());
            }
        }
        for (Map.Entry<String, SeqFile> e : files.entrySet()) {
            e.getValue().write(dir.resolve(e.getKey()));
            out.println("Wrote " + dir.resolve(e.getKey()));
            out.println("  " + TestFiles.DESCRIPTIONS.get(e.getKey()));
        }
        return EXIT_OK;
    }

    private static Path path(String text) {
        try {
            return Path.of(text);
        } catch (InvalidPathException e) {
            throw new UsageException("Not a valid path: " + text);
        }
    }

    private static int number(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new UsageException(option + ": the value is missing");
        }
        String text = args[index];
        try {
            int value = text.startsWith("0x") || text.startsWith("0X")
                    ? Integer.parseInt(text.substring(2), 16)
                    : Integer.parseInt(text);
            if (value < 0) {
                throw new UsageException(option + " must be 0 or more: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new UsageException(option + ": not a number: " + text);
        }
    }

    private static final class UsageException extends RuntimeException {
        UsageException(String message) {
            super(message);
        }
    }
}
