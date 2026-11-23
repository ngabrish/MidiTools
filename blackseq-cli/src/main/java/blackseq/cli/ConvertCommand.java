package blackseq.cli;

import blackseq.card.ProjectFolder;
import blackseq.card.WriteMode;
import blackseq.convert.ChordPolicy;
import blackseq.convert.ConversionException;
import blackseq.convert.ConversionOptions;
import blackseq.convert.ConversionPlan;
import blackseq.convert.ConversionWarning;
import blackseq.convert.OverflowPolicy;
import blackseq.convert.StepConverter;
import blackseq.format.SeqFile;
import blackseq.model.Pattern;
import blackseq.model.Step;
import blackseq.model.Track;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import miditools.core.midi.MidiFileReader;
import miditools.core.note.NoteClip;

/**
 * {@code blackseq convert}: a MIDI file becomes pattern files in a project folder.
 * This class reads arguments, calls the library and prints the result. It has no
 * conversion logic.
 */
final class ConvertCommand {

    static final String USAGE = """
              blackseq convert <file.mid> --project <dir> --bank <n> --pattern <n> [options]
                  Convert a MIDI file to pattern files. <dir> is a project folder that the
                  module made, for example E:\\PROJECTS\\MYSONG. Bank and pattern are 1 to 16.
                  A file longer than 64 steps goes into the next patterns.
                  --map <a,b,c,d>       MIDI clip numbers for CH1 to CH4, 0 for no clip.
                                        Default: the first four clips of the file.
                  --dry-run             Print the result and write nothing.
                  --verbose             Print each warning. Without this option, more than
                                        10 warnings are printed as one count for each kind.
                  --replace             Replace a pattern file that exists. The old file
                                        goes to the BACKUP folder of the project first.
                  --steps-per-beat <n>  Default 4: one step is a sixteenth note.
                  --transpose <n>       Semitones to add to each note. Default 0.
                  --chords <policy>     HIGHEST (default), LOWEST, FIRST or SPREAD.
                  --overflow <policy>   SPLIT (default), TRUNCATE or FAIL.
            """;

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final int LAST_PATTERN = 16;
    /** With more warnings than this, the command prints counts unless --verbose is given. */
    static final int MAX_WARNING_LINES = 10;
    /** The kinds of warning, as the converter writes them at the start of a message. */
    private static final String[] WARNING_KINDS = {"dropped", "moved", "shortened"};

    private ConvertCommand() {}

