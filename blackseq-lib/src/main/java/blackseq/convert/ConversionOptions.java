package blackseq.convert;

/**
 * @param stepsPerBeat step resolution: 4 makes each step a sixteenth note
 * @param chords       policy for notes on the same step
 * @param overflow     policy for clips longer than 64 steps
 * @param transpose    semitones that are added to each note
 */
public record ConversionOptions(int stepsPerBeat, ChordPolicy chords,
                                OverflowPolicy overflow, int transpose) {
    public ConversionOptions {
        if (stepsPerBeat < 1) {
            throw new IllegalArgumentException("stepsPerBeat must be 1 or more: " + stepsPerBeat);
        }
        if (chords == null || overflow == null) {
            throw new IllegalArgumentException("chords and overflow must not be null");
        }
    }

    /** Sixteenth-note steps, highest note of a chord, split into patterns, no transpose. */
    public static ConversionOptions defaults() {
        return new ConversionOptions(4, ChordPolicy.HIGHEST, OverflowPolicy.SPLIT, 0);
    }
}
