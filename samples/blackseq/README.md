# Sample patterns from the module

Nate saves these patterns on the Black Sequencer. Each sample changes one parameter from
a blank pattern. The format analysis (phase 0) cannot start before they are here.

The button names and page numbers are from the Erica Synths user manual "3-2023 v2".
"Step encoder 1" is the first of the 16 encoders. `SHIFT + X` means: push and hold SHIFT,
then push X.

## Before you start

1. Make a backup of the SD card: copy the full card to the computer. Do not format the
   card and do not delete files from it. The manual has no factory reset, and it does not
   say if the module makes the files in `SYSTEM` again when they are missing.
2. Push SETUP, turn DATA to the **Info** menu and push DATA. Write down the firmware
   version and the bootloader version (page 25).
3. In the SETUP menu, open the **Default** settings (page 26). Turn DATA to select a
   setting and turn step encoder 1 to change it. Set the factory values: default note C0,
   default gate length 50 %, gate state ON. These settings are the only module settings
   that change a blank pattern. Write down the values that the display shows.
4. Make a new project (page 13): `SHIFT + PATTERN`, push DATA, turn DATA to **NEW**, enter
   the name `RETEST` with the step encoders, push DATA to confirm.
5. Select bank 1: hold BANK and push step encoder 1 (page 5).
6. Keep the sequencer stopped (STOP) during all edits.

## Procedure for each sample

1. Push PATTERN, then hold SHIFT and push the step encoder of the pattern slot (page 14).
   With SHIFT, the change is immediate while the sequencer is stopped.
2. Push CH1. Make sure that the pattern is blank: on the CV page all steps show the
   default note, and on the GATE page all steps show the default gate length.
3. For the first sample only: `SHIFT + QUANT` (page 6). Write down the scale that the
   display shows. If the scale is NONE, stop and tell Claude Code, because the step
   encoders then move in cents and not in notes. Do not change the scale.
4. Make the one change from the table. Do not touch other controls. The RECORD button
   starts to blink (page 5).
5. Write down the value that the pop-up screen shows before and after the change.
6. Save: `SHIFT + RECORD` (pages 5 and 13). The RECORD button stops blinking and the slot
   LED is on.
7. Push BANK. The number in bank 1 is the quantity of saved patterns (page 14). It must be
   one more than before.

Slot LEDs: the manual gives green for "selected and empty", orange for "selected and
saved" and red for "saved". Nate's module shows a lit LED for a saved slot and no light
for an empty slot. Use the rule "LED on = saved" and the number on the BANK page. An edit that is not saved is lost when you select a different
   pattern.

If the module does not let you do a step, do not find a different method. Write in
`MANIFEST.md` what you did.

## The samples

All samples are in bank 1 of the project `RETEST`.

| Pattern | Change from blank | Operation |
| --- | --- | --- |
| 1 | None | `SHIFT + RECORD` only. See "Blank patterns" below. |
| 2 | CH1 step 1: note **1 octave up** | CH1, CV. Hold SHIFT and turn step encoder 1 clockwise until the octave number is 1 more than the default note (page 7). |
| 3 | CH1 step 1: note **3 octaves up** | Same as pattern 2, until the octave number is 3 more than the default note. |
| 4 | CH1 step 16: note **1 octave up, then D** | CH1, CV. SHIFT and step encoder 16 for the octave. Release SHIFT and turn clockwise to D in the same octave. |
| 5 | CH2 step 1: note **1 octave up, then E** | CH2, CV, step encoder 1. Same method as pattern 4. |
| 6 | CH1 step 1: gate length **80 %** (setting 8) | CH1, GATE. Turn step encoder 1 (page 8). |
| 7 | CH1 step 1: **muted** | CH1, GATE. Push step encoder 1 one time (page 8). |
| 8 | CH1 step 1: gate length **0** | CH1, GATE. Turn step encoder 1 fully counterclockwise. |
| 9 | CH1 step 1: gate length **10** (full gate) | CH1, GATE. Turn step encoder 1 fully clockwise. |
| 10 | CH1 steps 1 and 2: **merged** | CH1, GATE. Push step encoders 1 and 2 at the same time (page 8). |
| 11 | CH1: last step **8** | CH1, DRCTN/RANGE. Push step encoder 8, or turn step encoder 2 (page 6). |
| 12 | CH1: last step **64**, then step 64: note **1 octave up, then F** | CH1, DRCTN/RANGE, turn step encoder 2 to 64. Then CV, BAR> to bar 4, step encoder 16. |
| 13 | CH1: master track length **32** | CH1, SCALE. Turn step encoder 2 (page 5). |
| 14 | None (control sample) | Same as pattern 1. |

The notes are relative to the default note of the blank pattern. The default note on
Nate's module is not the C0 of the manual, so a fixed note name can be equal to the blank
value.

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
LED stays off, the module did not save the pattern. In that case change the note of
CH1 step 1 by one semitone, set it back to the default note, and save. Write in
`MANIFEST.md` which method you used.

## Copy the files

1. Switch off the power of the case, then remove the SD card (page 27).
2. Copy the full folder `PROJECTS/RETEST` to `samples/blackseq/project/RETEST`.
   Do not rename files. Do not open them in an editor.
3. If the card has a `SYSTEM` folder, copy it to `samples/blackseq/SYSTEM`.
4. Write the list of files and folders in the root of the SD card in `MANIFEST.md`.
5. Complete `MANIFEST.md`. The checklist page makes this text from your entries.
6. Commit the files to this repository, or attach a zip file of `samples/blackseq/` to
   the chat.

Claude Code makes the named copies (`blank.SEQ` and so on) from the project folder.

## Compare two samples

```bash
mvn -B -q package
```

```bash
java -jar blackseq-cli/target/blackseq.jar diff <a.SEQ> <b.SEQ>
```
