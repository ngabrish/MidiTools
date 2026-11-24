# blackseq-cli

The `blackseq` command. Package `blackseq.cli`.

- Commands: `dump`, `diff`, `testfiles` (phase 1 checkpoint files) and `convert`.
- No conversion logic and no byte offsets here. Call `blackseq-lib`.
- `convert` builds each file with `SeqFile.fromPattern()`, which starts from the blank
  pattern and sets the master track length to the longest track of the pattern.
- `convert` checks all target files before the first write: it writes all files or none.
- `convert` prints more than 10 warnings as counts, one for each `WarningKind`.
- Note names and the text line of a track come from `blackseq.model.PatternText`. Do not
  make that text here.
- `dump` and `diff` only read files. `testfiles` writes new files and replaces no file.
- `samples/blackseq/generated/phase1/` holds the output of `testfiles`. A test checks that
  the committed files are equal to the generated files. Run `testfiles` into a new folder
  and copy the files if the output changes.
- A command that writes must write only inside a directory that the user gives on the
  command line. The default is to fail if the file exists.
- Do not print a stack trace for a user error. Print one clear line and return status 2.
- Tests call `Main.run(args, out, err)`.
