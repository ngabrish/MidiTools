package blackseq.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blackseq.model.Pattern;
import blackseq.model.Step;
import java.util.List;
import miditools.core.note.NoteClip;
import miditools.core.note.NoteEvent;
import org.junit.jupiter.api.Test;

/** Each edit gives a note that the converter puts at the position that the user named. */
class NoteEditsTest {

    private final StepConverter converter = new StepConverter();

    private static ConversionOptions options(int stepsPerBeat, int transpose) {
        return new ConversionOptions(stepsPerBeat, ChordPolicy.HIGHEST, OverflowPolicy.SPLIT, transpose);
    }

    private Assignment only(NoteEvent note, ConversionOptions options, double clipBeats) {
        ConversionPlan plan = converter.plan(List.of(new NoteClip("clip", clipBeats, List.of(note))), options);
        assertTrue(plan.warnings().isEmpty(), plan.warnings().toString());
        assertEquals(1, plan.assignments().size());
        return plan.assignments().get(0);
    }

    @Test
    void movedNoteGoesToThePatternAndStepForEachStepResolution() {
        NoteEvent note = new NoteEvent(0.3, 0.01, 60, 90);   // shorter than one step at each resolution
        for (int stepsPerBeat : new int[] {1, 2, 3, 4, 6, 7, 8, 12, 24}) {
            ConversionOptions options = options(stepsPerBeat, 0);
            for (int pattern = 0; pattern < 3; pattern++) {
                for (int step = 1; step <= 64; step++) {
                    NoteEvent moved = NoteEdits.moved(note, options, pattern, step);
                    Assignment a = only(moved, options, 3 * 64.0 / stepsPerBeat);
                    assertEquals(pattern, a.pattern(), "spb " + stepsPerBeat);
                    assertEquals(step, a.step(), "spb " + stepsPerBeat);
                    assertEquals(note.lengthBeats(), moved.lengthBeats());
                    assertEquals(60, moved.midiNote());
                    assertEquals(90, moved.velocity());
                }
            }
        }
    }

    @Test
    void resizedNoteHoldsTheNumberOfStepsAndKeepsItsStart() {
        for (int stepsPerBeat : new int[] {1, 3, 4, 7, 24}) {
            ConversionOptions options = options(stepsPerBeat, 0);
            NoteEvent note = NoteEdits.created(options, 0, 5, 1, 60);
            for (int steps = 1; steps <= 60; steps++) {
                NoteEvent resized = NoteEdits.resized(note, options, steps);
                assertEquals(note.startBeats(), resized.startBeats());
                Assignment a = only(resized, options, 64.0 / stepsPerBeat);
                assertEquals(5, a.step());
                assertEquals(steps, a.steps(), "spb " + stepsPerBeat);
                assertEquals(StepConverter.LONGEST_GATE, a.gateLength());
            }
        }
    }

    @Test
    void repitchedNoteGivesThePatternNoteWithTheTransposeOfTheOptions() {
        ConversionOptions options = options(4, 12);
        NoteEvent note = new NoteEvent(0, 0.25, 40, 100);
        NoteEvent repitched = NoteEdits.repitched(note, options, 60);
        assertEquals(48, repitched.midiNote());
        ConversionPlan plan = converter.plan(List.of(new NoteClip("clip", 4, List.of(repitched))), options);
        Pattern p = converter.apply(plan).get(0);
        assertEquals(60, p.tracks().get(0).steps().get(0).midiNote());
    }

    @Test
    void createdNoteIsInThePatternAtItsStepWithItsLengthAndPitch() {
        ConversionOptions options = options(4, -2);
        NoteEvent created = NoteEdits.created(options, 1, 9, 3, 50);
        assertEquals(NoteEdits.NEW_NOTE_VELOCITY, created.velocity());
        ConversionPlan plan = converter.plan(List.of(new NoteClip("clip", 32, List.of(created))), options);
        assertTrue(plan.warnings().isEmpty());
        List<Step> steps = converter.apply(plan).get(1).tracks().get(0).steps();
        assertFalse(steps.get(7).gateOn());
        for (int s = 8; s <= 10; s++) {
            assertTrue(steps.get(s).gateOn());
            assertEquals(50, steps.get(s).midiNote());
        }
        assertTrue(steps.get(8).mergeNext());
        assertTrue(steps.get(9).mergeNext());
        assertFalse(steps.get(10).mergeNext());
        assertFalse(steps.get(11).gateOn());
    }

    @Test
    void valuesOutsideTheRangesAreRefused() {
        ConversionOptions options = options(4, 0);
        NoteEvent note = new NoteEvent(0, 0.25, 60, 100);
        assertThrows(IllegalArgumentException.class, () -> NoteEdits.moved(note, options, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> NoteEdits.moved(note, options, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> NoteEdits.moved(note, options, 0, 65));
        assertThrows(IllegalArgumentException.class, () -> NoteEdits.resized(note, options, 0));
        assertThrows(IllegalArgumentException.class, () -> NoteEdits.repitched(note, options, -1));
        assertThrows(IllegalArgumentException.class, () -> NoteEdits.repitched(note, options, 128));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> NoteEdits.repitched(note, options(4, 24), 10));
        assertTrue(e.getMessage().contains("source note -14"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> NoteEdits.created(options, 0, 1, 0, 60));
    }
}
