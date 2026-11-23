package blackseq.convert;

/** What the converter does when two or more notes of one clip go to the same step. */
public enum ChordPolicy {
    /** Keep the highest note. */
    HIGHEST,
    /** Keep the lowest note. */
    LOWEST,
    /** Keep the note that starts first. Notes with the same start: the first in the clip. */
    FIRST,
    /** Keep the highest note. The other notes go to channels that have no clip, if a step is free. */
    SPREAD
}
