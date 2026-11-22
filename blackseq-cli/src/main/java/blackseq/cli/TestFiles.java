package blackseq.cli;

import blackseq.format.SeqFile;
import blackseq.model.Pattern;
import blackseq.model.Step;
import blackseq.model.Track;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The three pattern files of the phase 1 checkpoint. Nate loads them on the module to see
 * if the module accepts files that this library made.
 */
final class TestFiles {

    /** MIDI note that the module shows as C-3, the note of a blank step. */
    private static final int C3 = 36;
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};

    /** One line of text for each file name, in the sequence of the files. */
    static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        DESCRIPTIONS.put("00.SEQ", "One note: CH1 step 1 is C-4. All other steps are blank (C-3).");
        DESCRIPTIONS.put("01.SEQ", "16-step scale: CH1 steps 1 to 16 are C major from C-3 up to D-5.");
        DESCRIPTIONS.put("02.SEQ", "64-step track: CH1 has 64 steps. Steps 1 to 32 go up by one "
                + "semitone from C-3 to G-5. Steps 33 to 64 go down from G#-5 to C#-3.");
    }

    private TestFiles() {}

    /** File name to file content. */
    static Map<String, SeqFile> phase1() {
        Map<String, SeqFile> files = new LinkedHashMap<>();
        files.put("00.SEQ", ch1(16, new int[] {C3 + 12}));

        int[] scale = new int[16];
        for (int i = 0; i < scale.length; i++) {
            scale[i] = C3 + 12 * (i / 7) + MAJOR_SCALE[i % 7];
        }
        files.put("01.SEQ", ch1(16, scale));

        int[] long64 = new int[64];
        for (int i = 0; i < 32; i++) {
            long64[i] = C3 + i;             // 36 to 67
            long64[32 + i] = C3 + 32 - i;   // 68 to 37
        }
        files.put("02.SEQ", ch1(64, long64));
        return files;
    }

    /** A blank pattern with a CH1 length and the given notes on the first steps of CH1. */
    private static SeqFile ch1(int length, int[] notes) {
        SeqFile blank = SeqFile.blank();
        Pattern p = blank.pattern();
        List<Step> steps = new ArrayList<>(p.tracks().get(0).steps());
        for (int i = 0; i < notes.length; i++) {
            Step s = steps.get(i);
            steps.set(i, new Step(s.gateOn(), notes[i], s.gateLength()));
        }
        List<Track> tracks = new ArrayList<>(p.tracks());
        tracks.set(0, new Track(length, steps));
        return blank.withPattern(new Pattern(tracks));
    }
}
