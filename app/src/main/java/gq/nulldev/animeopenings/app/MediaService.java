package gq.nulldev.animeopenings.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.danikula.videocache.HttpProxyCacheServer;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import gq.nulldev.animeopenings.app.util.SubtitleSeeker;

/**
 * Project: AnimeOpenings
 * Created: 19/11/15
 * Author: nulldev
 */
public class MediaService extends Service implements LibVLC.HardwareAccelerationError {

    public static final String ACTION_PREV = "gq.nulldev.animeopenings.app.ACTION_PREV";
    public static final String ACTION_PLAYPAUSE = "gq.nulldev.animeopenings.app.ACTION_PLAYPAUSE";
    public static final String ACTION_NEXT = "gq.nulldev.animeopenings.app.ACTION_NEXT";
    public static final String ACTION_EXIT = "gq.nulldev.animeopenings.app.ACTION_EXIT";

    static HttpProxyCacheServer proxyCacheServer;
    static long previousProxyCacheSize = -1;

    private LibVLC libvlc;
    private MediaPlayer player = null;
    ArrayList<Video> videos;
    Stack<Video> playedVideos = new Stack<>();
    Video currentVideo = null;
    SubtitleSeeker subtitleSeeker = null;
    SharedPreferences preferences;
    OnMediaPlayerBuiltListener onMediaPlayerBuiltListener;
    OnMediaPlayerReleasedListener onMediaPlayerReleasedListener;
    Runnable onStopListener = null;
    int playlistIndex = 0;
    boolean paused = false;

    //MediaNotification
    MediaNotification notification;

    public void setupService(ArrayList<Video> videos, SubtitleSeeker subtitleSeeker, SharedPreferences preferences) {
        this.videos = videos;
        this.subtitleSeeker = subtitleSeeker;
        this.preferences = preferences;
    }

    public static HttpProxyCacheServer getProxy(Context context, long size) {
        if (proxyCacheServer == null
                || previousProxyCacheSize == -1
                || size != previousProxyCacheSize) {
            if(proxyCacheServer != null) {
                proxyCacheServer.shutdown();
            }
            proxyCacheServer = new HttpProxyCacheServer.Builder(context)
                    .maxCacheSize(size)
                    .build();
            previousProxyCacheSize = size;
        }
        return proxyCacheServer;
    }

