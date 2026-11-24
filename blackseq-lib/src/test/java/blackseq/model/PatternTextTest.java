package blackseq.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatternTextTest {

    @Test
    void noteNamesAreTheNamesThatTheModuleShows() {
        assertEquals("C-3", PatternText.noteName(36));
        assertEquals("C-4", PatternText.noteName(48));
        assertEquals("D-4", PatternText.noteName(50));
        assertEquals("G#-5", PatternText.noteName(68));
        assertEquals("C-0", PatternText.noteName(0));
        assertEquals("G-10", PatternText.noteName(127));
        assertThrows(IllegalArgumentException.class, () -> PatternText.noteName(-1));
        assertThrows(IllegalArgumentException.class, () -> PatternText.noteName(128));
    }

    @Test
    void trackLineShowsNotesMutedStepsAndMergedSteps() {
        Track track = new Track(4, List.of(
                new Step(true, 36, 0.99, true),
                new Step(true, 36, 0.5, false),
                new Step(false, 36, 0.5),
                new Step(true, 50, 0.5)));
        assertEquals("CH2 (4 steps): C-3 ~ . D-4", PatternText.trackLine(2, track));
    }

    @Test
    void trackWithNoGateHasNoNotes() {
        Track track = new Track(2, List.of(new Step(false, 36, 0.5), new Step(false, 36, 0.5)));
        assertEquals("CH1 (2 steps): no notes", PatternText.trackLine(1, track));
    }

    @Test
    void stepsAfterTheTrackLengthAreNotShown() {
        Track track = new Track(2, List.of(
                new Step(true, 36, 0.5), new Step(false, 36, 0.5), new Step(true, 48, 0.5)));
        assertEquals("CH1 (2 steps): C-3 .", PatternText.trackLine(1, track));
    }

    @Test
    void longTrackHasOneRowForEach16Steps() {
        List<Step> steps = new ArrayList<>();
        for (int s = 0; s < 32; s++) {
            steps.add(new Step(s == 0 || s == 16, 36, 0.5));
        }
        String nl = System.lineSeparator();
        assertEquals("CH1 (32 steps):" + nl + "      C-3" + " .".repeat(15) + nl + "      C-3" + " .".repeat(15),
                PatternText.trackLine(1, new Track(32, steps)));
    }
}
