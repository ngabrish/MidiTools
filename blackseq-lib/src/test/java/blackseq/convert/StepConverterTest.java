package blackseq.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blackseq.model.Pattern;
import blackseq.model.Step;
import blackseq.model.Track;
import java.util.List;
import miditools.core.note.NoteClip;
import miditools.core.note.NoteEvent;
import org.junit.jupiter.api.Test;

class StepConverterTest {

    private static final double SIXTEENTH = 0.25;
    private final StepConverter converter = new StepConverter();

    private static ConversionOptions options(ChordPolicy chords, OverflowPolicy overflow) {
        return new ConversionOptions(4, chords, overflow, 0);
    }

    /** A note that starts on a step (1 = first step of the clip) and is half a step long. */
    private static NoteEvent at(int step, int midiNote) {
        return new NoteEvent((step - 1) * SIXTEENTH, SIXTEENTH / 2, midiNote, 100);
    }

    private static NoteClip clip(double lengthBeats, NoteEvent... notes) {
        return new NoteClip("clip", lengthBeats, List.of(notes));
    }

    private static long count(ConversionPlan plan, String word) {
        return plan.warnings().stream().filter(w -> w.message().contains(word)).count();
    }

    // ----- the simple case -----

    @Test
    void notesOnTheGridGoToTheirStepsWithNoWarnings() {
        ConversionPlan plan = converter.plan(List.of(clip(4, at(1, 60), at(5, 62), at(16, 64))),
                ConversionOptions.defaults());
        assertEquals(List.of(16, 0, 0, 0), plan.channelSteps());
        assertEquals(3, plan.assignments().size());
        assertEquals(new Assignment(0, at(5, 62), 1, 0, 5, 0.5), plan.assignments().get(1));
        assertTrue(plan.dropped().isEmpty());
        assertTrue(plan.warnings().isEmpty());

        List<Pattern> patterns = converter.apply(plan);
        assertEquals(1, patterns.size());
        Track ch1 = patterns.get(0).tracks().get(0);
        assertEquals(16, ch1.length());
        assertEquals(16, ch1.steps().size());
        assertEquals(new Step(true, 60, 0.5), ch1.steps().get(0));
        assertEquals(new Step(false, 60, 0.5), ch1.steps().get(1), "a rest keeps the note before it");
        assertEquals(new Step(true, 62, 0.5), ch1.steps().get(4));
        assertEquals(new Step(false, 62, 0.5), ch1.steps().get(14));
        assertEquals(new Step(true, 64, 0.5), ch1.steps().get(15));
    }

    @Test
    void clipsGoToChannelsInSequenceAndChannelsWithNoClipAreMuted() {
        ConversionPlan plan = converter.plan(
                List.of(clip(4, at(1, 60)), clip(2, at(1, 40), at(8, 43))), ConversionOptions.defaults());
        assertEquals(List.of(16, 8, 0, 0), plan.channelSteps());
        Pattern p = converter.apply(plan).get(0);
        assertEquals(16, p.tracks().get(0).length());
        assertEquals(8, p.tracks().get(1).length());
        assertEquals(new Step(true, 43, 0.5), p.tracks().get(1).steps().get(7));
        for (int channel = 2; channel < 4; channel++) {
            assertEquals(16, p.tracks().get(channel).length());
            for (Step s : p.tracks().get(channel).steps()) {
                assertEquals(new Step(false, 36, 0.5), s);
            }
        }
    }

    @Test
    void stepsBeforeTheFirstNoteAreRestsWithThePitchOfThatNote() {
        Pattern p = converter.apply(converter.plan(List.of(clip(1, at(3, 70))), ConversionOptions.defaults())).get(0);
        assertEquals(new Step(false, 70, 0.5), p.tracks().get(0).steps().get(0));
        assertEquals(new Step(true, 70, 0.5), p.tracks().get(0).steps().get(2));
    }

    @Test
    void stepsPerBeatSetsTheResolution() {
        NoteEvent secondBeat = new NoteEvent(1.0, 0.5, 60, 100);
        ConversionPlan eighths = converter.plan(List.of(clip(4, secondBeat)),
                new ConversionOptions(2, ChordPolicy.HIGHEST, OverflowPolicy.SPLIT, 0));
        assertEquals(3, eighths.assignments().get(0).step());
        assertEquals(List.of(8, 0, 0, 0), eighths.channelSteps());
        assertEquals(StepConverter.LONGEST_GATE, eighths.assignments().get(0).gateLength());
    }

