package blackseq.model;

/**
 * One step of a track.
 *
 * @param gateOn     false for a muted step: the step plays no gate
 * @param midiNote   MIDI note number, 0 to 127. The module shows note 36 as C-3
 * @param gateLength gate length as a fraction of the step, 0.0 to 1.0. 1.0 is a full gate
 * @param mergeNext  true if the gate of this step continues into the next step with no
 *                   new start. A note of three steps has this on its first two steps
 */
public record Step(boolean gateOn, int midiNote, double gateLength, boolean mergeNext) {
    public Step {
        if (midiNote < 0 || midiNote > 127) {
            throw new IllegalArgumentException("midiNote must be 0 to 127: " + midiNote);
        }
        if (!(gateLength >= 0.0 && gateLength <= 1.0)) {
            throw new IllegalArgumentException("gateLength must be 0.0 to 1.0: " + gateLength);
        }
    }

    /** A step that is not merged with the next step. */
    public Step(boolean gateOn, int midiNote, double gateLength) {
        this(gateOn, midiNote, gateLength, false);
    }
}
