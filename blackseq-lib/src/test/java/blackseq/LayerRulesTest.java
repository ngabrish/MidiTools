package blackseq;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks the package rules of blackseq-lib/CLAUDE.md on the source files. */
class LayerRulesTest {

    private static final Path MAIN = Path.of("src", "main", "java", "blackseq");
    private static final Path CORE = Path.of("..", "core", "src", "main", "java");

    private static List<Path> sources(Path dir) throws IOException {
        try (var files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static void assertNoImport(Path dir, String prefix) throws IOException {
        List<Path> files = sources(dir);
        assertFalse(files.isEmpty(), "no source files in " + dir);
        for (Path f : files) {
            for (String line : Files.readAllLines(f)) {
                assertFalse(line.startsWith("import " + prefix) || line.startsWith("import static " + prefix),
                        f + " must not import " + prefix + ": " + line);
            }
        }
    }

    @Test
    void modelAndConvertDoNotImportFormat() throws IOException {
        assertNoImport(MAIN.resolve("model"), "blackseq.format");
        assertNoImport(MAIN.resolve("convert"), "blackseq.format");
    }

    @Test
    void modelImportsNoOtherPackageOfTheProject() throws IOException {
        assertNoImport(MAIN.resolve("model"), "blackseq.");
        assertNoImport(MAIN.resolve("model"), "miditools.");
    }

    @Test
    void cardDependsOnFormatOnly() throws IOException {
        assertNoImport(MAIN.resolve("card"), "blackseq.convert");
        assertNoImport(MAIN.resolve("card"), "blackseq.model");
        assertNoImport(MAIN.resolve("card"), "miditools.");
    }

    @Test
    void coreDoesNotImportBlackseq() throws IOException {
        assertNoImport(CORE, "blackseq");
    }

    @Test
    void formatIsTheOnlyPackageThatUsesTheLayoutClass() throws IOException {
        for (Path f : sources(MAIN)) {
            if (!f.getParent().endsWith("format")) {
                assertFalse(Files.readString(f).contains("SeqLayout"), f.toString());
            }
        }
        assertTrue(Files.exists(MAIN.resolve("format").resolve("SeqLayout.java")));
    }
}