    @Test
    void transposeMovesTheNotesAndDropsWhatLeavesTheRange() {
        ConversionPlan plan = converter.plan(List.of(clip(1, at(1, 60), at(2, 120))),
                new ConversionOptions(4, ChordPolicy.HIGHEST, OverflowPolicy.SPLIT, 12));
        assertEquals(1, plan.assignments().size());
        assertEquals(List.of(at(2, 120)), plan.dropped());
        assertEquals(1, count(plan, "dropped"));
        assertEquals(72, converter.apply(plan).get(0).tracks().get(0).steps().get(0).midiNote());
    }

    // ----- gate length -----

    @Test
    void gateLengthIsTheNoteLengthAsAFractionOfAStep() {
        NoteEvent quarterStep = new NoteEvent(0, SIXTEENTH / 4, 60, 100);
        NoteEvent fullStep = new NoteEvent(SIXTEENTH, SIXTEENTH, 62, 100);
        NoteEvent veryShort = new NoteEvent(2 * SIXTEENTH, 0.0001, 64, 100);
        ConversionPlan plan = converter.plan(List.of(clip(1, quarterStep, fullStep, veryShort)),
                ConversionOptions.defaults());
        assertEquals(0.25, plan.assignments().get(0).gateLength());
        assertEquals(StepConverter.LONGEST_GATE, plan.assignments().get(1).gateLength(),
                "a note that fills its step does not get a full gate, so the next note starts again");
        assertEquals(0.01, plan.assignments().get(2).gateLength(), "a gate length is never 0");
        assertTrue(plan.warnings().isEmpty());
    }

    @Test
    void noteLongerThanOneStepIsShortenedWithAWarning() {
        NoteEvent halfNote = new NoteEvent(0, 2.0, 60, 100);
        ConversionPlan plan = converter.plan(List.of(clip(4, halfNote)), ConversionOptions.defaults());
        assertEquals(StepConverter.LONGEST_GATE, plan.assignments().get(0).gateLength());
        assertEquals(1, plan.warnings().size());
        assertTrue(plan.warnings().get(0).message().contains("shortened"), plan.warnings().get(0).message());
        assertTrue(plan.warnings().get(0).message().contains("8.00 steps"));
        assertEquals(1, plan.warnings().get(0).channel());
        assertEquals(0.0, plan.warnings().get(0).atBeats());
    }

    // ----- notes off the grid -----

    @Test
    void noteOffTheGridGoesToTheNearestStepWithAWarning() {
        NoteEvent late = new NoteEvent(SIXTEENTH * 1.4, SIXTEENTH / 2, 60, 100);    // nearest: step 2
        NoteEvent early = new NoteEvent(SIXTEENTH * 3.6, SIXTEENTH / 2, 62, 100);   // nearest: step 5
        ConversionPlan plan = converter.plan(List.of(clip(4, late, early)), ConversionOptions.defaults());
        assertEquals(2, plan.assignments().get(0).step());
        assertEquals(5, plan.assignments().get(1).step());
        assertEquals(2, count(plan, "moved"));
        assertTrue(plan.warnings().get(0).message().contains("step position 2.40 and goes to step 2"),
                plan.warnings().get(0).message());
    }

    @Test
    void noteThatRoundsPastTheEndOfTheClipStaysOnTheLastStep() {
        NoteEvent atTheEnd = new NoteEvent(SIXTEENTH * 15.7, SIXTEENTH / 4, 60, 100);
        ConversionPlan plan = converter.plan(List.of(clip(4, atTheEnd)), ConversionOptions.defaults());
        assertEquals(List.of(16, 0, 0, 0), plan.channelSteps());
        assertEquals(16, plan.assignments().get(0).step());
        assertEquals(0, plan.assignments().get(0).pattern());
        assertEquals(1, count(plan, "moved"));
    }

