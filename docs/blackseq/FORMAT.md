# Black Sequencer `.SEQ` format

Status of this document: **phase 0, before samples.** No field is `confirmed by samples`,
because `samples/blackseq/` is empty. No code in this repository writes a `.SEQ` file.

## Rules for this document

Each field has an offset, a size, an encoding, the evidence and a status:

| Status | Meaning | Can the codec write it? |
| --- | --- | --- |
| `confirmed by samples` | A sample pair from Nate's module shows it. | Yes |
| `reference code only` | The reference tool says so. Its fixtures can agree, but they are not proof. | No |
| `unknown` | No evidence for the meaning. | No. Copy from the template. |

## Sources

- **Samples**: `samples/blackseq/` and its `MANIFEST.md`. Not supplied at this time.
- **User manual**: Erica Synths "Black Sequencer user manual 3-2023 v2". It describes the
  functions of the module, not the file. It is not in this repository.
- **Reference code**: [black_seq_gui](https://github.com/ph1r3574r73r/black_seq_gui),
  commit `87fcea6` (2026-10-29). Files `blackseq/codec.py` (envelope, note, scale names),
  `blackseq/cardmap.py` (step and footer fields), `blackseq/midi_import.py` (gate values),
  `blackseq/library.py` (file names), `web/app.js` (step 1 note handling).
- **Reference fixtures**: `tests/fixtures/01.SEQ`, `02.SEQ`, `INFO.DAT`, `00.SNG`,
  `SYSTEM.DAT` and the `DEMO BANK` project in that repository. Their origin is not
  documented. The reference author calls `01.SEQ` a factory pattern. They are not Nate's
  samples and they are not in this repository.

All offsets are decimal and start at 0. All multi-byte integers are little-endian.
"varint" is the Protocol Buffers base-128 variable-length integer.

## File layout (reference code only)

In the reference fixtures, each `.SEQ` file is 9198 bytes. The file is a length prefix
followed by a Protocol Buffers message.

| File offset | Size | Encoding | Content | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| 0 | 2 | varint | Length of all bytes after this prefix (9196 = `ec 47`) | `codec.py` `unframe`; fixtures | reference code only |
| 2 | 2 | tag `08` + varint | Field 1. Value 5 in all fixtures. Reference name: `version` | `codec.py` `parse_pattern` | reference code only |
| 4 | 2 | tag `10` + varint | Field 2. Value 16 in the fixtures. Reference name: `field2`; an error text calls it "master length" | `codec.py` `parse_pattern` | reference code only (meaning: unknown) |
| 6 | 2298 | tag `1a` + varint 2295 + message | Track CH1 | `codec.py` `_parse_track` | reference code only |
| 2304 | 2298 | same | Track CH2 | same | reference code only |
| 4602 | 2298 | same | Track CH3 | same | reference code only |
| 6900 | 2298 | same | Track CH4 | same | reference code only |

Each track message is `0a` + varint 2292 (`f4 11`) + a 2292-byte **track blob**. The four
blobs start at file offsets 12, 2310, 4608 and 6906 in the fixtures.

The length prefix is a length field. The rule "prefix = file size − prefix size" must be
confirmed with a minimum of three samples before any write. The template method does not
change the file size, so a correct write never changes these two bytes. The codec must
still verify the prefix on read and refuse a file that does not agree.

The file has no checksum in the reference code. This is not evidence that the module has
none. See open question Q3.

## Track blob layout: two hypotheses

The reference code and its own fixture do not agree. This is the most important open
question (Q1).

**H1, the reference code.** A 4-byte header, 64 steps of 34 bytes, a 112-byte footer.
The note is the last field of each step.

| Blob offset | Size | Content |
| --- | --- | --- |
| 0 | 4 | `cvCents`, int32. Reference: "track CV offset" |
| 4 + 34 × s | 34 | Step s (s = 0 to 63). Note at step offset 30 |
| 2180 | 112 | Footer |

**H2, an alternative that the reference fixture supports.** 64 steps of 34 bytes with the
note as the *first* field, then a 116-byte footer. No header.

| Blob offset | Size | Content |
| --- | --- | --- |
| 34 × s | 34 | Step s (s = 0 to 63). Note at step offset 0 |
| 2176 | 116 | Footer |

H2 is H1 with each boundary moved 4 bytes earlier. The two give the same byte positions
but different step numbers for the notes: the H1 note of step s is the H2 note of step s + 1.

Evidence for H2 (not proof):

- In `01.SEQ`, CH1 has `cvCents` = 5600, 15 notes in H1 steps 1 to 15, and no note on H1
  step 16, in a track that the reference decodes as 16 steps long. With H2 the same bytes
  are 16 notes on steps 1 to 16.
- With H1, the track length is in "the second byte of the note field of step 64"
  (`cardmap.py` `_last_step`) and a blank step 64 has the "note" 3840. With H2 these
  bytes are the start of the footer, and no step field has two purposes.
- `web/app.js` (`cvPageShift`, `shownNote`) contains a heuristic that sometimes shows
  `cvCents` as the note of step 1 and moves all other notes one step. A tool that needs
  this heuristic does not have a correct model.

Result if H2 is correct: `blackseq/midi_import.py` in the reference writes each note one
step late. Do not port its note placement before Q1 is closed.

## Fields in a track blob (reference code only)

The table gives blob offsets, which are the same for H1 and H2. `s` is the H1 step index
(0 to 63). For H2, the fields of one step are the 4 note bytes at `34 × s` and the 30
bytes that follow.

| Blob offset | Size | Encoding | Reference meaning | Fixture value | Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | 4 | int32 | H1: track CV offset. H2: note of step 1 | 5600 / 5200 / 0 | `codec.py` `_parse_track` | reference code only |
| 4 + 34s + 0 | 1 | uint8 | Gate length, 0 to 100. Import writes 100 for a held step and 80 for the last step of a note. 0 with note 0 = empty | 50 | `codec.py` `GATE_OFFSET`; `midi_import.py` | reference code only |
| 4 + 34s + 1 | 1 | uint8 | Ratchet count − 1 | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 2 | 1 | uint8 | Arpeggio mode (4 = major) | 0 | `cardmap.py` `_arp` | reference code only |
| 4 + 34s + 3 | 1 | bits | Repeat count − 1 in bits 0 to 6; "merge" in bit 7 | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 4 | 1 | uint8 | Glide, 0 to 10 | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 5 | 1 | uint8 | Probability code, 10 = always (`codec.py` name: `gateIndex`) | 10 | `cardmap.py` | reference code only |
| 4 + 34s + 6 | 1 | uint8 | `flag` | 1 | `codec.py` `FLAG_OFFSET` | unknown |
| 4 + 34s + 7 | 1 | | | 0 | none | unknown |
| 4 + 34s + 8 | 2 | int16 | Micro-timing / shuffle offset | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 10 | 1 | | | 0 | none | unknown |
| 4 + 34s + 11 | 7 | uint8 × 7 | Arpeggio intervals | 0 | `cardmap.py` `_arp` | reference code only |
| 4 + 34s + 18 | 1 | uint8 | Arpeggio direction | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 19 | 5 | | | 0 | none | unknown |
| 4 + 34s + 24 | 2 | int16 | Modulation value (volts × 120, or a note) | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 26 | 1 | uint8 | Modulation decay / release | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 27 | 1 | uint8 | Modulation LFO wave | 0 | `cardmap.py` | reference code only |
| 4 + 34s + 28 | 1 | uint8 | Modulation trigger (inverted on step 1) | 0 | `cardmap.py` `_trigger_on` | reference code only |
| 4 + 34s + 29 | 1 | uint8 | `level` | 100 | `codec.py` `LEVEL_OFFSET` | unknown |
| 4 + 34s + 30 | 4 | int32 | Note = MIDI note number × 100 (cents). 6000 = C4 in the reference. 0 = no note | 0 or 2300 to 5900 | `codec.py` `NOTE_OFFSET`, `note_name` | reference code only |
| 2177 | 1 | uint8 | Last step index (track length − 1). 15 = 16 steps | 15 | `cardmap.py` `_last_step` | reference code only |
| 2176, 2178, 2179 | 3 | | H1: other bytes of the step 64 note | 0 | none | unknown |
| 2180 + 0 | 2 | | | 0 | none | unknown |
| 2180 + 2 | 2 | uint16 | Input scale mask, 12 bits (0x0FFF = chromatic) | 0x0FFF | `codec.py` | reference code only |
| 2180 + 4 | 28 | | Includes byte 31 = 6 in the fixtures | | none | unknown |
| 2180 + 32 | 1 | uint8 | Shuffle amount | 0 | `cardmap.py` | reference code only |
| 2180 + 33 | 7 | | Includes byte 36 = 0x44 in the fixtures | | none | unknown |
| 2180 + 40 | 1 | uint8 | Modulation mode, 0 to 8 | 0 | `cardmap.py` `MODES` | reference code only |
| 2180 + 41 | 3 | | | 0 | none | unknown |
| 2180 + 44 | 32 | float32 and others | Modulation envelope and LFO values. The reference constants overlap (offset 64 is "ADSR" and "LFO level") | 0.1, 0.5, 0.1, 0, 1.0 at 48 to 67 | `codec.py` | reference code only (not reliable) |
| 2180 + 76 | 1 | uint8 | Master glide, 0 to 10 | 0 | `cardmap.py` | reference code only |
| 2180 + 77 | 16 | ASCII, zero padding | Input scale name | `CHROMATIC` | `codec.py`; fixtures | reference code only |
| 2180 + 93 | 16 | ASCII, zero padding | Output scale name | `NONE` | `codec.py`; fixtures | reference code only |
| 2180 + 109 | 3 | | | 0 | none | unknown |

Version 1 of the library needs only these fields: note, gate on/off, gate length, track
length. All other fields stay as the template has them.

## Project folder (reference code only)

From `library.py` and the `DEMO BANK` folder in the reference repository:

```
PROJECTS/<project name>/
  INFO.DAT          tempo and two flags (10 bytes in the fixture)
  00/ .. 15/        one folder for each bank, two decimal digits
    BANK.DAT        671 bytes in the fixture; content unknown
    00.SEQ .. 15.SEQ  one file for each pattern, two decimal digits
    00.SNG ..       songs
SYSTEM/SCALES.DAT   scale list
```

A bank folder holds only the patterns that exist. Bank and pattern numbers start at 0 on
the card. The brief uses 1 to 16 in `ProjectFolder`, so `patternPath(1, 1)` would be
`00/00.SEQ`. Status: reference code only, until Nate's project folder copy confirms it.

## Open questions

| # | Question | Sample that answers it |
| --- | --- | --- |
| Q1 | H1 or H2: which bytes hold the note of step 1? | "One note changed on CH1 step 1" against `blank.SEQ`. H1 predicts a change at file offsets 46 to 49. H2 predicts 12 to 15. "CH1 step 16" and "64 steps with a note on step 64" must agree. |
| Q2 | Is the note value MIDI note × 100, and which octave name does the module show for 6000? | The step 1 sample and the "two octaves higher" sample: the difference must be 2400. The manifest must give the note names that the module shows. |
| Q3 | Is there a checksum, or other bytes that change when a note changes? | Each one-parameter sample: the diff must show only the bytes of that parameter. |
| Q4 | Which byte is the gate length, and what is its encoding? The manual gives settings 0 to 10 and a default of 50 % (page 8). The reference code stores setting × 10. | Patterns 6, 8 and 9 (gate length 80 %, 0 and 10). |
| Q5 | How does the module store a muted step? The manual gives mute (push) and gate length 0 as two operations (page 8). | Pattern 7 (muted) against pattern 8 (gate length 0). |
| Q5b | How does the module store a note that is longer than one step? The manual says that gate length 10 merges the step with the next one, and that a push of two encoders merges steps (page 8). | Pattern 9 (gate length 10) against pattern 10 (steps 1 and 2 merged). |
| Q6 | Where is the last step of a track? Is file field 2 (value 16) the master track length of the manual (page 5)? Is there a first step field? | Patterns 11 and 12 (last step 8 and 64) and pattern 13 (master track length 32). |
| Q7 | Do all channels use the same layout at a fixed distance of 2298 bytes? | "One note changed on CH2 step 1". |
| Q8 | Is the file size always 9198 bytes? Does the length prefix rule hold? | All samples (a minimum of three). |
| Q9 | Are the file and folder names as shown above? Which other files does a project hold? | The project folder copy. |
| Q10 | What is the value of field 1 (5 in the fixtures) on Nate's firmware? | `blank.SEQ` and the firmware version in the manifest. The codec must refuse an unknown value. |
| Q11 | Does a blank pattern from the module have gate length 50 and probability 10 on each step, as the fixture has? | `blank.SEQ`. |

The pattern numbers are the slots in `samples/blackseq/README.md`.

The manual also says (page 8) that a step can be skipped (two fast pushes), and that the
range of a track has a first step and a play direction (page 6). These are out of scope
for version 1. Their bytes stay as the template has them.
