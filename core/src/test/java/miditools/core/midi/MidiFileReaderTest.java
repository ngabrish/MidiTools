package miditools.core.midi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import miditools.core.note.NoteClip;
import miditools.core.note.NoteEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MidiFileReaderTest {

    private static final int PPQ = 480;

    @TempDir
    Path dir;

    private static void note(Track track, int channel, int number, long startTick, long ticks) throws Exception {
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, number, 100), startTick));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, number, 0), startTick + ticks));
    }

    private static void name(Track track, String name) throws Exception {
        byte[] data = name.getBytes(StandardCharsets.ISO_8859_1);
        track.add(new MidiEvent(new MetaMessage(0x03, data, data.length), 0));
    }

    private Path write(Sequence sequence, String fileName) throws IOException {
        Path file = dir.resolve(fileName);
        MidiSystem.write(sequence, 1, file.toFile());
        return file;
    }

    @Test
    void eachTrackWithNotesIsOneClipWithItsNameAndBeats() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track tempo = sequence.createTrack();          // no notes: no clip
        name(tempo, "Tempo");
        Track bass = sequence.createTrack();
        name(bass, "Bass");
        note(bass, 0, 36, 0, 120);                     // beat 0, a sixteenth
        note(bass, 0, 43, 3 * PPQ + 240, 480);         // beat 3.5, a quarter
        Track lead = sequence.createTrack();
        note(lead, 1, 72, PPQ, 240);

        List<NoteClip> clips = MidiFileReader.read(write(sequence, "two.mid"));
        assertEquals(2, clips.size());
        assertEquals("Bass", clips.get(0).name());
        assertEquals(List.of(new NoteEvent(0.0, 0.25, 36, 100), new NoteEvent(3.5, 1.0, 43, 100)),
                clips.get(0).notes());
        assertEquals("Track 3", clips.get(1).name());
        assertEquals(List.of(new NoteEvent(1.0, 0.5, 72, 100)), clips.get(1).notes());
    }

    @Test
    void clipLengthGoesToTheEndOfTheLastBar() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        note(track, 0, 60, 0, 240);
        note(track, 0, 62, 4 * PPQ + 240, 240);        // ends at beat 5.0: the second bar of 4/4
        List<NoteClip> clips = MidiFileReader.read(write(sequence, "bars.mid"));
        assertEquals(8.0, clips.get(0).lengthBeats());
    }

    @Test
    void barLengthIsFromTheTimeSignature() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        track.add(new MidiEvent(new MetaMessage(0x58, new byte[] {3, 2, 24, 8}, 4), 0));   // 3/4
        note(track, 0, 60, 0, 240);
        note(track, 0, 62, 3 * PPQ, 240);              // second bar of 3/4
        List<NoteClip> clips = MidiFileReader.read(write(sequence, "waltz.mid"));
        assertEquals(6.0, clips.get(0).lengthBeats());
    }

    @Test
    void fileThatEndsOnABarLineKeepsItsLength() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        note(track, 0, 60, 0, 4 * PPQ);
        assertEquals(4.0, MidiFileReader.read(write(sequence, "bar.mid")).get(0).lengthBeats());
    }

    @Test
    void twoChannelsInOneTrackAreTwoClips() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        name(track, "Keys");
        note(track, 2, 60, 0, 240);
        note(track, 0, 48, 0, 240);
        List<NoteClip> clips = MidiFileReader.read(write(sequence, "channels.mid"));
        assertEquals(2, clips.size());
        assertEquals("Keys (MIDI channel 1)", clips.get(0).name());
        assertEquals(48, clips.get(0).notes().get(0).midiNote());
        assertEquals("Keys (MIDI channel 3)", clips.get(1).name());
    }

    @Test
    void noteOnWithVelocityZeroEndsANote() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 90), 0));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 0), 240));
        List<NoteClip> clips = MidiFileReader.read(write(sequence, "running.mid"));
        assertEquals(List.of(new NoteEvent(0.0, 0.5, 60, 90)), clips.get(0).notes());
    }

    @Test
    void secondNoteOnForTheSameNoteEndsTheFirst() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 90), 0));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 80), 480));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 60, 0), 720));
        List<NoteClip> clips = MidiFileReader.read(write(sequence, "retrigger.mid"));
        assertEquals(List.of(new NoteEvent(0.0, 1.0, 60, 90), new NoteEvent(1.0, 0.5, 60, 80)),
                clips.get(0).notes());
    }

    @Test
    void noteWithNoNoteOffEndsAtTheEndOfTheTrack() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 90), 0));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 1, 0), 960));
        List<NoteClip> clips = MidiFileReader.read(write(sequence, "hanging.mid"));
        assertEquals(List.of(new NoteEvent(0.0, 2.0, 60, 90)), clips.get(0).notes());
    }

    @Test
    void notesAreInStartSequence() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();
        note(track, 0, 60, 0, 2000);                   // ends after the next note starts
        note(track, 0, 64, 480, 100);
        List<NoteEvent> notes = MidiFileReader.read(write(sequence, "overlap.mid")).get(0).notes();
        assertEquals(60, notes.get(0).midiNote());
        assertEquals(64, notes.get(1).midiNote());
    }

    @Test
    void fileWithNoNotesGivesNoClips() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        name(sequence.createTrack(), "Empty");
        assertTrue(MidiFileReader.read(write(sequence, "empty.mid")).isEmpty());
    }

    @Test
    void fileThatIsNotMidiIsAnIOException() throws Exception {
        Path file = Files.writeString(dir.resolve("text.mid"), "this is not MIDI");
        IOException e = assertThrows(IOException.class, () -> MidiFileReader.read(file));
        assertTrue(e.getMessage().contains("Not a MIDI file"), e.getMessage());
        assertThrows(IOException.class, () -> MidiFileReader.read(dir.resolve("none.mid")));
    }

    @Test
    void smpteTimeIsRefused() throws Exception {
        Sequence sequence = new Sequence(Sequence.SMPTE_25, 40);
        note(sequence.createTrack(), 0, 60, 0, 10);
        IOException e = assertThrows(IOException.class, () -> MidiFileReader.read(write(sequence, "smpte.mid")));
        assertTrue(e.getMessage().contains("SMPTE"), e.getMessage());
    }
}