    @Test
    void tinyTimingErrorsAreNotReported() {
        NoteEvent n = new NoteEvent(SIXTEENTH * 3 + 1e-9, SIXTEENTH / 2, 60, 100);
        ConversionPlan plan = converter.plan(List.of(clip(4, n)), ConversionOptions.defaults());
        assertEquals(4, plan.assignments().get(0).step());
        assertTrue(plan.warnings().isEmpty());
    }

    // ----- chords -----

    private ConversionPlan chord(ChordPolicy policy, NoteClip... clips) {
        return converter.plan(List.of(clips), options(policy, OverflowPolicy.SPLIT));
    }

    @Test
    void chordKeepsOneNoteAndDropsTheOthersWithAWarningForEach() {
        NoteClip c = clip(1, at(1, 64), at(1, 60), at(1, 67));
        ConversionPlan highest = chord(ChordPolicy.HIGHEST, c);
        assertEquals(67, highest.assignments().get(0).note().midiNote());
        assertEquals(List.of(at(1, 64), at(1, 60)), highest.dropped());
        assertEquals(2, count(highest, "dropped"));
        assertTrue(highest.warnings().get(0).message().contains("chord policy HIGHEST"));

        assertEquals(60, chord(ChordPolicy.LOWEST, c).assignments().get(0).note().midiNote());
        assertEquals(64, chord(ChordPolicy.FIRST, c).assignments().get(0).note().midiNote(),
                "same start: the first note in the clip");
        assertEquals(1, chord(ChordPolicy.LOWEST, c).assignments().size());
    }

    @Test
    void firstPolicyUsesTheStartTimeWhenTwoNotesRoundToOneStep() {
        NoteEvent later = new NoteEvent(SIXTEENTH * 0.3, SIXTEENTH / 2, 72, 100);
        NoteEvent earlier = new NoteEvent(SIXTEENTH * 0.1, SIXTEENTH / 2, 60, 100);
        ConversionPlan plan = chord(ChordPolicy.FIRST, clip(1, later, earlier));
        assertEquals(60, plan.assignments().get(0).note().midiNote());
        assertEquals(List.of(later), plan.dropped());
    }

    @Test
    void spreadPutsTheOtherChordNotesOnChannelsWithNoClip() {
        ConversionPlan plan = chord(ChordPolicy.SPREAD, clip(1, at(1, 60), at(1, 64), at(1, 67), at(2, 62)));
        assertEquals(4, plan.assignments().size());
        assertTrue(plan.dropped().isEmpty());
        assertEquals(new Assignment(0, at(1, 67), 1, 0, 1, 0.5), plan.assignments().get(0));
        assertEquals(new Assignment(0, at(2, 62), 1, 0, 2, 0.5), plan.assignments().get(1));
        assertEquals(new Assignment(0, at(1, 64), 2, 0, 1, 0.5), plan.assignments().get(2));
        assertEquals(new Assignment(0, at(1, 60), 3, 0, 1, 0.5), plan.assignments().get(3));
        assertEquals(List.of(4, 4, 4, 0), plan.channelSteps());
        assertEquals(2, count(plan, "moved: chord note goes to CH"));

        Pattern p = converter.apply(plan).get(0);
        assertEquals(new Step(true, 64, 0.5), p.tracks().get(1).steps().get(0));
        assertEquals(4, p.tracks().get(1).length());
    }

    @Test
    void spreadDropsANoteWhenNoChannelIsFree() {
        NoteClip five = clip(1, at(1, 60), at(1, 62), at(1, 64), at(1, 65), at(1, 67));
        ConversionPlan oneClip = chord(ChordPolicy.SPREAD, five);
        assertEquals(4, oneClip.assignments().size());
        assertEquals(List.of(at(1, 60)), oneClip.dropped());
        assertTrue(oneClip.warnings().stream().anyMatch(w -> w.message().contains("no channel is free")));

        NoteClip other = clip(1, at(1, 40));
        ConversionPlan fourClips = chord(ChordPolicy.SPREAD, clip(1, at(1, 60), at(1, 64)), other, other, other);
        assertEquals(List.of(at(1, 60)), fourClips.dropped(), "a channel with a clip is never a spread target");
    }

    // ----- pattern end and long clips -----

