package blackseq.convert;

import blackseq.model.Track;
import miditools.core.note.NoteEvent;

/**
 * Changes to source notes that a user makes in a step grid. A user interface names a
 * position in the grid (pattern, step) and a pitch as the module shows it; these functions
 * give the note that {@link StepConverter#plan} puts there. They are the reverse of the
 * decisions of the converter, so they are here and not in a user interface.
 *
 * <p>The functions change one note. The converter decides again for all notes after each
 * change: a moved note can make a chord, and then the chord policy drops a note.
 */
public final class NoteEdits {

    /** Velocity of a note that the user adds. A pattern does not hold a velocity. */
    public static final int NEW_NOTE_VELOCITY = 100;

    private NoteEdits() {}

    /**
     * The note at a different position, with the same length and pitch.
     *
     * @param pattern 0 for the first pattern of the result
     * @param step    first step of the note in that pattern, 1 to 64
     */
    public static NoteEvent moved(NoteEvent note, ConversionOptions options, int pattern, int step) {
        return new NoteEvent(startBeats(options, pattern, step), note.lengthBeats(), note.midiNote(),
                note.velocity());
    }

    /**
     * The note with a length of a full number of steps, at the same start.
     *
     * @param steps number of steps that the note holds, 1 or more
     */
    public static NoteEvent resized(NoteEvent note, ConversionOptions options, int steps) {
        return new NoteEvent(note.startBeats(), lengthBeats(options, steps), note.midiNote(), note.velocity());
    }

    /**
     * The note with a different pitch.
     *
     * @param shownMidiNote the MIDI note number that the pattern gets, 0 to 127. The source
     *                      note is this number without the transpose of the options
     */
    public static NoteEvent repitched(NoteEvent note, ConversionOptions options, int shownMidiNote) {
        return new NoteEvent(note.startBeats(), note.lengthBeats(), sourceNote(options, shownMidiNote),
                note.velocity());
    }

    /** A new note. The parameters are those of {@link #moved}, {@link #resized} and {@link #repitched}. */
    public static NoteEvent created(ConversionOptions options, int pattern, int step, int steps, int shownMidiNote) {
        return new NoteEvent(startBeats(options, pattern, step), lengthBeats(options, steps),
                sourceNote(options, shownMidiNote), NEW_NOTE_VELOCITY);
    }

    private static double startBeats(ConversionOptions options, int pattern, int step) {
        if (pattern < 0) {
            throw new IllegalArgumentException("pattern must be 0 or more: " + pattern);
        }
        if (step < 1 || step > Track.MAX_STEPS) {
            throw new IllegalArgumentException("step must be 1 to " + Track.MAX_STEPS + ": " + step);
        }
        long index = (long) pattern * Track.MAX_STEPS + (step - 1);
        return (double) index / options.stepsPerBeat();
    }

    private static double lengthBeats(ConversionOptions options, int steps) {
        if (steps < 1) {
            throw new IllegalArgumentException("steps must be 1 or more: " + steps);
        }
        return (double) steps / options.stepsPerBeat();
    }

    private static int sourceNote(ConversionOptions options, int shownMidiNote) {
        if (shownMidiNote < 0 || shownMidiNote > 127) {
            throw new IllegalArgumentException("midiNote must be 0 to 127: " + shownMidiNote);
        }
        int source = shownMidiNote - options.transpose();
        if (source < 0 || source > 127) {
            throw new IllegalArgumentException("With transpose " + options.transpose() + ", the pattern note "
                    + shownMidiNote + " needs the source note " + source + ", outside 0 to 127");
        }
        return source;
    }
}
