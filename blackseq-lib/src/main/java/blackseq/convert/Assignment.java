package blackseq.convert;

import miditools.core.note.NoteEvent;

/**
 * One source note and its destination.
 *
 * @param sourceClip index of the clip in the list that {@code plan()} got
 * @param note       the source note, not transposed
 * @param channel    1 to 4
 * @param pattern    0 for the first pattern of the result
 * @param step       first step of the note, 1 to 64
 * @param steps      number of steps that the note holds, 1 or more. With more than 1, the
 *                   steps are merged. The last step must not be after step 64. This field
 *                   is not in the brief: it holds a note that is longer than one step
 * @param gateLength gate length of the last step of the note, as a fraction of one step,
 *                   more than 0.0 and not more than 1.0
 */
public record Assignment(int sourceClip, NoteEvent note,
                         int channel, int pattern, int step, int steps, double gateLength) {
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
        if (steps < 1 || step + steps - 1 > 64) {
            throw new IllegalArgumentException(
                    "steps must be 1 or more and end at step 64 or before: step " + step + ", steps " + steps);
        }
        if (!(gateLength > 0.0 && gateLength <= 1.0)) {
            throw new IllegalArgumentException("gateLength must be more than 0.0 and not more than 1.0: " + gateLength);
        }
    }

    /** A note that holds one step. */
    public Assignment(int sourceClip, NoteEvent note, int channel, int pattern, int step, double gateLength) {
        this(sourceClip, note, channel, pattern, step, 1, gateLength);
    }

    /** The last step of the note, 1 to 64. */
    public int lastStep() {
        return step + steps - 1;
    }
}
