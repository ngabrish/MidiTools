package blackseq.format;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ByteDiffTest {

    @Test
    void equalInputsHaveNoRanges() {
        assertTrue(ByteDiff.diff(new byte[] {1, 2, 3}, new byte[] {1, 2, 3}).isEmpty());
        assertTrue(ByteDiff.diff(new byte[0], new byte[0]).isEmpty());
    }

    @Test
    void adjacentChangedBytesMakeOneRange() {
        byte[] a = {0, 1, 2, 3, 4, 5, 6, 7};
        byte[] b = {0, 9, 9, 3, 4, 5, 8, 7};
        List<ByteDiff.Range> ranges = ByteDiff.diff(a, b);
        assertEquals(2, ranges.size());
        assertEquals(1, ranges.get(0).offset());
        assertEquals(2, ranges.get(0).lastOffset());
        assertArrayEquals(new byte[] {1, 2}, ranges.get(0).a());
        assertArrayEquals(new byte[] {9, 9}, ranges.get(0).b());
        assertEquals(6, ranges.get(1).offset());
        assertEquals(1, ranges.get(1).length());
    }

    @Test
    void changeAtFirstAndLastByteIsFound() {
        List<ByteDiff.Range> ranges = ByteDiff.diff(new byte[] {1, 0, 1}, new byte[] {2, 0, 2});
        assertEquals(2, ranges.size());
        assertEquals(0, ranges.get(0).offset());
        assertEquals(2, ranges.get(1).offset());
    }

    @Test
    void longerSecondInputGivesATailRange() {
        List<ByteDiff.Range> ranges = ByteDiff.diff(new byte[] {1, 2}, new byte[] {1, 2, 3, 4});
        assertEquals(1, ranges.size());
        assertEquals(2, ranges.get(0).offset());
        assertEquals(2, ranges.get(0).length());
        assertEquals(0, ranges.get(0).a().length);
        assertArrayEquals(new byte[] {3, 4}, ranges.get(0).b());
    }

    @Test
    void tailJoinsAChangeAtTheEndOfTheShorterInput() {
        List<ByteDiff.Range> ranges = ByteDiff.diff(new byte[] {1, 2, 3}, new byte[] {1, 9});
        assertEquals(1, ranges.size());
        assertEquals(1, ranges.get(0).offset());
        assertArrayEquals(new byte[] {2, 3}, ranges.get(0).a());
        assertArrayEquals(new byte[] {9}, ranges.get(0).b());
    }

    @Test
    void reportShowsOffsetsInHexAndDecimal() {
        byte[] a = new byte[20];
        byte[] b = new byte[20];
        b[17] = (byte) 0xAB;
        b[18] = 0x01;
        assertEquals(
                "size a: 20 bytes, size b: 20 bytes\n"
                        + "00000011..00000012      17..18         2 bytes  a: 00 00  b: ab 01\n"
                        + "1 range, 2 bytes changed\n",
                ByteDiff.format(a, b));
    }

    @Test
    void reportForEqualInputsSaysNoDifferences() {
        assertEquals("size a: 1 bytes, size b: 1 bytes\nno differences\n",
                ByteDiff.format(new byte[1], new byte[1]));
    }
}
