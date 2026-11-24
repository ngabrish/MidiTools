# midi-tools

Java tools for MIDI and hardware sequencers.

## blackseq

A library and a command-line tool for the pattern files (`.SEQ`) of the Erica Synths
Black Sequencer. This is not an Erica Synths product.

**Status: phase 3 (MIDI conversion).** The library reads and writes the note, the gate,
merged steps and the track length of a pattern file, and converts a MIDI file to
patterns. Three generated test files loaded and played correctly on the module. A
converted MIDI file has not been tested on the module at this time. The format notes are in [docs/blackseq/FORMAT.md](docs/blackseq/FORMAT.md).

### Build

Java 17 or later and Maven are necessary.

```bash
mvn -B verify
```

### Use

Convert a MIDI file to pattern files in a project folder on the SD card:

```bash
java -jar blackseq.jar convert song.mid --project E:\PROJECTS\MYSONG --bank 1 --pattern 1
```

- The project folder must be one that the module made. Select the bank on the module one
  time before the first conversion into it.
- The first four clips of the MIDI file go to CH1 to CH4. A clip is one track and MIDI
  channel that has notes. Use `--map 2,1,0,3` to select clips; 0 means no clip.
- One step is a sixteenth note. A file longer than 64 steps goes into the next patterns.
- The command replaces no file. With `--replace`, the old pattern file goes to the
  `BACKUP` folder of the project first.
- `--dry-run` prints the result and writes nothing. With `--dry-run`, `--project` is not
  necessary.
- Each note that is dropped, moved or shortened gives one warning. The command prints
  each warning if there are 10 or fewer. With more, it prints one count for each kind;
  use `--verbose` to print each warning.
- Other options: `--steps-per-beat`, `--transpose`, `--chords`, `--overflow`. Run the
  command with `--help` for the list.

MIDI note 36 is the note that the module shows as `C-3`.

Format tools:

```bash
java -jar blackseq.jar dump <file> [--offset <n>] [--length <n>]
```

```bash
java -jar blackseq.jar diff <a> <b>
```

`dump` shows a hex view with offsets. `diff` shows the byte ranges that are different
between two files.

After a build, the file is `blackseq-cli/target/blackseq.jar`. Each CI run on GitHub also
has it as the artifact `blackseq-jar`.

### Use the library in a different project

[JitPack](https://jitpack.io) builds the library from a release tag. Add the repository
and the dependency to the `pom.xml` of the project; `core` comes with `blackseq-lib`.

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.ngabrish.MidiTools</groupId>
  <artifactId>blackseq-lib</artifactId>
  <version>blackseq-lib-v1.0</version>
</dependency>
```

## Credits

The format notes use [black_seq_gui](https://github.com/ph1r3574r73r/black_seq_gui) by
ph1r3574r73r as a reference. That project is in the public domain (Unlicense).
