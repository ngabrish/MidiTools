package blackseq.format;

/**
 * Byte positions of the .SEQ file. Each constant here has the status "confirmed by samples"
 * in docs/blackseq/FORMAT.md. Do not add a position with a different status.
 */
final class SeqLayout {

    static final int FILE_SIZE = 9198;

    /** Bytes 0 to 4: length prefix 9196, field 1 tag, value 5, field 2 tag. */
    static final int[] FILE_HEADER = {0xEC, 0x47, 0x08, 0x05, 0x10};
    /** File offset of the master track length, 1 to 64. */
    static final int MASTER_TRACK_LENGTH = 5;

    static final int FIRST_TRACK = 6;
    static final int TRACK_STRIDE = 2298;
    /** The 6 bytes before each track blob. */
    static final int[] TRACK_FRAME = {0x1A, 0xF7, 0x11, 0x0A, 0xF4, 0x11};

    static final int STEP_SIZE = 34;
    static final int STEP_COUNT = 64;
    /** int32, little-endian, cents: 100 for each semitone. */
    static final int STEP_NOTE = 0;
    /** uint8, percent, 0 to 100. */
    static final int STEP_GATE_LENGTH = 4;
    /** Bit 0 = gate on. Bit 1 = merged with the next step (read only at this time). */
    static final int STEP_GATE_FLAGS = 10;
    static final int GATE_ON_BIT = 0x01;

    static final int FOOTER = STEP_SIZE * STEP_COUNT;
    /** uint8, last step minus 1. */
    static final int FOOTER_LAST_STEP = 1;

    static final int CENTS_PER_SEMITONE = 100;
    static final int MAX_NOTE_CENTS = 127 * CENTS_PER_SEMITONE;

    private SeqLayout() {}

    /** File offset of the track blob of a channel (0 = CH1). */
    static int blob(int channel) {
        return FIRST_TRACK + TRACK_STRIDE * channel + TRACK_FRAME.length;
    }

    /** File offset of a step (0 = step 1) of a channel. */
    static int step(int channel, int step) {
        return blob(channel) + STEP_SIZE * step;
    }

    static int lastStep(int channel) {
        return blob(channel) + FOOTER + FOOTER_LAST_STEP;
    }
}
