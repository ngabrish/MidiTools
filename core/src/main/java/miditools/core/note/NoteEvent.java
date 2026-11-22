package miditools.core.note;

/**
 * One note, independent of its source (MIDI file, DAW clip) and of any device.
 *
 * @param startBeats  start position in beats (quarter notes) from the start of the clip
 * @param lengthBeats duration in beats
 * @param midiNote    MIDI note number, 0 to 127
 * @param velocity    MIDI velocity, 1 to 127
 */
public record NoteEvent(double startBeats, double lengthBeats, int midiNote, int velocity) {
    public NoteEvent {
        if (!(startBeats >= 0)) {
            throw new IllegalArgumentException("startBeats must be 0 or more: " + startBeats);
        }
        if (!(lengthBeats > 0)) {
            throw new IllegalArgumentException("lengthBeats must be more than 0: " + lengthBeats);
        }
        if (midiNote < 0 || midiNote > 127) {
            throw new IllegalArgumentException("midiNote must be 0 to 127: " + midiNote);
        }
        if (velocity < 1 || velocity > 127) {
            throw new IllegalArgumentException("velocity must be 1 to 127: " + velocity);
        }
    }
}
