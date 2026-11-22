package blackseq.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private Path file(String name, byte... bytes) throws IOException {
        return Files.write(dir.resolve(name), bytes);
    }

    @Test
    void dumpPrintsHexViewAndTotal() throws IOException {
        Path f = file("00.SEQ", new byte[] {(byte) 0xEC, 0x47, 0x08, 0x05});
        assertEquals(Main.EXIT_OK, run("dump", f.toString()));
        assertEquals("00000000      0  ec 47 08 05 " + " ".repeat(37) + "|.G..|\n4 bytes total\n",
                out());
    }

    @Test
    void dumpAcceptsHexOffsetAndClampsLength() throws IOException {
        Path f = file("a.SEQ", new byte[40]);
        assertEquals(Main.EXIT_OK, run("dump", f.toString(), "--offset", "0x20", "--length", "100"));
        assertTrue(out().startsWith("00000020     32  00 00 00 00 00 00 00 00 "), out());
        assertTrue(out().endsWith("40 bytes total\n"));
    }

    @Test
    void dumpDoesNotChangeTheFile() throws IOException {
        byte[] bytes = {1, 2, 3};
        Path f = file("a.SEQ", bytes);
        run("dump", f.toString());
        assertArrayEquals(bytes, Files.readAllBytes(f));
    }

    @Test
    void diffOfEqualFilesReturnsZero() throws IOException {
        Path a = file("a.SEQ", (byte) 1, (byte) 2);
        Path b = file("b.SEQ", (byte) 1, (byte) 2);
        assertEquals(Main.EXIT_OK, run("diff", a.toString(), b.toString()));
        assertTrue(out().endsWith("no differences\n"), out());
    }

    @Test
    void diffOfDifferentFilesReturnsOneAndListsTheRange() throws IOException {
        Path a = file("a.SEQ", (byte) 1, (byte) 2, (byte) 3);
        Path b = file("b.SEQ", (byte) 1, (byte) 9, (byte) 3);
        assertEquals(Main.EXIT_DIFFERENT, run("diff", a.toString(), b.toString()));
        assertTrue(out().contains("00000001..00000001       1..1          1 byte   a: 02  b: 09\n"),
                out());
        assertTrue(out().endsWith("1 range, 1 byte changed\n"), out());
    }

    @Test
    void missingFileIsAnErrorWithoutAStackTrace() {
        assertEquals(Main.EXIT_ERROR, run("dump", dir.resolve("none.SEQ").toString()));
        assertTrue(err().startsWith("File not found: "), err());
        assertEquals("", out());
    }

    @Test
    void incorrectArgumentsPrintUsage() {
        assertEquals(Main.EXIT_ERROR, run());
        assertEquals(Main.EXIT_ERROR, run("convert"));
        assertEquals(Main.EXIT_ERROR, run("diff", "only-one"));
        assertEquals(Main.EXIT_ERROR, run("dump", "a", "--offset", "x"));
        assertEquals(Main.EXIT_ERROR, run("dump", "a", "--bogus"));
        assertTrue(err().contains("Usage:"));
    }

    @Test
    void helpPrintsUsageToStandardOutput() {
        assertEquals(Main.EXIT_OK, run("--help"));
        assertTrue(out().contains("blackseq dump <file>"));
    }
}
