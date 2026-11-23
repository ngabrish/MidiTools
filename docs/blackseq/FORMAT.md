# Black Sequencer `.SEQ` format

Status of this document: **phase 1.** All fields that version 1 needs are confirmed by
samples. `blackseq.format.SeqFile` reads and writes them. Nate tested three generated
files on the module on 2026-11-23: all three loaded and played correctly (see "Module
tests").

## Rules for this document

Each field has an offset, a size, an encoding, the evidence and a status:

| Status | Meaning | Can the codec write it? |
| --- | --- | --- |
| `confirmed by samples` | A sample pair from Nate's module shows it. | Yes |
| `reference code only` | The reference tool says so. Other files can agree, but they are not proof. | No |
| `unknown` | No evidence for the meaning. | No. Copy from the template. |

`SampleEvidenceTest` in `blackseq-lib` checks each `confirmed by samples` claim against
the sample files on each build.

## Sources

- **Samples**: `samples/blackseq/PROJECTS/RETEST/00/00.SEQ` to `14.SEQ`. `14.SEQ` (P15) is
  from 2026-11-23: three merged steps. The intended steps were 1 to 3; the file has steps
  49 to 51, which gives the same evidence. `00.SEQ` to `13.SEQ` were saved by Nate on
  2026-11-22. Nate saved P7, P9 and P12 a second time, because the first files did not
  show the intended change; the files in the repository are the second ones. Module firmware 1.06, bootloader 0.2. `NN.SEQ` is pattern NN + 1 of the procedure in `samples/blackseq/README.md`.
  In this document, "P1" to "P14" are those patterns.
- **Factory project**: `samples/blackseq/PROJECTS/FACTORY/00/00.SEQ` to `06.SEQ`, from the
  same card. The module or Erica Synths made these files, but no manifest says what they
  contain. They are supporting evidence, not proof.
- **User manual**: Erica Synths "Black Sequencer user manual 3-2023 v2". It describes the
  functions of the module, not the file. It is not in this repository.
- **Reference code**: [black_seq_gui](https://github.com/ph1r3574r73r/black_seq_gui),
  commit `87fcea6` (2026-10-29): `blackseq/codec.py`, `blackseq/cardmap.py`.

All offsets are decimal and start at 0. All multi-byte integers are little-endian.
"varint" is the Protocol Buffers base-128 variable-length integer.

## Sample results

Each line is the full output of `blackseq diff 00.SEQ NN.SEQ`.

| Pattern | Change on the module | Changed file bytes | Old | New |
| --- | --- | --- | --- | --- |
| P2 | CH1 step 1 note C-3 to C-4 | 12..13 | `10 0e` (3600) | `c0 12` (4800) |
| P3 | CH1 step 1 note C-3 to C-6 | 12..13 | `10 0e` (3600) | `20 1c` (7200) |
| P4 | CH1 step 16 note C-3 to D-4 | 522..523 | `10 0e` (3600) | `88 13` (5000) |
| P5 | CH2 step 1 note C-3 to E-4 | 2310..2311 | `10 0e` (3600) | `50 14` (5200) |
| P6 | CH1 step 1 gate length to 80 % | 16 | `32` (50) | `50` (80) |
| P7 | CH1 step 1 muted | 22 | `01` | `00` |
| P8 | CH1 step 1 gate length to 0 | 16 | `32` (50) | `00` (0) |
| P9 | CH1 step 1 gate length to the maximum | 16 | `32` (50) | `64` (100) |
| P10 | CH1 steps 1 and 2 merged | 22 | `01` | `03` |
| P11 | CH1 last step to 8 | 2189 | `0f` (15) | `07` (7) |
| P12 | CH1 last step to 64, step 64 note to F-4 | 2154..2155 and 2189 | `10 0e` (3600) and `0f` (15) | `b4 14` (5300) and `3f` (63) |
| P13 | CH1 master track length to 32 | 5 | `10` (16) | `20` (32) |
| P14 | None (second blank) | none | | |
| P15 | CH1 steps 49 to 51 merged | 1654 and 1688 | `01` and `01` | `03` and `03` |

An earlier file for P9 had gate length 10 %: file offset 16 was `0a` (10). That file is
not in the repository now, but it is one more data point for the percent encoding.

## File layout

All 14 samples and all 7 factory files are 9198 bytes.

| File offset | Size | Encoding | Content | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| 0 | 2 | varint | Length of all bytes after this prefix: 9196 = `ec 47` | Equal in all 21 files | confirmed by samples (value). The rule "prefix = size − 2" has no sample with a different size. |
| 2 | 1 | tag | `08` (field 1, varint) | Equal in all 21 files | confirmed by samples (value) |
| 3 | 1 | varint | Value 5. Reference name: `version` | Equal in all 21 files | value confirmed; meaning unknown |
| 4 | 1 | tag | `10` (field 2, varint) | Equal in all 21 files | confirmed by samples (value) |
| 5 | 1 | varint | **Master track length**, 1 to 64 (manual page 5). Default 16 | P13: 16 to 32 | confirmed by samples |
| 6 + 2298 × c | 6 | tags and lengths | `1a f7 11 0a f4 11`: track message of 2295 bytes that holds one blob of 2292 bytes | Equal for all 4 channels in all 21 files | confirmed by samples (value) |
| 12 + 2298 × c | 2292 | | **Track blob** of channel c (0 = CH1 ... 3 = CH4) | P5: CH2 step 1 is 2298 bytes after CH1 step 1 | confirmed by samples |

Rules for the codec, from this table:

- The file size, the prefix, the field 1 value and the track framing bytes are constants.
  On read, refuse a file where one of them is different. Never write them.
- The master track length is one byte, because a varint below 128 has one byte.
- **No checksum, no pattern number, no save counter.** P1 and P14 are byte for byte
  equal, and each one-parameter sample changes only the bytes of that parameter.

## Track blob layout

The blob is 64 steps of 34 bytes, then a footer of 116 bytes. There is no header.
The note is the first field of a step.

| Blob offset | Size | Content | Evidence | Status |
| --- | --- | --- | --- | --- |
| 34 × s | 34 | Step s (0 = step 1 ... 63 = step 64) | P2: step 1 note at blob offset 0. P4: step 16 note at blob offset 510 = 34 × 15. P12: step 64 note at blob offset 2142 = 34 × 63 | confirmed by samples |
| 2176 | 116 | Footer | P11, P12: last step at blob offset 2177 | confirmed by samples (position of byte 1) |

This is hypothesis H2 of the first version of this document. The reference code uses a
different model (a 4-byte "track CV" header and the note as the last 4 bytes of a step),
and that model puts each note one step late. **Do not port the note placement of the
reference code.** The other reference offsets are correct when 4 is added to them.

In a blank pattern, all 4 track blobs are equal and all 64 steps are equal.

### Step fields

Offsets are in the 34-byte step.

| Offset | Size | Encoding | Content | Blank | Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | 4 | int32 | **Note** in cents: 1200 × octave + 100 × semitone, with the octave number that the module shows. C-3 = 3600 | 3600 | P2, P3, P4, P5, P12 | confirmed by samples. Only bytes 0 and 1 changed; bytes 2 and 3 are 0 in all files |
| 4 | 1 | uint8 | **Gate length** in percent, 0 to 100. 0 = no gate. 100 = full gate | 50 | P6 (80), P8 (0), P9 (100) | confirmed by samples |
| 5 | 1 | uint8 | Ratchet count − 1 | 0 | `cardmap.py` | reference code only |
| 6 | 1 | uint8 | Arpeggio mode | 0 | `cardmap.py` | reference code only |
| 7 | 1 | bits | Repeat count − 1, bit 7 = merge | 0 | `cardmap.py` | reference code only |
| 8 | 1 | uint8 | Glide | 0 | `cardmap.py` | reference code only |
| 9 | 1 | uint8 | Probability code, 10 = always | 10 | `cardmap.py` | reference code only |
| 10 | 1 | bits | **Gate flags.** Bit 0 = gate on (0 = muted step). Bit 1 = merged with the next step: in a group of merged steps, each step except the last one has it | 1 | P7 (0), P10 (3 on step 1, step 2 stays 1), P15 (3 on steps 49 and 50, step 51 stays 1) | confirmed by samples |
| 11 | 1 | | | 0 | none | unknown |
| 12 | 2 | int16 | Micro-timing | 0 | `cardmap.py` | reference code only |
| 14 | 1 | | | 0 | none | unknown |
| 15 | 7 | uint8 × 7 | Arpeggio intervals | 0 | `cardmap.py` | reference code only |
| 22 | 1 | uint8 | Arpeggio direction | 0 | `cardmap.py` | reference code only |
| 23 | 5 | | | 0 | none | unknown |
| 28 | 2 | int16 | Modulation value | 0 | `cardmap.py` | reference code only |
| 30 | 1 | uint8 | Modulation decay or release | 0 | `cardmap.py` | reference code only |
| 31 | 1 | uint8 | Modulation LFO wave | 0 | `cardmap.py` | reference code only |
| 32 | 1 | uint8 | Modulation trigger | 0 | `cardmap.py` | reference code only |
| 33 | 1 | uint8 | Modulation level | 100 | `codec.py` | reference code only |

Mute and gate length are independent: a muted step keeps its gate length (P7), and a
step with gate length 0 keeps gate flags 1 (P8). A full gate (P9) does not set the merge
bit. The factory project agrees: its gate flags are 0, 1 and 3, and its gate lengths
include 100.

### Footer fields

Offsets are in the 116-byte footer (blob offset 2176 + offset).

| Offset | Size | Encoding | Content | Blank | Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | 1 | | Possibly the first step of the range | 0 | none | unknown |
| 1 | 1 | uint8 | **Last step** − 1 (15 = 16 steps, 63 = 64 steps) | 15 | P11 (7), P12 (63) | confirmed by samples |
| 2 | 4 | | | 0 | none | unknown |
| 6 | 2 | uint16 | Input scale mask | 0x0FFF | `codec.py` | reference code only |
| 8 | 28 | | Byte 35 is 6 | | none | unknown |
| 36 | 1 | uint8 | Shuffle amount | 0 | `cardmap.py` | reference code only |
| 37 | 7 | | Byte 40 is 0x44 | | none | unknown |
| 44 | 1 | uint8 | Modulation mode | 0 | `cardmap.py` | reference code only |
| 45 | 3 | | | 0 | none | unknown |
| 48 | 32 | float32 and others | Modulation envelope and LFO values | | `codec.py` | reference code only (not reliable) |
| 80 | 1 | uint8 | Master glide | 0 | `cardmap.py` | reference code only |
| 81 | 16 | ASCII, zero padding | Input scale name | `CHROMATIC` | `codec.py`; text is visible in the samples | reference code only |
| 97 | 16 | ASCII, zero padding | Output scale name | `NONE` | `codec.py`; text is visible in the samples | reference code only |
| 113 | 3 | | | 0 | none | unknown |

## Project folder

| Claim | Evidence | Status |
| --- | --- | --- |
| A project is the folder `PROJECTS/<name>/` | The card copy | confirmed by samples |
| Bank b (1 to 16) is the folder with the two-digit number b − 1 | Bank 1 = `00` | confirmed by samples for bank 1 |
| Pattern p (1 to 16) is the file with the two-digit number p − 1 and the extension `.SEQ` | P1 to P14 = `00.SEQ` to `13.SEQ` | confirmed by samples |
| A bank folder holds only the patterns that are saved | 14 files for 14 saved patterns | confirmed by samples |

Other files on the card. Their content is out of scope for version 1, and the library
must not write them:

- `PROJECTS/<name>/INFO.DAT`, 8 bytes. The reference code reads a float32 tempo at offset 2.
  The module changes the last byte with no user edit of project settings: it was `0d`
  after the first save session and `0b` after the second one. Probably the last selected
  pattern. Unknown.
- `PROJECTS/<name>/<bank>/BANK.DAT`, 656 bytes in `RETEST`. The module made one in bank
  `01` also, where no pattern is saved.
- `SYSTEM/SCALES.DAT`, 256 bytes, the scale list.
- `SYSTEM/SYSTEM.DAT`, 7 bytes.

Open point for phase 3: does the module load a pattern file in a bank folder that the
module did not make, and must `BANK.DAT` list the pattern? See Q5.

## Module tests

| Date | Files | Result (reported by Nate) |
| --- | --- | --- |
| 2026-11-23 | `samples/blackseq/generated/phase1/00.SEQ`, `01.SEQ`, `02.SEQ`, copied by a computer to `PROJECTS/RETEST/01`. Firmware 1.06 | All three patterns loaded and played correctly |

This shows that the module loads a pattern file that a computer copied into an existing
bank folder, and that it accepts files with changed notes and a changed track length.
It does not cover muted steps, gate lengths other than 50 %, or channels CH2 to CH4.

## Open questions

| # | Question | What answers it |
| --- | --- | --- |
| Q4 | **MIDI note number. Decided without a test (Nate, 2026-11-22): file value = MIDI note × 100, so MIDI note 36 is C-3. If a converted file plays in the wrong octave, change the default transpose.** Original question: the file value 3600 is the note that the module shows as C-3. Which MIDI note number is that on the MIDI input and output of the module? The converter needs this. | Nate: send one known MIDI note to the module and read the note name, or tell which convention you want. Without a test, the converter uses value = MIDI note × 100 and reports the octave name that the module shows. |

## Decisions for the model, from these results

The codec (`SeqFile`) uses these rules:

- **Constants are checked, not written.** `read` refuses a file whose size, header or
  track framing is different from the samples, and a value that the model cannot hold
  (note outside MIDI 0 to 127, gate length above 100, track length outside 2 to 64).
- **A field is written only when its value changes.** `withPattern` compares each new
  value with the value that the bytes decode to. So `file.withPattern(file.pattern())` is
  equal to `file`, also for a file with a microtonal note that the model rounds.
- **Additions to the API of the brief:** `SeqFile.fromBytes`, `masterTrackLength` and
  `withMasterTrackLength`. Reason: the master track length is one value for the pattern,
  outside the four tracks, and the samples confirm it. `Pattern` keeps the shape of the brief.
- **`SeqFile.write` never replaces a file.** The replace-with-backup mode belongs to `card`.
- **`Step.mergeNext`** holds the merge bit (P10, P15). The brief has no such field; a note
  that is longer than one step needs it. The meaning of the merge bit on the last step of
  a track is not known, and the converter never sets it there. The gate length of a
  merged step is 50 % in the samples, because the merge does not change it. The converter
  writes 99 % on merged steps; the effect of that value on the module is not tested.
- **Note range.** The model accepts MIDI 0 to 127. The samples confirm 36 to 72 on the
  module. The factory files have values up to 11400 (MIDI 114).

Earlier notes:

- `Step.midiNote` cannot be the storage unit: the file stores cents. The codec converts.
  The mapping from MIDI note number to cents waits for Q4.
- `Step.gateLength` as a fraction 0.0 to 1.0 maps to the percent byte.
- A rest can be written in two confirmed ways: gate flags bit 0 = 0 (mute, P7), or gate
  length 0 (P8). Mute keeps the gate length of the step, so it is the better match for
  `Step.gateOn = false`.
- A note longer than one step needs the merge bit (P10). A merge of two steps is confirmed.
- `Pattern` needs the master track length, or the codec must keep it from the template.
