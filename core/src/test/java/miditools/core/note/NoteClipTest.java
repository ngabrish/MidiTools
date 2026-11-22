package miditools.core.note;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoteClipTest {

    @Test
    void clipKeepsAnImmutableCopyOfItsNotes() {
        List<NoteEvent> source = new ArrayList<>(List.of(new NoteEvent(0, 1, 60, 100)));
        NoteClip clip = new NoteClip("lead", 4, source);
        source.clear();
        assertEquals(1, clip.notes().size());
        assertThrows(UnsupportedOperationException.class, () -> clip.notes().clear());
    }

    @Test
    void invalidNoteValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NoteEvent(-1, 1, 60, 100));
        assertThrows(IllegalArgumentException.class, () -> new NoteEvent(0, 0, 60, 100));
        assertThrows(IllegalArgumentException.class, () -> new NoteEvent(0, 1, 128, 100));
        assertThrows(IllegalArgumentException.class, () -> new NoteEvent(0, 1, 60, 0));
        assertThrows(IllegalArgumentException.class, () -> new NoteEvent(Double.NaN, 1, 60, 100));
    }
}
