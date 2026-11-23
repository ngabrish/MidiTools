# Phase 1 checkpoint: three generated test files

`blackseq testfiles` made these files from the bundled blank pattern. They are not samples
from the module. A test makes sure that they are equal to what the code generates.

**Result:** Nate tested the three files on the module on 2026-11-23 (firmware 1.06). All
three patterns loaded and played correctly.

| File | Slot | Content |
| --- | --- | --- |
| `00.SEQ` | Pattern 1 | One note: CH1 step 1 is C-4. All other steps are blank (C-3). This file is byte for byte equal to the file that the module saved for the same edit (`RETEST/00/01.SEQ`). |
| `01.SEQ` | Pattern 2 | 16-step scale: CH1 steps 1 to 16 are C major from C-3 up to D-5. |
| `02.SEQ` | Pattern 3 | 64-step track: CH1 has 64 steps. Steps 1 to 32 go up by one semitone from C-3 to G-5. Steps 33 to 64 go down from G#-5 to C#-3. |

CH2, CH3 and CH4 are blank in all three files. All notes are in the range that the
samples confirm (C-3 to C-6).

## Test procedure

Use bank 2 of the project `RETEST`. Its folder `PROJECTS/RETEST/01` exists on the card and
holds no patterns.

1. Make a backup of the card.
2. With the card in the computer, copy `00.SEQ`, `01.SEQ` and `02.SEQ` from this folder to
   `PROJECTS/RETEST/01` on the card. Do not copy this README.
3. Put the card in the module and switch on the power.
4. Load the project `RETEST` (`SHIFT + PATTERN`). Select bank 2 (hold BANK, push step
   encoder 2).
5. For each of patterns 1, 2 and 3: select the pattern, look at the pages in the table
   below, then push PLAY and listen.

| Pattern | What the module must show | What you must hear on CH1 |
| --- | --- | --- |
| 1 | CV page: step 1 is C-4, steps 2 to 16 are C-3 | One note that is an octave above the other 15 |
| 2 | CV page: C-3 D-3 E-3 F-3 G-3 A-3 B-3 C-4 D-4 E-4 F-4 G-4 A-4 B-4 C-5 D-5 | A major scale that goes up for 16 steps |
| 3 | DRCTN page: range 01-64. CV page, bar 1: C-3 to D#-4 in semitones. Bar 3, step 33: G#-5. Bar 4, step 64: C#-3 | 32 steps up in semitones, then 32 steps down |

## What to report

For each pattern:

- Does the BANK page count the three patterns, and are the slot LEDs on?
- Does the pattern load with no error message?
- Does the CV page show the notes in the table?
- Does CH1 play the correct notes and gates?
- Are CH2 to CH4 the same as in a blank pattern?
- All other behaviour that is not the same as for a pattern that the module made.

Optional, but useful: save pattern 2 on the module with `SHIFT + RECORD`, and send the
file `PROJECTS/RETEST/01/01.SEQ` from the card. If the module changed bytes in it, those
bytes show what the library must do differently.
