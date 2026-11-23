package blackseq.convert;

import java.util.ArrayList;
import java.util.List;
import miditools.core.note.NoteEvent;

/**
 * Where each note goes. {@code StepConverter.plan()} makes a plan, a program can change it,
 * and {@code StepConverter.apply()} builds the patterns from it.
 *
 * @param options      the options that made the plan
 * @param channelSteps total number of steps of each channel, index 0 = CH1, over all
 *                     patterns. 0 for a channel with no content. This field is not in the
 *                     brief: without it, the steps with no note at the end of a clip are lost
 * @param assignments  the notes that go into the patterns
 * @param dropped      the notes that do not
 * @param warnings     one warning for each dropped, moved or shortened note
 */
public record ConversionPlan(ConversionOptions options, List<Integer> channelSteps,
                             List<Assignment> assignments,
                             List<NoteEvent> dropped, List<ConversionWarning> warnings) {
    public ConversionPlan {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        channelSteps = List.copyOf(channelSteps);
        if (channelSteps.size() != 4) {
            throw new IllegalArgumentException("channelSteps must have 4 values: " + channelSteps.size());
        }
        for (int steps : channelSteps) {
            if (steps < 0) {
                throw new IllegalArgumentException("channelSteps must be 0 or more: " + steps);
            }
        }
        assignments = List.copyOf(assignments);
        dropped = List.copyOf(dropped);
        warnings = List.copyOf(warnings);
    }

    /** A copy in which the assignment at {@code index} is {@code changed}. */
    public ConversionPlan withAssignment(int index, Assignment changed) {
        List<Assignment> list = new ArrayList<>(assignments);
        list.set(index, changed);
        return new ConversionPlan(options, channelSteps, list, dropped, warnings);
    }

    /** A copy in which the note of the assignment at {@code index} is dropped. */
    public ConversionPlan withDropped(int index) {
        List<Assignment> list = new ArrayList<>(assignments);
        List<NoteEvent> gone = new ArrayList<>(dropped);
        gone.add(list.remove(index).note());
        return new ConversionPlan(options, channelSteps, list, gone, warnings);
    }
}
