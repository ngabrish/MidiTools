package blackseq.format;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the byte ranges that are different between two files. A format discovery tool: it
 * has no knowledge of the .SEQ layout.
 *
 * <p>The comparison is by position. It does not find inserted or removed bytes. If the two
 * inputs have different lengths, the bytes after the end of the shorter input make one
 * final range.
 */
public final class ByteDiff {

    /**
     * One run of changed bytes.
     *
     * @param offset position of the first changed byte
     * @param a      the bytes from the first input (shorter than {@code b} only at its end)
     * @param b      the bytes from the second input
     */
    public record Range(int offset, byte[] a, byte[] b) {
        /** Number of positions in this range. */
        public int length() {
            return Math.max(a.length, b.length);
        }

        /** Position of the last changed byte. */
        public int lastOffset() {
            return offset + length() - 1;
        }
    }

    private ByteDiff() {}

    /** The changed ranges in ascending offset order. Empty if the inputs are equal. */
    public static List<Range> diff(byte[] a, byte[] b) {
        List<Range> ranges = new ArrayList<>();
        int common = Math.min(a.length, b.length);
        int start = -1;
        for (int i = 0; i < common; i++) {
            if (a[i] != b[i]) {
                if (start < 0) {
                    start = i;
                }
            } else if (start >= 0) {
                ranges.add(range(a, b, start, i));
                start = -1;
            }
        }
        if (a.length != b.length) {
            // The tail joins a run that reaches the end of the shorter input.
            int from = start >= 0 ? start : common;
            ranges.add(range(a, b, from, Math.max(a.length, b.length)));
        } else if (start >= 0) {
            ranges.add(range(a, b, start, common));
        }
        return ranges;
    }

    /** Text report of the changed ranges, one line for each range. Lines end with {@code \n}. */
    public static String format(byte[] a, byte[] b) {
        List<Range> ranges = diff(a, b);
        StringBuilder out = new StringBuilder();
        out.append("size a: ").append(a.length).append(" bytes, size b: ").append(b.length)
                .append(" bytes\n");
        if (ranges.isEmpty()) {
            out.append("no differences\n");
            return out.toString();
        }
        int changed = 0;
        for (Range r : ranges) {
            changed += r.length();
            out.append(String.format("%08x..%08x  %6d..%-6d  %4d byte%s  a: %s  b: %s%n",
                    r.offset(), r.lastOffset(), r.offset(), r.lastOffset(),
                    r.length(), r.length() == 1 ? " " : "s",
                    hexOrNone(r.a()), hexOrNone(r.b())).replace(System.lineSeparator(), "\n"));
        }
        out.append(ranges.size()).append(ranges.size() == 1 ? " range, " : " ranges, ")
                .append(changed).append(changed == 1 ? " byte" : " bytes").append(" changed\n");
        return out.toString();
    }

    private static Range range(byte[] a, byte[] b, int from, int to) {
        return new Range(from, slice(a, from, to), slice(b, from, to));
    }

    private static byte[] slice(byte[] data, int from, int to) {
        int end = Math.min(to, data.length);
        if (from >= end) {
            return new byte[0];
        }
        return java.util.Arrays.copyOfRange(data, from, end);
    }

    private static String hexOrNone(byte[] bytes) {
        return bytes.length == 0 ? "(none)" : HexDump.hex(bytes, 0, bytes.length);
    }
}
