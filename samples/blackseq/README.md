# Sample patterns from the module

Nate saves these patterns on the Black Sequencer. Each sample changes one parameter from
a blank pattern. The format analysis (phase 0) cannot start before they are here.

The button names and page numbers are from the Erica Synths user manual "3-2023 v2".
"Step encoder 1" is the first of the 16 encoders. `SHIFT + X` means: push and hold SHIFT,
then push X.

## Before you start

1. Make a backup of the SD card: copy the `PROJECTS` folder to the computer (page 27).
2. Push SETUP, turn DATA to the **Info** menu and push DATA. Write down the firmware
   version and the bootloader version (page 25).
3. In the SETUP menu, open the **Default** settings (page 26). Write down the default note,
   the default gate length and the default gate state. The factory values are C0, 50 % and
   ON. Do not change them.
4. Make a new project (page 13): `SHIFT + PATTERN`, push DATA, turn DATA to **NEW**, enter
   the name `RETEST` with the step encoders, push DATA to confirm.
5. Select bank 1: hold BANK and push step encoder 1 (page 5).
6. Keep the sequencer stopped (STOP) during all edits.

## Procedure for each sample

1. Push PATTERN and push the step encoder of the pattern slot (page 14). The LED of an
   empty selected slot is green.
2. Push CH1. Make sure that the pattern is blank: on the CV page all steps show the
   default note, and on the GATE page all steps show the default gate length.
3. For the first sample only: `SHIFT + QUANT` (page 6). Write down the scale that the
   display shows. If the scale is NONE, stop and tell Claude Code, because the step
   encoders then move in cents and not in notes. Do not change the scale.
4. Make the one change from the table. Do not touch other controls. The RECORD button
   starts to blink (page 5).
5. Write down the value that the pop-up screen shows before and after the change.
6. Save: `SHIFT + RECORD` (pages 5 and 13). The RECORD button stops blinking and the slot
   LED becomes orange. An edit that is not saved is lost when you select a different
   pattern.

If the module does not let you do a step, do not find a different method. Write in
`MANIFEST.md` what you did.

## The samples

All samples are in bank 1 of the project `RETEST`.

| Pattern | Change from blank | Operation |
| --- | --- | --- |
| 1 | None | `SHIFT + RECORD` only. See "Blank patterns" below. |
| 2 | CH1 step 1: note **C3** | CH1, CV. Hold SHIFT and turn step encoder 1 to change the octave. Release SHIFT to change the note (page 7). |
| 3 | CH1 step 1: note **C5** | Same as pattern 2. |
| 4 | CH1 step 16: note **D3** | CH1, CV, step encoder 16. |
| 5 | CH2 step 1: note **E3** | CH2, CV, step encoder 1. |
| 6 | CH1 step 1: gate length **80 %** (setting 8) | CH1, GATE. Turn step encoder 1 (page 8). |
| 7 | CH1 step 1: **muted** | CH1, GATE. Push step encoder 1 one time (page 8). |
| 8 | CH1 step 1: gate length **0** | CH1, GATE. Turn step encoder 1 fully counterclockwise. |
| 9 | CH1 step 1: gate length **10** (full gate) | CH1, GATE. Turn step encoder 1 fully clockwise. |
| 10 | CH1 steps 1 and 2: **merged** | CH1, GATE. Push step encoders 1 and 2 at the same time (page 8). |
| 11 | CH1: last step **8** | CH1, DRCTN/RANGE. Push step encoder 8, or turn step encoder 2 (page 6). |
| 12 | CH1: last step **64**, then step 64: note **F3** | CH1, DRCTN/RANGE, turn step encoder 2 to 64. Then CV, BAR> to bar 4, step encoder 16. |
| 13 | CH1: master track length **32** | CH1, SCALE. Turn step encoder 2 (page 5). |
| 14 | None (control sample) | Same as pattern 1. |

Why there are more samples than in the brief:

- **Patterns 7 to 10.** The manual gives three different operations that make a step
  silent or long: mute, gate length 0, and full gate or merge. The converter needs "rest"
  and "note longer than one step", so the bytes of each operation must be known.
- **Pattern 13.** The file has one value outside the four tracks. The master track length
  is the probable meaning.
- **Pattern 14.** Two blank patterns show which bytes are different when no parameter is
  different.

### Blank patterns

For patterns 1 and 14, select the slot and do `SHIFT + RECORD` with no edit. If the slot
LED stays green, the module did not save the pattern. In that case change the note of
CH1 step 1 by one semitone, set it back to the default note, and save. Write in
`MANIFEST.md` which method you used.

## Copy the files

1. Switch off the power of the case, then remove the SD card (page 27).
2. Copy the full folder `PROJECTS/RETEST` to `samples/blackseq/project/RETEST`.
   Do not rename files. Do not open them in an editor.
3. Write the list of files and folders in the root of the SD card in `MANIFEST.md`.
4. Complete `MANIFEST.md`.
5. Commit the files to this repository, or attach a zip file of `samples/blackseq/` to
   the chat.

Claude Code makes the named copies (`blank.SEQ` and so on) from the project folder.

## Compare two samples

```bash
mvn -B -q package
```

```bash
java -jar blackseq-cli/target/blackseq.jar diff <a.SEQ> <b.SEQ>
```