    @Test
    void noteThatCrossesThePatternEndIsShortenedWithAWarning() {
        NoteEvent overTheEnd = new NoteEvent(63 * SIXTEENTH, 4 * SIXTEENTH, 60, 100);   // steps 64 to 67
        ConversionPlan plan = converter.plan(List.of(clip(32, overTheEnd)), ConversionOptions.defaults());
        assertEquals(new Assignment(0, overTheEnd, 1, 0, 64, StepConverter.LONGEST_GATE),
                plan.assignments().get(0));
        assertEquals(1, plan.warnings().size());
        assertTrue(plan.warnings().get(0).message().contains("crosses the end of the pattern at step 64"),
                plan.warnings().get(0).message());
    }

    @Test
    void noteThatCrossesTheEndOfAShortClipIsShortenedWithAWarning() {
        NoteEvent overTheEnd = new NoteEvent(15 * SIXTEENTH, 2 * SIXTEENTH, 60, 100);
        ConversionPlan plan = converter.plan(List.of(clip(4, overTheEnd)), ConversionOptions.defaults());
        assertTrue(plan.warnings().get(0).message().contains("crosses the end of the pattern at step 16"),
                plan.warnings().get(0).message());
    }

    private static NoteClip longClip() {
        // 100 steps: notes on steps 1, 64, 65 and 100.
        return clip(25, at(1, 60), at(64, 61), at(65, 62), at(100, 63));
    }

    @Test
    void splitPutsALongClipIntoMorePatterns() {
        ConversionPlan plan = converter.plan(List.of(longClip()), options(ChordPolicy.HIGHEST, OverflowPolicy.SPLIT));
        assertEquals(List.of(100, 0, 0, 0), plan.channelSteps());
        assertEquals(new Assignment(0, at(64, 61), 1, 0, 64, 0.5), plan.assignments().get(1));
        assertEquals(new Assignment(0, at(65, 62), 1, 1, 1, 0.5), plan.assignments().get(2));
        assertEquals(new Assignment(0, at(100, 63), 1, 1, 36, 0.5), plan.assignments().get(3));
        assertTrue(plan.warnings().isEmpty());

        List<Pattern> patterns = converter.apply(plan);
        assertEquals(2, patterns.size());
        assertEquals(64, patterns.get(0).tracks().get(0).length());
        assertEquals(36, patterns.get(1).tracks().get(0).length());
        assertEquals(new Step(true, 62, 0.5), patterns.get(1).tracks().get(0).steps().get(0));
        assertEquals(new Step(false, 62, 0.5), patterns.get(1).tracks().get(0).steps().get(1),
                "the carried note continues in the next pattern");
    }

    @Test
    void truncateKeepsOnePatternAndReportsEachDroppedNote() {
        ConversionPlan plan = converter.plan(List.of(longClip()), options(ChordPolicy.HIGHEST, OverflowPolicy.TRUNCATE));
        assertEquals(List.of(64, 0, 0, 0), plan.channelSteps());
        assertEquals(2, plan.assignments().size());
        assertEquals(List.of(at(65, 62), at(100, 63)), plan.dropped());
        assertEquals(2, count(plan, "TRUNCATE"));
        assertEquals(1, converter.apply(plan).size());
    }

    @Test
    void failStopsOnALongClip() {
        ConversionException e = assertThrows(ConversionException.class,
                () -> converter.plan(List.of(longClip()), options(ChordPolicy.HIGHEST, OverflowPolicy.FAIL)));
        assertTrue(e.getMessage().contains("100 steps"), e.getMessage());
        // A clip of exactly 64 steps is not an overflow.
        converter.plan(List.of(clip(16, at(64, 60))), options(ChordPolicy.HIGHEST, OverflowPolicy.FAIL));
    }

    @Test
    void shorterClipIsMutedInTheLaterPatterns() {
        ConversionPlan plan = converter.plan(List.of(longClip(), clip(4, at(1, 40))),
                options(ChordPolicy.HIGHEST, OverflowPolicy.SPLIT));
        List<Pattern> patterns = converter.apply(plan);
        assertEquals(16, patterns.get(0).tracks().get(1).length());
        Track later = patterns.get(1).tracks().get(1);
        assertEquals(16, later.length());
        assertTrue(later.steps().stream().noneMatch(Step::gateOn));
    }

