package blackseq.convert;

import miditools.core.note.NoteEvent;

/**
 * One source note and its destination.
 *
 * @param sourceClip index of the clip in the list that {@code plan()} got
 * @param note       the source note, not transposed
 * @param channel    1 to 4
 * @param pattern    0 for the first pattern of the result
 * @param step       1 to 64
 * @param gateLength fraction of one step, more than 0.0 and not more than 1.0
 */
public record Assignment(int sourceClip, NoteEvent note,
                         int channel, int pattern, int step, double gateLength) {
    public Assignment {
        if (sourceClip < 0) {
            throw new IllegalArgumentException("sourceClip must be 0 or more: " + sourceClip);
        }
        if (note == null) {
            throw new IllegalArgumentException("note must not be null");
        }
        if (channel < 1 || channel > 4) {
            throw new IllegalArgumentException("channel must be 1 to 4: " + channel);
        }
        if (pattern < 0) {
            throw new IllegalArgumentException("pattern must be 0 or more: " + pattern);
        }
        if (step < 1 || step > 64) {
            throw new IllegalArgumentException("step must be 1 to 64: " + step);
        }
        if (!(gateLength > 0.0 && gateLength <= 1.0)) {
            throw new IllegalArgumentException("gateLength must be more than 0.0 and not more than 1.0: " + gateLength);
        }
    }
}
