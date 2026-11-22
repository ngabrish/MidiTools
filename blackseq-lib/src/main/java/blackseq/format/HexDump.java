package blackseq.format;

/**
 * Hex view of raw bytes with offsets. A format discovery tool: it has no knowledge of the
 * .SEQ layout.
 */
public final class HexDump {

    private static final int BYTES_PER_ROW = 16;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HexDump() {}

    /** Hex view of all bytes. */
    public static String format(byte[] data) {
        return format(data, 0, data.length);
    }

    /**
     * Hex view of {@code length} bytes that start at {@code offset}. Each row shows the offset
     * in hexadecimal and in decimal, 16 bytes, and the printable ASCII characters. Lines end
     * with {@code \n}.
     */
    public static String format(byte[] data, int offset, int length) {
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new IllegalArgumentException(
                    "range " + offset + "+" + length + " is outside the " + data.length + " bytes");
        }
        StringBuilder out = new StringBuilder();
        int end = offset + length;
        for (int row = offset; row < end; row += BYTES_PER_ROW) {
            out.append(String.format("%08x %6d  ", row, row));
            StringBuilder ascii = new StringBuilder();
            for (int i = 0; i < BYTES_PER_ROW; i++) {
                if (i == BYTES_PER_ROW / 2) {
                    out.append(' ');
                }
                int index = row + i;
                if (index < end) {
                    int value = data[index] & 0xFF;
                    out.append(HEX[value >> 4]).append(HEX[value & 0x0F]).append(' ');
                    ascii.append(value >= 0x20 && value < 0x7F ? (char) value : '.');
                } else {
                    out.append("   ");
                }
            }
            out.append('|').append(ascii).append("|\n");
        }
        return out.toString();
    }

    /** Bytes as two-digit hexadecimal values with a space between them. */
    public static String hex(byte[] data, int offset, int length) {
        StringBuilder out = new StringBuilder(length * 3);
        for (int i = offset; i < offset + length; i++) {
            if (i > offset) {
                out.append(' ');
            }
            int value = data[i] & 0xFF;
            out.append(HEX[value >> 4]).append(HEX[value & 0x0F]);
        }
        return out.toString();
    }
}
