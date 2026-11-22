# Sample patterns from the module

Nate saves these patterns on the Black Sequencer. Each sample changes one parameter from
a blank pattern. The format analysis (phase 0) cannot start before they are here.

This procedure does not name buttons, because the user manual was not available when it
was written. Use the manual for the operation of the module.

## Before you start

1. Make a backup of the full SD card on the computer.
2. Write down the firmware version of the module.
3. Create a new project on the module. Suggested name: `RETEST`. Use this project only
   for the samples.

## Rules for each sample

- Start each sample from a **blank** pattern. Do not continue from the previous sample.
- Change **only** the parameter in the table. Do not touch other controls.
- Use the note names in the table. Different notes in different samples make the bytes
  easy to identify.
- Before the change, write down the value that the display shows. After the change, write
  down the new value. Put the two values in `MANIFEST.md`.
- Save the pattern. If the module does not let you do a step (for example, gate length
  cannot be edited on a step with no note), do not find a different method. Write in
  `MANIFEST.md` what you did.

## The samples

All samples are in bank 1 of the project `RETEST`.

| Pattern | Change from blank |
| --- | --- |
| 1 | No change. Save the blank pattern. |
| 2 | CH1 step 1: note set to **C3** |
| 3 | CH1 step 1: note set to **C5** (two octaves above pattern 2) |
| 4 | CH1 step 16: note set to **D3** |
| 5 | CH2 step 1: note set to **E3** |
| 6 | CH1 step 1: gate length changed to a value that is not the default |
| 7 | CH1 step 1: gate switched off |
| 8 | CH1: track length changed to **8** steps |
| 9 | CH1: track length changed to **64** steps, then step 64: note set to **F3** |
| 10 | No change. A second blank pattern (control sample). |

Pattern 10 shows which bytes are different between two files when no parameter is
different (for example a pattern number or a counter).

If the module does not make a file for a pattern with no edits, write that in
`MANIFEST.md`. That is useful data. Then make patterns 1 and 10 with one edit that you
set back to the initial value.

## Copy the files

1. Stop the sequencer and remove the SD card as the manual tells you.
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
