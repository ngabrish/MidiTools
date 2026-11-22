package miditools.core.note;

import java.util.List;

/**
 * A named group of notes with a length. This is the only input to the converters.
 *
 * @param name        label for messages, for example a MIDI track name
 * @param lengthBeats clip length in beats; a clip can be longer than its last note
 * @param notes       the notes, kept as an immutable copy
 */
public record NoteClip(String name, double lengthBeats, List<NoteEvent> notes) {
    public NoteClip {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!(lengthBeats >= 0)) {
            throw new IllegalArgumentException("lengthBeats must be 0 or more: " + lengthBeats);
        }
        notes = List.copyOf(notes);
    }
}
