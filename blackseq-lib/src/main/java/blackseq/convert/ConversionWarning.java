package blackseq.convert;

/**
 * One thing that the user must know about the result.
 *
 * @param channel 1 to 4: the channel of the source clip, or of the step for a plan conflict
 * @param atBeats start of the note in its clip, in beats
 * @param message full sentence in English
 */
public record ConversionWarning(int channel, double atBeats, String message) {}
