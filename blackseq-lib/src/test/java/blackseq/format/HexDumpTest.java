package blackseq.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HexDumpTest {

    @Test
    void fullRowShowsOffsetHexAndAscii() {
        byte[] data = "CHROMATIC\0\0\0\0\0\0\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(
                "00000000      0  43 48 52 4f 4d 41 54 49  43 00 00 00 00 00 00 00 |CHROMATIC.......|\n",
                HexDump.format(data));
    }

    @Test
    void shortLastRowKeepsTheAsciiColumnAligned() {
        byte[] data = new byte[18];
        data[16] = (byte) 0xFF;
        data[17] = 0x41;
        String[] lines = HexDump.format(data).split("\n");
        assertEquals(2, lines.length);
        assertEquals("00000010     16  ff 41 " + " ".repeat(43) + "|.A|", lines[1]);
        assertEquals(lines[0].indexOf('|'), lines[1].indexOf('|'));
    }

    @Test
    void rangeStartsAtTheGivenOffset() {
        byte[] data = new byte[300];
        data[258] = 0x7F;
        assertEquals("00000102    258  7f 00 " + " ".repeat(43) + "|..|\n",
                HexDump.format(data, 258, 2));
    }

    @Test
    void emptyInputGivesEmptyText() {
        assertEquals("", HexDump.format(new byte[0]));
    }

    @Test
    void rangeOutsideTheDataIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> HexDump.format(new byte[4], 2, 3));
        assertThrows(IllegalArgumentException.class, () -> HexDump.format(new byte[4], -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> HexDump.format(new byte[4], 2, Integer.MAX_VALUE));
    }
}