    public static String proxyURL(Context context, SharedPreferences preferences, String url) {
        if(preferences.getBoolean("prefCacheVideos", false)) {
            int cacheLimit = Integer.parseInt(preferences.getString("prefCacheLimit", "512"));
            HttpProxyCacheServer proxy = getProxy(context,
                    cacheLimit * 1024 * 1024);
            return proxy.getProxyUrl(url);
        } else {
            return url;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (!action.isEmpty()) {
                switch (action) {
                    case ACTION_PREV:
                        doPrev();
                        break;
                    case ACTION_PLAYPAUSE:
                        doPlayPause();
                        break;
                    case ACTION_NEXT:
                        doNext();
                        break;
                    case ACTION_EXIT:
                        if (getPlayer() != null) {
                            getPlayer().stop();
                            getPlayer().release();
                            player = null;
                            if (onStopListener != null)
                                onStopListener.run();
                            stopSelf();
                        }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    boolean doPrev() {
        boolean result = playPrevVideo();
        updateNotification();
        return result;
    }

    void doPlayPause() {
        if (player != null) {
            if (paused) {
                player.play();
                paused = false;
            } else {
                try {
                    player.pause();
                } catch (IllegalStateException e) {
                    //Cannot pause while player is buffering, just stop the player entirely
                    player.stop();
                }
                paused = true;
            }
        }
        updateNotification();
    }

    void doNext() {
        playNextVideo();
        updateNotification();
    }

    public boolean playPrevVideo() {
        if (playedVideos.size() > 0) {
            playedVideos.pop(); //Pop the current video
            playVideo(playedVideos.peek());
            return true;
        } else {
            return false;
        }
    }

    public void playVideo(final Video vid) {
        subtitleSeeker.deSync(); //Desync the subtitle seeker
        currentVideo = vid;
        //Clear subtitles
        if (player != null) {
            releasePlayer();
        }
        String url = vid.getFileURL();
        if (preferences.getBoolean("prefAudioOnly", false)) {
            url = "http://omam.nulldev.xyz/" + vid.getFile() + ".ogg";
        }
        Log.i(ActivityNewVideo.TAG, "Playing media: " + url);

        paused = false;

        try {
            if (player == null)
                buildNewMediaPlayer();
            //TODO
            Media m = new Media(libvlc, Uri.parse(proxyURL(this, preferences, url)));
            player.setMedia(m);
            player.play();
        } catch (Exception ignored) {
            Log.e(ActivityNewVideo.TAG, "Exception thrown while preparing player!", e);
        }
        updateNotification();
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        if(onMediaPlayerReleasedListener != null)
            onMediaPlayerReleasedListener.onMediaPlayerReleased(player);
        player.stop();
        final IVLCVout vout = player.getVLCVout();
        vout.detachViews();
        player.release();
        libvlc.release();
        libvlc = null;
        player = null;
    }

    public void playNextVideo() {
        Video vid = null;
        boolean handledByPlaylist = false;
        if (preferences.getBoolean("enable_playlist", false)) {
            boolean found = false;
            while (!found) {
                //Disable empty playlist
                //noinspection Convert2Diamond (Doesn't work without it)
                if (preferences.getStringSet("playlist", new HashSet<String>()).size() < 1) {
                    preferences.edit().putBoolean("enable_playlist", false).apply();
                    break;
                } else {
                    //noinspection Convert2Diamond (Doesn't work without it)
                    Set<String> playlistSet = preferences.getStringSet("playlist", new HashSet<String>());
                    String[] playlist = playlistSet.toArray(new String[playlistSet.size()]);
                    if (playlistIndex >= playlist.length) {
                        playlistIndex = 0;
                    }
                    String target = playlist[playlistIndex];
                    playlistIndex++;
                    for (Video video : videos) {
                        if (video.getFile().equals(target)) {
                            vid = video;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        playlistSet.remove(target);
                        preferences
                                .edit()
                                .remove("playlist")
                                .putStringSet("playlist", playlistSet)
                                .apply();
                    }
                }
            }
            if (found) {
                handledByPlaylist = true;
            }
        }
        if (!handledByPlaylist) {
            vid = Video.getRandomVideo(videos);
            switch (preferences.getString("prefVideoType", "all")) {
                case "openings":
                    //Loop till we get an opening
                    while (!vid.getName().toUpperCase(ActivityNewVideo.LOCALE).contains("OPENING")) {
                        vid = Video.getRandomVideo(videos);
                    }
                    break;
                case "endings":
                    //Loop till we get an ending
                    while (!vid.getName().toUpperCase(ActivityNewVideo.LOCALE).contains("ENDING")) {
                        vid = Video.getRandomVideo(videos);
                    }
                    break;
                default:
                    //Do nothing really
                    break;
            }
        }
        playedVideos.push(vid);
        playVideo(vid);
    }

    public void updateNotification() {
//        if(notification != null) {
//            notification.cancel();
//        }
        if (getCurrentVideo() != null) {
            notification = new MediaNotification(this,
                    getCurrentVideo().getName(),
                    getCurrentVideo().getSource(),
                    isPaused());
        }
    }

    MediaPlayer buildNewMediaPlayer() {
        try {
            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            libvlc = new LibVLC(options);
            libvlc.setOnHardwareAccelerationError(this);

            // Create media player
            player = new MediaPlayer(libvlc);

            // Set up video output
//            final IVLCVout vout = mMediaPlayer.getVLCVout();
//            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
//            vout.addCallback(this);
//            vout.attachViews();
            player.play();
        } catch (Exception e) {
            Log.e(ActivityNewVideo.TAG, "Error creating player!", e);
        }
        if (subtitleSeeker != null)
            subtitleSeeker.setPlayer(player);

        if (onMediaPlayerBuiltListener != null) {
            onMediaPlayerBuiltListener.onMediaPlayerBuilt(player);
        }
        updateNotification();
        return player;
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public long getPositionMS() {
        return (long) (player.getPosition() * player.getLength());
    }

    public boolean isPlaying() {
        try {
            return player.isPlaying();
        } catch(IllegalStateException e) {
            //Player is released so obviously not playing
            return false;
        }
    }

    public void setPlayer(MediaPlayer player) {
        this.player = player;
    }

    public ArrayList<Video> getVideos() {
        return videos;
    }

    public void setVideos(ArrayList<Video> videos) {
        this.videos = videos;
    }

    public Stack<Video> getPlayedVideos() {
        return playedVideos;
    }

    public void setPlayedVideos(Stack<Video> playedVideos) {
        this.playedVideos = playedVideos;
    }

    public Video getCurrentVideo() {
        return currentVideo;
    }

    public void setCurrentVideo(Video currentVideo) {
        this.currentVideo = currentVideo;
    }

    public OnMediaPlayerReleasedListener getOnMediaPlayerReleasedListener() {
        return onMediaPlayerReleasedListener;
    }

    public void setOnMediaPlayerReleasedListener(OnMediaPlayerReleasedListener onMediaPlayerReleasedListener) {
        this.onMediaPlayerReleasedListener = onMediaPlayerReleasedListener;
    }

    public boolean isPaused() {
        return paused;
    }

    public OnMediaPlayerBuiltListener getOnMediaPlayerBuiltListener() {
        return onMediaPlayerBuiltListener;
    }

    public void setOnMediaPlayerBuiltListener(OnMediaPlayerBuiltListener onMediaPlayerBuiltListener) {
        this.onMediaPlayerBuiltListener = onMediaPlayerBuiltListener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MediaBinder();
    }

    @Override public void eventHardwareAccelerationError() {
        //TODO
        Log.e(ActivityNewVideo.TAG, "HW acceleration error!");
    }

    public LibVLC getLibvlc() {
        return libvlc;
    }

    public void setLibvlc(LibVLC libvlc) {
        this.libvlc = libvlc;
    }

    public interface OnMediaPlayerBuiltListener {
        void onMediaPlayerBuilt(MediaPlayer mediaPlayer);
    }

    public interface OnMediaPlayerReleasedListener {
        void onMediaPlayerReleased(MediaPlayer player);
    }

    public class MediaBinder extends Binder {
        MediaService getService() {
            return MediaService.this;
        }
    }

    public SubtitleSeeker getSubtitleSeeker() {
        return subtitleSeeker;
    }

    public void setSubtitleSeeker(SubtitleSeeker subtitleSeeker) {
        this.subtitleSeeker = subtitleSeeker;
    }

    public Runnable getOnStopListener() {
        return onStopListener;
    }

    public void setOnStopListener(Runnable onStopListener) {
        this.onStopListener = onStopListener;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notification != null) {
            notification.cancel();
        }
    }
}
