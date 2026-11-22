package blackseq.model;

import java.util.List;

/**
 * One pattern: the four tracks CH1 to CH4.
 *
 * @param tracks exactly 4 tracks; index 0 is CH1
 */
public record Pattern(List<Track> tracks) {

    public static final int CHANNELS = 4;

    public Pattern {
        tracks = List.copyOf(tracks);
        if (tracks.size() != CHANNELS) {
            throw new IllegalArgumentException("a pattern has exactly 4 tracks: " + tracks.size());
        }
    }
}
