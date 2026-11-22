# blackseq-lib

Black Sequencer pattern library. The format notes are in `docs/blackseq/FORMAT.md`.
Read that file before any change to `blackseq.format`.

A wrong byte can corrupt a pattern on the module. Accuracy is more important than speed.

## Packages

| Package | Purpose | Depends on |
| --- | --- | --- |
| `blackseq.model` | Immutable pattern data | nothing |
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

## Card safety

- Write only inside a directory that the user gives on the command line.
- The default write mode is `FAIL_IF_EXISTS`.
- `BACKUP_THEN_REPLACE` copies the old file to a `backup/` folder first.
- Never delete files.

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
