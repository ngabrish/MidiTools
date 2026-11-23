# blackseq-cli

The `blackseq` command. Package `blackseq.cli`.

- Commands: `dump`, `diff`, `testfiles` (phase 1 checkpoint files) and `convert`.
- No conversion logic and no byte offsets here. Call `blackseq-lib`.
- `convert` builds each file from `SeqFile.blank()`, and sets the master track length to
  the longest track of the pattern.
- `convert` checks all target files before the first write: it writes all files or none.
- `convert` prints more than 10 warnings as counts. It finds the kind of a warning from
  the words `dropped:`, `moved:` and `shortened:` in the message. If the converter gets
  a new kind of warning, add it to `WARNING_KINDS`; a warning of an unknown kind is
  counted as "other".
- `dump` and `diff` only read files. `testfiles` writes new files and replaces no file.
- `samples/blackseq/generated/phase1/` holds the output of `testfiles`. A test checks that
  the committed files are equal to the generated files. Run `testfiles` into a new folder
  and copy the files if the output changes.
- A command that writes must write only inside a directory that the user gives on the
  command line. The default is to fail if the file exists.
- Do not print a stack trace for a user error. Print one clear line and return status 2.
- Tests call `Main.run(args, out, err)`.