    static int run(String[] args, PrintStream out, PrintStream err) throws IOException {
        Path midi = null;
        Path projectDir = null;
        Integer bank = null;
        Integer pattern = null;
        int[] map = null;
        boolean dryRun = false;
        boolean verbose = false;
        boolean replace = false;
        int stepsPerBeat = 4;
        int transpose = 0;
        ChordPolicy chords = ChordPolicy.HIGHEST;
        OverflowPolicy overflow = OverflowPolicy.SPLIT;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--project" -> projectDir = Main.path(value(args, ++i, "--project"));
                case "--bank" -> bank = Main.number(args, ++i, "--bank");
                case "--pattern" -> pattern = Main.number(args, ++i, "--pattern");
                case "--map" -> map = map(value(args, ++i, "--map"));
                case "--dry-run" -> dryRun = true;
                case "--verbose" -> verbose = true;
                case "--replace" -> replace = true;
                case "--steps-per-beat" -> stepsPerBeat = Main.number(args, ++i, "--steps-per-beat");
                case "--transpose" -> transpose = signed(value(args, ++i, "--transpose"), "--transpose");
                case "--chords" -> chords = policy(ChordPolicy.class, value(args, ++i, "--chords"), "--chords");
                case "--overflow" -> overflow = policy(OverflowPolicy.class, value(args, ++i, "--overflow"), "--overflow");
                default -> {
                    if (args[i].startsWith("--")) {
                        throw new Main.UsageException("Unknown option: " + args[i]);
                    }
                    if (midi != null) {
                        throw new Main.UsageException("convert takes one MIDI file");
                    }
                    midi = Main.path(args[i]);
                }
            }
        }
        if (midi == null) {
            throw new Main.UsageException("convert: the MIDI file is missing");
        }
        if (stepsPerBeat < 1) {
            throw new Main.UsageException("--steps-per-beat must be 1 or more");
        }
        if (!dryRun && (projectDir == null || bank == null || pattern == null)) {
            throw new Main.UsageException("convert needs --project, --bank and --pattern (or --dry-run)");
        }
        range("--bank", bank);
        range("--pattern", pattern);

        // 1. Read the MIDI file and select the clips.
        List<NoteClip> all = MidiFileReader.read(midi);
        if (all.isEmpty()) {
            err.println("The MIDI file has no notes: " + midi);
            return Main.EXIT_ERROR;
        }
        out.println("MIDI file: " + midi);
        for (int i = 0; i < all.size(); i++) {
            NoteClip c = all.get(i);
            out.printf(Locale.ROOT, "  clip %d: %s, %d notes, %s beats%n", i + 1, c.name(), c.notes().size(),
                    trim(c.lengthBeats()));
        }
        List<NoteClip> selected = new ArrayList<>();
        if (map == null) {
            selected.addAll(all.subList(0, Math.min(4, all.size())));
            if (all.size() > 4) {
                out.println("Warning: the file has " + all.size() + " clips. Clips 5 and later are not used."
                        + " Use --map to select clips.");
            }
        } else {
            for (int number : map) {
                if (number > all.size()) {
                    throw new Main.UsageException("--map: the file has no clip " + number);
                }
                selected.add(number == 0 ? new NoteClip("(no clip)", 0, List.of()) : all.get(number - 1));
            }
        }
        for (int i = 0; i < selected.size(); i++) {
            out.println("  CH" + (i + 1) + " <- " + selected.get(i).name());
        }

        // 2. Convert.
        StepConverter converter = new StepConverter();
        ConversionPlan plan;
        try {
            plan = converter.plan(selected, new ConversionOptions(stepsPerBeat, chords, overflow, transpose));
        } catch (ConversionException e) {
            err.println(e.getMessage());
            return Main.EXIT_ERROR;
        }
        List<Pattern> patterns = converter.apply(plan);

        out.println();
        out.println("Result: " + plan.assignments().size() + " notes in " + patterns.size()
                + (patterns.size() == 1 ? " pattern, " : " patterns, ") + plan.dropped().size() + " dropped, "
                + plan.warnings().size() + (plan.warnings().size() == 1 ? " warning" : " warnings"));
        if (verbose || plan.warnings().size() <= MAX_WARNING_LINES) {
            for (ConversionWarning w : plan.warnings()) {
                out.printf(Locale.ROOT, "  CH%d beat %s: %s%n", w.channel(), trim(w.atBeats() + 1), w.message());
            }
        } else {
            printWarningCounts(plan.warnings(), out);
        }

        // 3. Build the files. The master track length is the longest track of the pattern.
        List<SeqFile> files = new ArrayList<>();
        for (Pattern p : patterns) {
            int longest = p.tracks().stream().mapToInt(Track::length).max().orElse(16);
            files.add(SeqFile.blank().withPattern(p).withMasterTrackLength(longest));
        }
        int first = pattern == null ? 1 : pattern;
        if (first + files.size() - 1 > LAST_PATTERN) {
            err.println("The result has " + files.size() + " patterns. From pattern " + first
                    + ", it does not fit in the bank, which has " + LAST_PATTERN + " patterns.");
            return Main.EXIT_ERROR;
        }
        for (int i = 0; i < patterns.size(); i++) {
            out.println();
            out.println("Pattern " + (first + i) + ":");
            for (int c = 0; c < Pattern.CHANNELS; c++) {
                out.println("  " + line(c + 1, patterns.get(i).tracks().get(c)));
            }
        }
        out.println();

        if (projectDir == null) {
            out.println("Dry run: no file was written.");
            return Main.EXIT_OK;
        }

        // 4. Check all targets before the first write, so that the command writes all files or none.
        ProjectFolder project = ProjectFolder.open(projectDir);
        List<Path> targets = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            Path target = project.patternPath(bank, first + i);
            targets.add(target);
            if (Files.exists(target) && !replace && !dryRun) {
                throw new FileAlreadyExistsException(target.toString(), null,
                        "use --replace to replace it; the old file goes to the BACKUP folder first");
            }
        }
        if (dryRun) {
            for (Path target : targets) {
                out.println("Dry run: would write " + target + (Files.exists(target) ? " (the file exists)" : ""));
            }
            out.println("Dry run: no file was written.");
            return Main.EXIT_OK;
        }
        WriteMode mode = replace ? WriteMode.BACKUP_THEN_REPLACE : WriteMode.FAIL_IF_EXISTS;
        for (int i = 0; i < files.size(); i++) {
            project.writePattern(bank, first + i, files.get(i), mode);
            out.println("Wrote " + targets.get(i));
        }
        return Main.EXIT_OK;
    }

    /** One line for each kind of warning and for each channel that has warnings of that kind. */
    private static void printWarningCounts(List<ConversionWarning> warnings, PrintStream out) {
        int counted = 0;
        for (String kind : WARNING_KINDS) {
            int[] perChannel = new int[Pattern.CHANNELS + 1];
            int total = 0;
            for (ConversionWarning w : warnings) {
                if (w.message().contains(" " + kind + ": ")) {
                    perChannel[w.channel()]++;
                    total++;
                }
            }
            if (total > 0) {
                StringBuilder channels = new StringBuilder();
                for (int c = 1; c <= Pattern.CHANNELS; c++) {
                    if (perChannel[c] > 0) {
                        channels.append(channels.length() == 0 ? "" : ", ").append("CH").append(c).append(": ")
                                .append(perChannel[c]);
                    }
                }
                out.printf(Locale.ROOT, "  %4d %s (%s)%n", total, kind, channels);
                counted += total;
            }
        }
        if (counted < warnings.size()) {
            out.printf(Locale.ROOT, "  %4d other%n", warnings.size() - counted);
        }
        out.println("  Use --verbose to print each warning.");
    }

    /** One line for a track: the steps that play, with the note names that the module shows. */
    static String line(int channel, Track track) {
        StringBuilder sb = new StringBuilder("CH" + channel + " (" + track.length() + " steps):");
        boolean any = false;
        for (int s = 0; s < track.length(); s++) {
            Step step = track.steps().get(s);
            boolean held = s > 0 && track.steps().get(s - 1).mergeNext() && track.steps().get(s - 1).gateOn();
            if (s % 16 == 0 && track.length() > 16) {
                sb.append(System.lineSeparator()).append("     ");
            }
            sb.append(' ');
            if (!step.gateOn()) {
                sb.append('.');
            } else if (held) {
                sb.append('~');
                any = true;
            } else {
                sb.append(noteName(step.midiNote()));
                any = true;
            }
        }
        return any ? sb.toString() : "CH" + channel + " (" + track.length() + " steps): no notes";
    }

    /** The note name as the module shows it: MIDI note 36 is C-3. */
    static String noteName(int midiNote) {
        return NOTE_NAMES[midiNote % 12] + "-" + midiNote / 12;
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        return text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new Main.UsageException(option + ": the value is missing");
        }
        return args[index];
    }

    private static int signed(String text, String option) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new Main.UsageException(option + ": not a number: " + text);
        }
    }

    private static void range(String option, Integer value) {
        if (value != null && (value < 1 || value > 16)) {
            throw new Main.UsageException(option + " must be 1 to 16: " + value);
        }
    }

    private static int[] map(String text) {
        String[] parts = text.split(",", -1);
        if (parts.length < 1 || parts.length > 4) {
            throw new Main.UsageException("--map takes 1 to 4 clip numbers: " + text);
        }
        int[] map = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                map[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new Main.UsageException("--map: not a number: " + parts[i]);
            }
            if (map[i] < 0) {
                throw new Main.UsageException("--map: a clip number must be 0 or more: " + map[i]);
            }
        }
        return map;
    }

    private static <E extends Enum<E>> E policy(Class<E> type, String text, String option) {
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new Main.UsageException(option + ": unknown value: " + text);
        }
    }
}
