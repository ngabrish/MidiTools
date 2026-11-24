package blackseq.model;

/**
 * Text for a pattern, as a person reads it: note names and one line for each track.
 * A command and a user interface use these functions, so that the two show the same text.
 */
public final class PatternText {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    /** Steps in one row of a long track. */
    private static final int ROW_STEPS = 16;

    private PatternText() {}

    /**
     * The note name as the module shows it: MIDI note 36 is {@code C-3}.
     *
     * @param midiNote 0 to 127
     */
    public static String noteName(int midiNote) {
        if (midiNote < 0 || midiNote > 127) {
            throw new IllegalArgumentException("midiNote must be 0 to 127: " + midiNote);
        }
        return NOTE_NAMES[midiNote % 12] + "-" + midiNote / 12;
    }

    /**
     * One line for a track: the steps that play, with the note names that the module shows.
     * A muted step is {@code .} and a step that continues a merged note is {@code ~}. A
     * track of more than 16 steps has one row for each 16 steps.
     *
     * @param channel 1 to 4
     */
    public static String trackLine(int channel, Track track) {
        StringBuilder sb = new StringBuilder("CH" + channel + " (" + track.length() + " steps):");
        boolean any = false;
        for (int s = 0; s < track.length(); s++) {
            Step step = track.steps().get(s);
            boolean held = s > 0 && track.steps().get(s - 1).mergeNext() && track.steps().get(s - 1).gateOn();
            if (s % ROW_STEPS == 0 && track.length() > ROW_STEPS) {
                sb.append(System.lineSeparator()).append("     ");
            }
            sb.append(' ');
            if (!step.gateOn()) {
                sb.append('.');
            } else if (held) {
                sb.append('~');
                any = true;
            } else {
                sb.append(noteName(step.midiNote()));
                any = true;
            }
        }
        return any ? sb.toString() : "CH" + channel + " (" + track.length() + " steps): no notes";
    }
}
