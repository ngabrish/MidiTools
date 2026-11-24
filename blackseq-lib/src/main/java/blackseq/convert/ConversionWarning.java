package blackseq.convert;

import miditools.core.note.NoteEvent;

/**
 * One thing that the user must know about the result.
 *
 * @param kind       what the warning is about. This field is not in the brief: a user
 *                   interface filters and counts warnings by kind
 * @param channel    1 to 4: the channel of the source clip, or of the step for a plan conflict
 * @param atBeats    start of the note in its clip, in beats
 * @param message    full sentence in English
 * @param sourceClip index of the clip of the note in the list that {@code plan()} got. This
 *                   field and {@code note} are not in the brief: a user interface shows a
 *                   warning at the note that it is about
 * @param note       the source note, not transposed: the same object that {@code plan()} got
 */
public record ConversionWarning(WarningKind kind, int channel, double atBeats, String message,
                                int sourceClip, NoteEvent note) {
    public ConversionWarning {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (sourceClip < 0) {
            throw new IllegalArgumentException("sourceClip must be 0 or more: " + sourceClip);
        }
        if (note == null) {
            throw new IllegalArgumentException("note must not be null");
        }
    }
}
