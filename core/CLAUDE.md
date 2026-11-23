# core

Neutral note data and the MIDI file reader. Other tools will use this module.

- `miditools.core.note`: `NoteEvent`, `NoteClip`. Depends on nothing.
- `miditools.core.midi`: `MidiFileReader` (`javax.sound.midi`). Depends on `note`.
  One clip for each track and MIDI channel that has notes. All clips of a file have the
  same length: the file length, increased to the end of its last bar.

Rules:

- No import of a `blackseq` package.
- No device knowledge: no `.SEQ` format, no 4-channel limit, no 64-step limit.
- `NoteClip` is the only input to converters. Do not add MIDI-file data to it.
