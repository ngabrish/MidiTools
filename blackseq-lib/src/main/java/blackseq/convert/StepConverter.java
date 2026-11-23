package blackseq.convert;

import blackseq.model.Pattern;
import blackseq.model.Step;
import blackseq.model.Track;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import miditools.core.note.NoteClip;
import miditools.core.note.NoteEvent;

/**
 * Converts notes to steps in two stages. {@link #plan} decides where each note goes, and
 * {@link #apply} builds the patterns. A user interface can show and change the plan
 * between the two stages.
 *
 * <p>Each track is monophonic: one step holds one note. A note that is longer than one
 * step is shortened to one step, because this version does not write merged steps.
 */
public final class StepConverter {

    private static final int CHANNELS = Pattern.CHANNELS;
    private static final int PATTERN_STEPS = Track.MAX_STEPS;
    /** Track length of a channel that has no content in a pattern. */
    private static final int DEFAULT_LENGTH = 16;
    /** Note of a step with no gate, when the track has no earlier note: C-3, the blank note. */
    private static final int REST_NOTE = 36;
    private static final double REST_GATE = 0.5;
    /**
     * Gate length for a note that fills its step. It is less than 1.0 because a full gate
     * joins the step to the next one, and then a repeated note does not start again.
     */
    static final double LONGEST_GATE = 0.99;
    private static final double SHORTEST_GATE = 0.01;
    /** Position difference, in steps, below which a note is on the grid. */
    private static final double GRID_TOLERANCE = 1e-6;

    /** A note with its step position in the clip. */
    private record Placed(int clip, NoteEvent note, int index, double lengthSteps) {}

    /**
     * Decides where each note goes. {@code clips.get(0)} goes to CH1 ... {@code clips.get(3)}
     * to CH4.
     *
     * @param clips 1 to 4 clips
     * @throws ConversionException if a clip is longer than 64 steps and the policy is FAIL
     */
    public ConversionPlan plan(List<NoteClip> clips, ConversionOptions options) {
        if (clips.isEmpty() || clips.size() > CHANNELS) {
            throw new IllegalArgumentException("plan needs 1 to 4 clips: " + clips.size());
        }
        List<Assignment> assignments = new ArrayList<>();
        List<NoteEvent> dropped = new ArrayList<>();
        List<ConversionWarning> warnings = new ArrayList<>();
        int[] channelSteps = new int[CHANNELS];
        // Steps in use on the channels that have no clip, for ChordPolicy.SPREAD.
        List<Set<Integer>> spreadUse = new ArrayList<>();
        for (int c = 0; c < CHANNELS; c++) {
            spreadUse.add(new HashSet<>());
        }

        for (int clipIndex = 0; clipIndex < clips.size(); clipIndex++) {
            NoteClip clip = clips.get(clipIndex);
            int channel = clipIndex + 1;
            int spb = options.stepsPerBeat();

            int total = (int) Math.ceil(clip.lengthBeats() * spb - GRID_TOLERANCE);
            for (NoteEvent n : clip.notes()) {
                total = Math.max(total, (int) Math.floor(n.startBeats() * spb + GRID_TOLERANCE) + 1);
            }
            if (total > PATTERN_STEPS && options.overflow() == OverflowPolicy.FAIL) {
                throw new ConversionException("Clip " + channel + " (" + clip.name() + ") has " + total
                        + " steps. One pattern holds " + PATTERN_STEPS + " steps.");
            }
            int kept = options.overflow() == OverflowPolicy.TRUNCATE ? Math.min(total, PATTERN_STEPS) : total;
            channelSteps[clipIndex] = kept;

            // Step index -> notes on that step, in clip order.
            Map<Integer, List<Placed>> byStep = new TreeMap<>();
            for (NoteEvent n : clip.notes()) {
                int pitch = n.midiNote() + options.transpose();
                if (pitch < 0 || pitch > 127) {
                    dropped.add(n);
                    warnings.add(warn(channel, n, "dropped: with transpose " + options.transpose()
                            + " the note number is " + pitch + ", outside 0 to 127"));
                    continue;
                }
                double position = n.startBeats() * spb;
                int index = Math.min((int) Math.floor(position + 0.5), total - 1);
                if (index >= kept) {
                    dropped.add(n);
                    warnings.add(warn(channel, n, "dropped: the note is after step " + PATTERN_STEPS
                            + " and the overflow policy is TRUNCATE"));
                    continue;
                }
                if (Math.abs(position - index) > GRID_TOLERANCE) {
                    warnings.add(warn(channel, n, String.format(Locale.ROOT,
                            "moved: the note starts at step position %.2f and goes to step %d",
                            position + 1, index % PATTERN_STEPS + 1)));
                }
                byStep.computeIfAbsent(index, k -> new ArrayList<>())
                        .add(new Placed(clipIndex, n, index, n.lengthBeats() * spb));
            }

            for (List<Placed> onStep : byStep.values()) {
                Placed winner = pick(onStep, options.chords());
                assignments.add(assign(winner, channel, kept, warnings));
                List<Placed> others = new ArrayList<>(onStep);
                others.remove(winner);
                others.sort(Comparator.comparingInt((Placed p) -> p.note().midiNote()).reversed());
                for (Placed other : others) {
                    int free = options.chords() == ChordPolicy.SPREAD
                            ? freeChannel(clips.size(), spreadUse, other.index()) : 0;
                    if (free == 0) {
                        dropped.add(other.note());
                        warnings.add(warn(channel, other.note(), "dropped: step "
                                + (other.index() % PATTERN_STEPS + 1) + " has a different note"
                                + (options.chords() == ChordPolicy.SPREAD ? " and no channel is free"
                                : " (chord policy " + options.chords() + ")")));
                    } else {
                        spreadUse.get(free - 1).add(other.index());
                        channelSteps[free - 1] = Math.max(channelSteps[free - 1], kept);
                        warnings.add(warn(channel, other.note(), "moved: chord note goes to CH" + free));
                        assignments.add(assign(other, free, kept, warnings));
                    }
                }
            }
        }

        assignments.sort(Comparator.comparingInt(Assignment::channel)
                .thenComparingInt(Assignment::pattern).thenComparingInt(Assignment::step));
        List<Integer> steps = new ArrayList<>();
        for (int s : channelSteps) {
            steps.add(s);
        }
        return new ConversionPlan(options, steps, assignments, dropped, warnings);
    }

