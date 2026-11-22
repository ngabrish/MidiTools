package blackseq.model;

import java.util.List;

/**
 * One channel of a pattern.
 *
 * @param length number of steps that play, 2 to 64. This is the last step of the track
 * @param steps  step data, from step 1. A minimum of {@code length} and a maximum of 64
 *               steps. A track that comes from a file has 64 steps, because the file
 *               keeps the steps after the last step
 */
public record Track(int length, List<Step> steps) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_STEPS = 64;

    public Track {
        if (length < MIN_LENGTH || length > MAX_STEPS) {
            throw new IllegalArgumentException("length must be 2 to 64: " + length);
        }
        steps = List.copyOf(steps);
        if (steps.size() < length || steps.size() > MAX_STEPS) {
            throw new IllegalArgumentException(
                    "steps must hold " + length + " to 64 steps: " + steps.size());
        }
    }
}
