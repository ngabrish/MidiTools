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
        assertOneChange(8, 16, 100);
    }

    @Test
    void gateFlagsHoldMuteInBitZeroAndMergeInBitOne() throws IOException {
        assertOneChange(6, 22, 0);   // step 1 muted
        assertOneChange(9, 22, 3);   // steps 1 and 2 merged: only step 1 changes
    }

    @Test
    void mergeOfThreeStepsSetsBitOneOnAllStepsExceptTheLast() throws IOException {
        // 14.SEQ: CH1 steps 49 to 51 merged.
        List<ByteDiff.Range> ranges = ByteDiff.diff(sample(0), sample(14));
        assertEquals(2, ranges.size());
        assertEquals(12 + 34 * 48 + 10, ranges.get(0).offset());
        assertEquals(12 + 34 * 49 + 10, ranges.get(1).offset());
        assertArrayEquals(new byte[] {3}, ranges.get(0).b());
        assertArrayEquals(new byte[] {3}, ranges.get(1).b());
        assertEquals(1, sample(14)[12 + 34 * 50 + 10], "the last merged step keeps gate flags 1");
    }

    @Test
    void lastStepIsInTheSecondFooterByte() throws IOException {
        assertOneChange(10, 12 + 2177, 7);
        assertEquals(63, sample(11)[12 + 2177]);
    }

    @Test
    void stepSixtyFourNoteIsSixtyThreeStepsAfterStepOne() throws IOException {
        List<ByteDiff.Range> ranges = ByteDiff.diff(sample(0), sample(11));
        assertEquals(2, ranges.size());
        assertEquals(12 + 34 * 63, ranges.get(0).offset());
        assertArrayEquals(new byte[] {(byte) 0xb4, 0x14}, ranges.get(0).b());   // F-4 = 5300
        assertEquals(12 + 2177, ranges.get(1).offset());
    }

    @Test
    void masterTrackLengthIsFileByteFive() throws IOException {
        assertOneChange(12, 5, 32);
    }
}
