# midi-tools

Java tools for MIDI and hardware sequencers.

## blackseq

A library and a command-line tool for the pattern files (`.SEQ`) of the Erica Synths
Black Sequencer. This is not an Erica Synths product.

**Status: phase 0 (format discovery).** The tool can only inspect files. It cannot write
a pattern file. The format notes are in [docs/blackseq/FORMAT.md](docs/blackseq/FORMAT.md).

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

`dump` shows a hex view with offsets. `diff` shows the byte ranges that are different
between two files. The exit status of `diff` is 0 when the files are equal and 1 when
they are different.

## Credits

The format notes use [black_seq_gui](https://github.com/ph1r3574r73r/black_seq_gui) by
ph1r3574r73r as a reference. That project is in the public domain (Unlicense).