    /**
     * Finds problems in a plan, for example after a program changed it. Two notes on one
     * step are a conflict: {@link #apply} refuses such a plan.
     */
    public List<ConversionWarning> validate(ConversionPlan plan) {
        List<ConversionWarning> found = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (Assignment a : plan.assignments()) {
            long key = a.channel() * 1_000_000L + (long) a.pattern() * PATTERN_STEPS + a.step();
            if (!used.add(key)) {
                found.add(new ConversionWarning(a.channel(), a.note().startBeats(),
                        "conflict: CH" + a.channel() + " pattern " + a.pattern() + " step " + a.step()
                                + " has two notes"));
            }
            int pitch = a.note().midiNote() + plan.options().transpose();
            if (pitch < 0 || pitch > 127) {
                found.add(new ConversionWarning(a.channel(), a.note().startBeats(),
                        "conflict: with transpose " + plan.options().transpose()
                                + " the note number is " + pitch + ", outside 0 to 127"));
            }
        }
        return found;
    }

    /**
     * Builds the patterns of a plan. Each step with no note is a muted step that keeps the
     * note of the step before it.
     *
     * @throws ConversionException if {@link #validate} finds a conflict
     */
    public List<Pattern> apply(ConversionPlan plan) {
        List<ConversionWarning> conflicts = validate(plan);
        if (!conflicts.isEmpty()) {
            throw new ConversionException("The plan has " + conflicts.size() + " conflict(s). First: "
                    + conflicts.get(0).message());
        }
        int patternCount = 1;
        for (int steps : plan.channelSteps()) {
            patternCount = Math.max(patternCount, (steps + PATTERN_STEPS - 1) / PATTERN_STEPS);
        }
        for (Assignment a : plan.assignments()) {
            patternCount = Math.max(patternCount, a.pattern() + 1);
        }

        // tracks[pattern][channel]
        List<List<Track>> tracks = new ArrayList<>();
        for (int p = 0; p < patternCount; p++) {
            tracks.add(new ArrayList<>());
        }
        for (int channel = 1; channel <= CHANNELS; channel++) {
            int carried = REST_NOTE;
            boolean first = true;
            for (Assignment a : plan.assignments()) {
                if (a.channel() == channel && first) {
                    carried = a.note().midiNote() + plan.options().transpose();
                    first = false;
                }
            }
            for (int p = 0; p < patternCount; p++) {
                int remaining = plan.channelSteps().get(channel - 1) - p * PATTERN_STEPS;
                int length = remaining <= 0 ? DEFAULT_LENGTH
                        : Math.max(Track.MIN_LENGTH, Math.min(PATTERN_STEPS, remaining));
                Step[] notes = new Step[PATTERN_STEPS];
                for (Assignment a : plan.assignments()) {
                    if (a.channel() == channel && a.pattern() == p) {
                        notes[a.step() - 1] = new Step(true,
                                a.note().midiNote() + plan.options().transpose(), a.gateLength());
                        length = Math.max(length, a.step());
                    }
                }
                List<Step> steps = new ArrayList<>();
                for (int s = 0; s < length; s++) {
                    if (notes[s] != null) {
                        carried = notes[s].midiNote();
                        steps.add(notes[s]);
                    } else {
                        steps.add(new Step(false, carried, REST_GATE));
                    }
                }
                tracks.get(p).add(new Track(length, steps));
            }
        }
        List<Pattern> patterns = new ArrayList<>();
        for (List<Track> t : tracks) {
            patterns.add(new Pattern(t));
        }
        return patterns;
    }

