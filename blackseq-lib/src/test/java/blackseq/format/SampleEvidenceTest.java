package blackseq.format;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Checks each "confirmed by samples" claim of docs/blackseq/FORMAT.md against the sample
 * files from the module. If a sample is replaced, a claim that is no longer true fails here.
 *
 * <p>NN.SEQ is pattern NN + 1 of samples/blackseq/README.md.
 */
class SampleEvidenceTest {

    private static final Path BANK = Path.of("..", "samples", "blackseq", "PROJECTS", "RETEST", "00");
    private static final Path FACTORY = Path.of("..", "samples", "blackseq", "PROJECTS", "FACTORY", "00");

    private static byte[] sample(int fileNumber) throws IOException {
        return Files.readAllBytes(BANK.resolve(String.format("%02d.SEQ", fileNumber)));
    }

    /** Asserts that the sample is different from the blank file in exactly one range. */
    private static void assertOneChange(int fileNumber, int offset, int... newBytes) throws IOException {
        List<ByteDiff.Range> ranges = ByteDiff.diff(sample(0), sample(fileNumber));
        assertEquals(1, ranges.size(), "ranges in sample " + fileNumber);
        ByteDiff.Range r = ranges.get(0);
        assertEquals(offset, r.offset(), "offset in sample " + fileNumber);
        byte[] expected = new byte[newBytes.length];
        for (int i = 0; i < newBytes.length; i++) {
            expected[i] = (byte) newBytes[i];
        }
        assertArrayEquals(expected, r.b(), "bytes in sample " + fileNumber);
    }

    @Test
    void allModuleFilesHaveTheSameSizeAndHeader() throws IOException {
        byte[] blank = sample(0);
        for (Path dir : List.of(BANK, FACTORY)) {
            try (var files = Files.list(dir)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".SEQ")).toList()) {
                    byte[] data = Files.readAllBytes(f);
                    assertEquals(9198, data.length, f.toString());
                    assertEquals("ec 47 08 05 10", HexDump.hex(data, 0, 5), f.toString());
                    for (int channel = 0; channel < 4; channel++) {
                        assertEquals("1a f7 11 0a f4 11", HexDump.hex(data, 6 + 2298 * channel, 6),
                                f.toString());
                    }
                }
            }
        }
        assertEquals(16, blank[5], "master track length of the blank pattern");
    }

    @Test
    void blankPatternHasFourEqualTracksOfSixtyFourEqualSteps() throws IOException {
        byte[] blank = sample(0);
        String step = HexDump.hex(blank, 12, 34);
        assertTrue(step.startsWith("10 0e 00 00 32 00 00 00 00 0a 01 "), step);
        for (int channel = 0; channel < 4; channel++) {
            int blob = 12 + 2298 * channel;
            for (int s = 0; s < 64; s++) {
                assertEquals(step, HexDump.hex(blank, blob + 34 * s, 34));
            }
            assertEquals(HexDump.hex(blank, 12 + 2176, 116), HexDump.hex(blank, blob + 2176, 116));
            assertEquals(15, blank[blob + 2177], "last step - 1");
        }
    }

    @Test
    void secondBlankPatternIsEqualToTheFirst() throws IOException {
        assertArrayEquals(sample(0), sample(13));
    }

    @Test
    void noteIsCentsAtTheStartOfAStep() throws IOException {
        assertOneChange(1, 12, 0xc0, 0x12);                 // CH1 step 1, C-4 = 4800
        assertOneChange(2, 12, 0x20, 0x1c);                 // CH1 step 1, C-6 = 7200
        assertOneChange(3, 12 + 34 * 15, 0x88, 0x13);       // CH1 step 16, D-4 = 5000
        assertOneChange(4, 12 + 2298, 0x50, 0x14);          // CH2 step 1, E-4 = 5200
    }

    @Test
    void gateLengthIsPercentAtStepOffsetFour() throws IOException {
        assertOneChange(5, 16, 80);
        assertOneChange(7, 16, 0);
        assertOneChange(8, 16, 10);
    }

    @Test
    void mergeSetsBitOneOfTheGateFlagsOfTheFirstStep() throws IOException {
        assertOneChange(9, 22, 3);
    }

    @Test
    void lastStepIsInTheSecondFooterByte() throws IOException {
        assertOneChange(10, 12 + 2177, 7);
        assertOneChange(11, 12 + 2177, 63);
    }

    @Test
    void masterTrackLengthIsFileByteFive() throws IOException {
        assertOneChange(12, 5, 32);
    }
}
