# midi-tools

Java tools for MIDI and hardware sequencers. The first tool reads and writes pattern
files for the Erica Synths Black Sequencer.

## Layout

```
pom.xml            parent: Java level, JUnit version, module list
jitpack.yml        Java version for the JitPack build of the library artifacts
core/              note model and MIDI file reader; no device knowledge
blackseq-lib/      Black Sequencer model, .SEQ codec, converter, card writer
blackseq-cli/      the blackseq command
docs/blackseq/     FORMAT.md, the .SEQ format notes
samples/blackseq/  pattern files that Nate saved on the module
```

Each module has its own `CLAUDE.md` with the rules for that module.
`bitwig-extension/` is planned. Do not create it now.

There is no desktop GUI in this repository. Nate decided (2026-11-24) to replace the
JavaFX program of the first brief with the web GUI in the Synthnomicon application
(`ngabrish/synthnomicon`): MIDI conversion, note editing, a pattern editor, saved
patterns and a browser preview. Do not create `blackseq-gui/`. `blackseq-lib` has no
user-interface code, so a desktop program stays possible later.

## Build

```bash
mvn -B verify
```

Java 17 language level, Maven, JUnit 5. No runtime dependencies.

## Rules for the full repository

- Dependencies go in one direction: `blackseq-cli` -> `blackseq-lib` -> `core`.
- `core` imports no `blackseq` package and knows nothing about a device.
- The target platform is Windows first. Use `java.nio.file.Path` for all paths. CI runs
  the tests on Windows and Linux.
- Release tags have a module prefix, for example `blackseq-cli-v1.0`. A tag
  `blackseq-lib-v<version>` is a library release: JitPack builds `core` and `blackseq-lib`
  from it for other projects (see `blackseq-lib/CLAUDE.md`).
- Do not commit to `main`. Make each change on a branch and open a pull request. Merge a
  pull request only when Nate tells you to.
- Work in phases. Stop at each checkpoint until Nate replies. The phase list is in
  `blackseq-lib/CLAUDE.md`.
- Only Nate can test on the module. Do not say that a file works on the module.
