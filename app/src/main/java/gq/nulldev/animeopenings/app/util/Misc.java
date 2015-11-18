package gq.nulldev.animeopenings.app.util;

import android.media.MediaPlayer;

/**
 * Project: AnimeOpenings
 * Created: 22/10/15
 * Author: nulldev
 */
public class Misc {
    public static int findTrackIndexFor(int mediaTrackType, MediaPlayer.TrackInfo[] trackInfo) {
        int index = -1;
        for (int i = 0; i < trackInfo.length; i++) {
            if (trackInfo[i].getTrackType() == mediaTrackType) {
                return i;
            }
        }
        return index;
    }
}
