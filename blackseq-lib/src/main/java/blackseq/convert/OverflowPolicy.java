package blackseq.convert;

/** What the converter does with a clip that is longer than one pattern (64 steps). */
public enum OverflowPolicy {
    /** Keep the first 64 steps. Drop the notes after them. */
    TRUNCATE,
    /** Continue in the next pattern. */
    SPLIT,
    /** Stop with a {@link ConversionException}. */
    FAIL
}
