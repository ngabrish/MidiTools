package blackseq.convert;

/**
 * One thing that the user must know about the result.
 *
 * @param kind    what the warning is about. This field is not in the brief: a user
 *                interface filters and counts warnings by kind
 * @param channel 1 to 4: the channel of the source clip, or of the step for a plan conflict
 * @param atBeats start of the note in its clip, in beats
 * @param message full sentence in English
 */
public record ConversionWarning(WarningKind kind, int channel, double atBeats, String message) {
    public ConversionWarning {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
    }
}
