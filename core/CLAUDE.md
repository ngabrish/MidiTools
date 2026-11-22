# core

Neutral note data and the MIDI file reader. Other tools will use this module.

- `miditools.core.note`: `NoteEvent`, `NoteClip`. Depends on nothing.
- `miditools.core.midi`: `MidiFileReader` (`javax.sound.midi`). Depends on `note`. Phase 3.

Rules:

- No import of a `blackseq` package.
- No device knowledge: no `.SEQ` format, no 4-channel limit, no 64-step limit.
- `NoteClip` is the only input to converters. Do not add MIDI-file data to it.
