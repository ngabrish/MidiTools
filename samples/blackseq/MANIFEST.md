# Sample manifest

From Nate, 2026-11-22. The column "File result" is from Claude Code: it says if the file
shows the change. Details are in `docs/blackseq/FORMAT.md`.

- Firmware version (SETUP, Info): 1.06
- Bootloader version (SETUP, Info): 0.2
- Date of the samples: 2026-11-22
- Project name on the card: `RETEST`
- Files and folders in the root of the SD card: SYSTEM, CAPTURE, PROJECTS, EBSEQ_V1_05_4F172EA5.BIN, EBSEQ_V1_06_7824E1DD.BIN

## Module settings

- Default note (SETUP, Default): C-3
- Default gate length (SETUP, Default): 50
- Default gate state (SETUP, Default): ON
- Scale on the QUANT page of CH1 in a blank pattern: Chromatic (Root C)

## Values of a blank pattern

- CH1 step 1 note (CV page): C-3
- CH1 step 1 gate length (GATE page): bar at half height, no number shown
- CH1 first step and last step (DRCTN/RANGE page): 01-16
- CH1 master track length (SCALE page): 16

## Module behaviour that is different from the manual

- The factory default note is C-3. The manual says C0.
- The module writes a note as letter, `-`, octave number (for example `C-3`). The `-` is a
  separator, not a minus sign.
- On the PATTERN page, the LED of a saved slot is on and the LED of an empty slot is off.
  The manual gives red, orange and green states.
- `SHIFT + RECORD` saves a pattern that has no edits.

## Samples

Pattern p is the file `PROJECTS/RETEST/00/NN.SEQ` with NN = p − 1.

| Bank | Pattern | File | Change | Display before | Display after | Saved | Notes | File result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 | `00.SEQ` | None (blank) | n/a | n/a | yes | | Base |
| 1 | 2 | `01.SEQ` | CH1 step 1 note C-3 to C-4 | C-3 | C-4 | yes | | Shows the change |
| 1 | 3 | `02.SEQ` | CH1 step 1 note C-3 to C-6 | C-3 | C-6 | yes | | Shows the change |
| 1 | 4 | `03.SEQ` | CH1 step 16 note C-3 to D-4 | C-3 | D-4 | yes | | Shows the change |
| 1 | 5 | `04.SEQ` | CH2 step 1 note C-3 to E-4 | | | yes | | Shows the change |
| 1 | 6 | `05.SEQ` | CH1 step 1 gate length to 80 % | | | yes | | Shows the change |
| 1 | 7 | `06.SEQ` | CH1 step 1 muted (push) | | | yes | Saved a second time | Shows the change |
| 1 | 8 | `07.SEQ` | CH1 step 1 gate length to 0 | | | yes | | Shows the change |
| 1 | 9 | `08.SEQ` | CH1 step 1 gate length to 10 (full) | | | yes | Saved a second time at the maximum | Shows the change |
| 1 | 10 | `09.SEQ` | CH1 steps 1 and 2 merged | | | yes | | Shows the change |
| 1 | 11 | `10.SEQ` | CH1 last step to 8 | | | yes | | Shows the change |
| 1 | 12 | `11.SEQ` | CH1 last step to 64, step 64 note C-3 to F-4 | | | yes | Saved a second time | Shows the two changes |
| 1 | 13 | `12.SEQ` | CH1 master track length to 32 | | | yes | | Shows the change |
| 1 | 14 | `13.SEQ` | None (blank, control) | n/a | n/a | yes | | Equal to `00.SEQ` |
| 1 | 15 | `14.SEQ` | CH1 three steps merged (2026-11-23) | | | yes | "Ties the first 3 steps together" | Shows the change on steps 49 to 51 (bar 4), not on steps 1 to 3 |
