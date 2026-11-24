package blackseq.convert;

/** What a {@link ConversionWarning} is about. */
public enum WarningKind {
    /** The note is not in the result. */
    DROPPED,
    /** The note is on a different step or a different channel. */
    MOVED,
    /** The note holds fewer steps than its length. */
    SHORTENED,
    /** A changed plan has a problem that {@code StepConverter.validate()} found. */
    CONFLICT
}
