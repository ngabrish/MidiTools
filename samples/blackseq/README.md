# Sample patterns from the module

Nate saves these files on the Black Sequencer and copies them here. Each sample changes
one parameter from the blank pattern. Phase 0 analysis cannot start before they are here.

Copy the files from the SD card without changes. Do not open them in an editor.

| File name (suggestion) | Content |
| --- | --- |
| `blank.SEQ` | A new pattern with no edits |
| `ch1-step1-note.SEQ` | One note changed on CH1 step 1 |
| `ch1-step1-note-plus2oct.SEQ` | The same step, note two octaves higher |
| `ch1-step16-note.SEQ` | One note changed on CH1 step 16 |
| `ch2-step1-note.SEQ` | One note changed on CH2 step 1 |
| `ch1-step1-gatelen.SEQ` | Gate length changed on CH1 step 1 |
| `ch1-step1-gateoff.SEQ` | Gate switched off on CH1 step 1 |
| `ch1-len8.SEQ` | CH1 track length changed from the default to 8 steps |
| `ch1-len64-step64-note.SEQ` | CH1 track length 64, with a note on step 64 |
| `project/` | A copy of one complete project folder |
| `MANIFEST.md` | One line for each file, plus the firmware version |

For each line in `MANIFEST.md`, write the exact values that the module display shows
before and after the change. For example: "CH1 step 1 note: C3 changed to E3" or
"CH1 step 1 gate length: 50 % changed to 75 %". The note names and the gate values are
necessary to find the encoding.

Compare two samples:

```bash
java -jar blackseq-cli/target/blackseq.jar diff samples/blackseq/blank.SEQ samples/blackseq/ch1-step1-note.SEQ
```
