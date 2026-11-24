# blackseq-lib

Black Sequencer pattern library. The format notes are in `docs/blackseq/FORMAT.md`.
Read that file before any change to `blackseq.format`.

A wrong byte can corrupt a pattern on the module. Accuracy is more important than speed.

## Packages

| Package | Purpose | Depends on |
| --- | --- | --- |
| `blackseq.model` | Immutable pattern data; note names and track text (`PatternText`) | nothing |
| `blackseq.format` | Read and write `.SEQ` bytes; hex dump and byte diff | `model` |
| `blackseq.convert` | Notes to steps (`plan()` then `apply()`) | `model`, `core` |
| `blackseq.card` | Project folder layout and safe writes | `format` |

## Format rules

- **Evidence for each format claim.** Each field in `FORMAT.md` has an offset, a size, an
  encoding, the evidence and a status: `confirmed by samples`, `reference code only` or
  `unknown`.
- **No guesses.** Write only fields with the status `confirmed by samples`. Confirm the
  rule of a checksum or a length field with a minimum of three samples before any write.
- **Write from a template.** `SeqFile` keeps the full original bytes. A write starts from
  a file that the module made and changes only the bytes of known fields.
- **Keep unknown bytes.** Copy them from the template without change.
- **Missing sample.** If a sample is missing, stop and ask Nate for it.
- **Layer discipline.** `model` and `convert` do not import `format`. Byte offsets do not
  appear outside `format`.
- **Small model first.** `Step` holds only gate, note and gate length. Add a field only
  after the samples confirm its bytes.
- **Write only on change.** `SeqFile.withPattern` writes a field only when the new value
  is different from the decoded value. Keep `file.withPattern(file.pattern())` equal to
  `file`; a test checks this for each sample.
- **One place for offsets.** `SeqLayout` holds each byte position. Add a position there
  only when `FORMAT.md` gives it the status `confirmed by samples`.
- **Bundled blank pattern.** `src/main/resources/blackseq/format/blank.SEQ` is a copy of
  the sample `RETEST/00/00.SEQ`. A test checks that the two are equal.
- **Signatures.** The public API in the brief is the intended shape. Change a signature
  only when the format work makes it necessary, and record the reason in `FORMAT.md`.
- **No user-interface code** in this module.

## Users of this library

- `blackseq-cli` in this repository.
- The Synthnomicon web application (`ngabrish/synthnomicon`) gets `blackseq-lib` and `core`
  from JitPack, built from a tag `blackseq-lib-v<version>`. `jitpack.yml` in the repository
  root sets the Java version of that build. Synthnomicon holds no format code and no
  conversion logic, so each thing that it needs must be public here.
- A change to a public signature needs a new tag, and then a version change in Synthnomicon.

## Converter rules

- `plan()` decides, `apply()` builds. Put no placement logic in `apply()` and none in a
  user interface.
- Each dropped, moved or shortened note gets one warning. A test checks this for each case.
- A note that is longer than one step holds merged steps. It ends at the next note of its
  channel and at the end of its pattern. The last step of a note is never merged with
  the next step.
- A note that fills its step gets gate length 0.99, not 1.0. A full gate joins the step
  to the next one, and then a repeated note does not start again.
- A step with no note is a muted step with the note of the step before it.

## Differences from the brief

Record each change to a signature of the brief here, with the reason.

| Change | Reason |
| --- | --- |
| `SeqFile.fromBytes`, `masterTrackLength`, `withMasterTrackLength` added | The master track length is one value for the pattern, outside the four tracks |
| `ConversionPlan` has the field `channelSteps` | `apply()` needs the length of each clip. Without it, the steps with no note at the end of a clip are lost |
| `Step` has the field `mergeNext` | A note that is longer than one step needs merged steps. Samples P10 and P15 confirm the bit |
| `Assignment` has the field `steps` | The number of steps that a note holds. `gateLength` is the gate of the last step |
| `ConversionException` added | `OverflowPolicy.FAIL` and a plan conflict need an exception type |
| `ConversionOptions.defaults()` added | One place for the default values of the CLI and a later user interface |
| `SeqFile.fromPattern` added | The CLI and the Synthnomicon web API must build a file with the same code |
| `PatternText` added (`noteName`, `trackLine`) | Moved from the CLI. Each program must show the note names that the module shows |
| `ConversionWarning` has the field `kind` (`WarningKind`) | A user interface filters and counts warnings by kind. The CLI read the kind from the message text before |
| `ConversionWarning` has the fields `sourceClip` and `note` | A user interface shows a warning at the note that it is about. The channel and the beat do not identify one note of a chord |
| `NoteEdits` added (`moved`, `resized`, `repitched`, `created`) | The note editor of Synthnomicon names a pattern, a step and a pitch. The reverse of the converter's decisions must be here, not in a user interface |
| `ProjectFolder.patternName`, `BANKS`, `PATTERNS_PER_BANK` added | A program that makes a zip file needs the name of a pattern file and the limits, and must not hold them itself |

## Card safety

- Write only inside a directory that the user gives on the command line.
- The default write mode is `FAIL_IF_EXISTS`.
- `BACKUP_THEN_REPLACE` copies the old file to a `backup/` folder first.
- Never delete files.
- `ProjectFolder` opens only a folder with `INFO.DAT`, and writes only into a bank folder
  with `BANK.DAT`. The module makes these files. The library never makes them, because
  their content is not known.
- A write is read back and compared.
- Backups go to `<project>/BACKUP/` with names of 8 characters, for example
  `0003_001.SEQ` (bank folder 00, pattern file 03, copy 1). A backup is never replaced.

## Reference code

[black_seq_gui](https://github.com/ph1r3574r73r/black_seq_gui) (Unlicense) is a hint, not
proof. Format logic can be ported and must be credited in the README. Do not copy its
audio preview code (MIT, Mutable Instruments).

## Phases

0. Format discovery: `dump`, `diff`, `FORMAT.md`. Checkpoint: Nate reads `FORMAT.md`.
1. Codec: `model`, `format`. Checkpoint: Nate loads three generated files on the module.
2. Converter: `convert`, unit tests only.
3. MIDI and card: `core` `midi`, `card`, `blackseq convert`. Checkpoint: Nate tests a real
   MIDI file on the module.