    private static Placed pick(List<Placed> onStep, ChordPolicy policy) {
        Placed best = onStep.get(0);
        for (Placed p : onStep) {
            boolean better = switch (policy) {
                case HIGHEST, SPREAD -> p.note().midiNote() > best.note().midiNote();
                case LOWEST -> p.note().midiNote() < best.note().midiNote();
                case FIRST -> p.note().startBeats() < best.note().startBeats();
            };
            if (better) {
                best = p;
            }
        }
        return best;
    }

    /** The first channel with no clip whose step is free, or 0. */
    private static int freeChannel(int clipCount, List<Set<Integer>> spreadUse, int index) {
        for (int channel = clipCount + 1; channel <= CHANNELS; channel++) {
            if (!spreadUse.get(channel - 1).contains(index)) {
                return channel;
            }
        }
        return 0;
    }

    private static Assignment assign(Placed p, int channel, int clipSteps, List<ConversionWarning> warnings) {
        int sourceChannel = p.clip() + 1;
        int stepInPattern = p.index() % PATTERN_STEPS;
        double length = p.lengthSteps();
        double gate;
        if (length > 1.0 + GRID_TOLERANCE) {
            gate = LONGEST_GATE;
            int patternEnd = Math.min(PATTERN_STEPS, clipSteps - (p.index() - stepInPattern));
            String reason = stepInPattern + length > patternEnd + GRID_TOLERANCE
                    ? "it crosses the end of the pattern at step " + patternEnd
                    : "this version does not write merged steps";
            warnings.add(warn(sourceChannel, p.note(), String.format(Locale.ROOT,
                    "shortened: the note is %.2f steps long and gets one step, because %s", length, reason)));
        } else {
            gate = Math.max(SHORTEST_GATE, Math.min(LONGEST_GATE, length));
        }
        return new Assignment(p.clip(), p.note(), channel, p.index() / PATTERN_STEPS, stepInPattern + 1, gate);
    }

    private static ConversionWarning warn(int channel, NoteEvent n, String text) {
        return new ConversionWarning(channel, n.startBeats(), "Note " + n.midiNote() + " " + text + ".");
    }
}