    // ----- empty clips -----

    @Test
    void emptyClipGivesAMutedTrackOfItsLength() {
        ConversionPlan plan = converter.plan(List.of(clip(2)), ConversionOptions.defaults());
        assertTrue(plan.assignments().isEmpty());
        assertTrue(plan.warnings().isEmpty());
        assertEquals(List.of(8, 0, 0, 0), plan.channelSteps());
        List<Pattern> patterns = converter.apply(plan);
        assertEquals(1, patterns.size());
        assertEquals(8, patterns.get(0).tracks().get(0).length());
        assertTrue(patterns.get(0).tracks().get(0).steps().stream().noneMatch(Step::gateOn));
    }

    @Test
    void clipWithNoLengthAndNoNotesGivesOneMutedPattern() {
        List<Pattern> patterns = converter.apply(converter.plan(List.of(clip(0)), ConversionOptions.defaults()));
        assertEquals(1, patterns.size());
        assertEquals(16, patterns.get(0).tracks().get(0).length());
    }

    @Test
    void clipShorterThanItsNotesGrowsToHoldThem() {
        ConversionPlan plan = converter.plan(List.of(clip(0, at(1, 60))), ConversionOptions.defaults());
        assertEquals(List.of(1, 0, 0, 0), plan.channelSteps());
        assertEquals(2, converter.apply(plan).get(0).tracks().get(0).length(), "a track has a minimum of 2 steps");
    }

    @Test
    void planNeedsOneToFourClips() {
        NoteClip c = clip(1);
        assertThrows(IllegalArgumentException.class, () -> converter.plan(List.of(), ConversionOptions.defaults()));
        assertThrows(IllegalArgumentException.class,
                () -> converter.plan(List.of(c, c, c, c, c), ConversionOptions.defaults()));
    }

    // ----- a program changes the plan -----

    @Test
    void planCanBeChangedBeforeApply() {
        ConversionPlan plan = converter.plan(List.of(clip(4, at(1, 60), at(2, 62))), ConversionOptions.defaults());
        Assignment moved = new Assignment(0, at(2, 62), 3, 0, 9, 0.75);
        ConversionPlan changed = plan.withAssignment(1, moved);
        assertEquals(2, plan.assignments().get(1).step(), "the first plan is not changed");
        assertTrue(converter.validate(changed).isEmpty());

        Pattern p = converter.apply(changed).get(0);
        assertEquals(new Step(true, 62, 0.75), p.tracks().get(2).steps().get(8));
        assertFalse(p.tracks().get(0).steps().get(1).gateOn());
        assertEquals(16, p.tracks().get(2).length(), "the track grows to hold a moved note");

        ConversionPlan dropped = plan.withDropped(0);
        assertEquals(1, dropped.assignments().size());
        assertEquals(List.of(at(1, 60)), dropped.dropped());
        assertEquals(1, converter.apply(dropped).size());
    }

    @Test
    void twoNotesOnOneStepAreAConflictAndApplyRefusesThePlan() {
        ConversionPlan plan = converter.plan(List.of(clip(4, at(1, 60), at(2, 62))), ConversionOptions.defaults());
        ConversionPlan conflict = plan.withAssignment(1, new Assignment(0, at(2, 62), 1, 0, 1, 0.5));
        List<ConversionWarning> found = converter.validate(conflict);
        assertEquals(1, found.size());
        assertTrue(found.get(0).message().contains("CH1 pattern 0 step 1 has two notes"), found.get(0).message());
        assertThrows(ConversionException.class, () -> converter.apply(conflict));
    }

    @Test
    void recordsRejectValuesOutsideTheirRange() {
        NoteEvent n = at(1, 60);
        assertThrows(IllegalArgumentException.class, () -> new Assignment(0, n, 0, 0, 1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new Assignment(0, n, 5, 0, 1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new Assignment(0, n, 1, -1, 1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new Assignment(0, n, 1, 0, 65, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new Assignment(0, n, 1, 0, 1, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Assignment(0, n, 1, 0, 1, 1.5));
        assertThrows(IllegalArgumentException.class,
                () -> new ConversionOptions(0, ChordPolicy.HIGHEST, OverflowPolicy.SPLIT, 0));
    }
}
