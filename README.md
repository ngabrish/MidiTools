# midi-tools

Java tools for MIDI and hardware sequencers.

## blackseq

A library and a command-line tool for the pattern files (`.SEQ`) of the Erica Synths
Black Sequencer. This is not an Erica Synths product.

**Status: phase 1 (codec).** The library reads and writes the note, the gate and the
track length of a pattern file. The module has not confirmed the written files at this
time. There is no MIDI conversion. The format notes are in [docs/blackseq/FORMAT.md](docs/blackseq/FORMAT.md).

### Build

Java 17 or later and Maven are necessary.

```bash
mvn -B verify
```

### Use

```bash
java -jar blackseq-cli/target/blackseq.jar dump <file> [--offset <n>] [--length <n>]
```

```bash
java -jar blackseq-cli/target/blackseq.jar diff <a> <b>
```

```bash
java -jar blackseq-cli/target/blackseq.jar testfiles <dir>
```

`dump` shows a hex view with offsets. `diff` shows the byte ranges that are different
between two files. The exit status of `diff` is 0 when the files are equal and 1 when
they are different. `testfiles` writes three test patterns into a directory and replaces
no file.

## Credits

The format notes use [black_seq_gui](https://github.com/ph1r3574r73r/black_seq_gui) by
ph1r3574r73r as a reference. That project is in the public domain (Unlicense).
