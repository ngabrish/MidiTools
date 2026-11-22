package blackseq.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelTest {

    private static final Step STEP = new Step(true, 36, 0.5);

    @Test
    void stepRejectsValuesOutsideTheRange() {
        assertThrows(IllegalArgumentException.class, () -> new Step(true, -1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new Step(true, 128, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new Step(true, 60, -0.01));
        assertThrows(IllegalArgumentException.class, () -> new Step(true, 60, 1.01));
        assertThrows(IllegalArgumentException.class, () -> new Step(true, 60, Double.NaN));
    }

    @Test
    void trackLengthIsTwoToSixtyFour() {
        assertThrows(IllegalArgumentException.class, () -> new Track(1, Collections.nCopies(64, STEP)));
        assertThrows(IllegalArgumentException.class, () -> new Track(65, Collections.nCopies(65, STEP)));
        assertEquals(2, new Track(2, Collections.nCopies(2, STEP)).length());
        assertEquals(64, new Track(64, Collections.nCopies(64, STEP)).length());
    }

    @Test
    void trackNeedsAStepForEachPlayedStepAndNoMoreThanSixtyFour() {
        assertThrows(IllegalArgumentException.class, () -> new Track(16, Collections.nCopies(15, STEP)));
        assertThrows(IllegalArgumentException.class, () -> new Track(16, Collections.nCopies(65, STEP)));
        assertEquals(16, new Track(16, Collections.nCopies(16, STEP)).steps().size());
        assertEquals(64, new Track(16, Collections.nCopies(64, STEP)).steps().size());
    }

    @Test
    void patternHasExactlyFourTracks() {
        Track track = new Track(16, Collections.nCopies(16, STEP));
        assertThrows(IllegalArgumentException.class, () -> new Pattern(Collections.nCopies(3, track)));
        assertThrows(IllegalArgumentException.class, () -> new Pattern(Collections.nCopies(5, track)));
        assertEquals(4, new Pattern(Collections.nCopies(4, track)).tracks().size());
    }

    @Test
    void listsAreImmutableCopies() {
        List<Step> steps = new ArrayList<>(Collections.nCopies(16, STEP));
        Track track = new Track(16, steps);
        steps.clear();
        assertEquals(16, track.steps().size());
        assertThrows(UnsupportedOperationException.class, () -> track.steps().clear());
    }
}
