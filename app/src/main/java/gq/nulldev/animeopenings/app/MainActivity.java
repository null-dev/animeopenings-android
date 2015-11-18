/*

REPLACED WITH "ActivityNewVideo"

package gq.nulldev.animeopenings.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import gq.nulldev.animeopenings.app.util.ScreenReceiver;
import gq.nulldev.animeopenings.app.util.SubtitleSeeker;
import io.fabric.sdk.android.Fabric;
import org.json.JSONException;
import subtitleFile.TimedTextObject;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.sample.widget.media.AndroidMediaController;
import tv.danmaku.ijk.media.sample.widget.media.IjkVideoView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

*/
/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *//*
public class MainActivity extends Activity {


    public static final String SETTINGS_AD_UNIT = "ca-app-pub-4804830220165854/5458961128";

    public static MainActivity INSTANCE;

    public static SharedPreferences sharedPrefs;

    GestureDetector gestureDetector;

    public ArrayList<Video> videos;
    public Stack<Video> playedVideos = new Stack<>();

    boolean paused = false;
    boolean screenOff = false;
    int stopLoc = 0;
    int playlistIndex = 0;

    BroadcastReceiver screenReceiver;

    public static final String LOG_TAG = "AnimeOpenings";

    public static int AD_GRACE = 3-1;

    SubtitleSeeker subtitleSeeker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Fabric.with(this, new Crashlytics());
//        try {
//            StartAppSDK.init(this, "209361997", true);
//        } catch(Throwable t) {
//             Exception loading advertisements :{
//            Log.e("AnimeOpenings", "Error loading ads!", t);
//        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Show ads every 3 opens
        if (getIntent() == null || getIntent().getBooleanExtra("showAd", true)) {
            if(sharedPrefs.getInt("leftAdGrace", 0) <= 0) {
//                StartAppAd.showSplash(this, savedInstanceState);
                sharedPrefs.edit().putInt("leftAdGrace", AD_GRACE).apply();
            } else {
                sharedPrefs.edit().putInt("leftAdGrace", sharedPrefs.getInt("leftAdGrace", AD_GRACE)-1).apply();
            }
        }

        INSTANCE = this;
        if (sharedPrefs.getBoolean("firstrun", true)) {
            sharedPrefs.edit().putBoolean("firstrun", false).apply();
            try {
                new OkHttpClient().newCall(new Request.Builder().url("http://api.nulldev.xyz/install.php").build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                    }
                });
            } catch (Exception ignored) {}
        } else {
            new OkHttpClient().newCall(new Request.Builder().url("http://api.nulldev.xyz/run.php").build()).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                }

                @Override
                public void onResponse(Response response) throws IOException {
                }
            });
        }

        super.onCreate(savedInstanceState);

        gestureDetector = new GestureDetector(this, new GestureListener());

        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // initialize receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver, filter);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.controls_info).postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
                anim.setDuration(1000);
                anim.setFillEnabled(true);
                anim.setFillAfter(true);
                findViewById(R.id.controls_info).startAnimation(anim);
            }
        }, 3000);

        final IjkVideoView videoView = (IjkVideoView) findViewById(R.id.fullscreen_video);

        videoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                if(sharedPrefs.getBoolean("prefLoopVideo", false)) {
                    videoView.pause();
                    videoView.seekTo(0);
                    videoView.start();
                } else {
                    playNextVideo();
                }
            }
        });

        subtitleSeeker = new SubtitleSeeker(videoView, (TextView) findViewById(R.id.subTextView));

        AndroidMediaController vidControl = new AndroidMediaController(this);
        vidControl.setAnchorView(videoView);
        videoView.setMediaController(vidControl);
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        buildVideos();
    }
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if(sharedPrefs.getBoolean("prefContinuePlaying", true)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                playNextVideo();
                return true;
            } else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                playPrevVideo();
                return true;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }


    @Override
    protected void onPause() {
        // when the screen is about to turn off
        if (ScreenReceiver.wasScreenOn) {
            IjkVideoView videoView = (IjkVideoView) findViewById(R.id.fullscreen_video);
            if(!sharedPrefs.getBoolean("prefContinuePlaying", true)) {
                stopLoc = videoView.getCurrentPosition();
                videoView.pause();
                paused = true;
                screenOff = true;
            } else {
                videoView.enterBackground();
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (!ScreenReceiver.wasScreenOn) {
            IjkVideoView videoView = (IjkVideoView) findViewById(R.id.fullscreen_video);
            if(paused) {
                videoView.seekTo(stopLoc);
                videoView.start();
                paused = false;
            } else {
                videoView.stopBackgroundPlay();
            }
            screenOff = false;
        }

        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    void onSwipeRight() {
        playNextVideo();
    }
    void onSwipeLeft() {
        playPrevVideo();
    }

    TrackInfoShower currentTrackInfoShower;
    void onSwipeBottom() {

        Log.i(LOG_TAG, "Showing track info!");

        final TextView trackInfo = (TextView) findViewById(R.id.song_info);

        trackInfo.clearAnimation();
        trackInfo.setAlpha(1.0f);

        if(currentTrackInfoShower != null) {
            trackInfo.removeCallbacks(currentTrackInfoShower);
        }

        currentTrackInfoShower = new TrackInfoShower(trackInfo);

        trackInfo.postDelayed(currentTrackInfoShower, 2000);
    }

    class TrackInfoShower implements Runnable {
        View trackInfo;

        public TrackInfoShower(View trackInfo) {
            this.trackInfo = trackInfo;
        }

        @Override
        public void run() {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(1000);
            anim.setFillEnabled(true);
            anim.setFillAfter(true);
            trackInfo.startAnimation(anim);
        }
    }

    ControlsShower currentControls;
    void onSwipeTop() {
        Log.d(LOG_TAG, "Showing controls!");

        final View controls = findViewById(R.id.controls_info);

        controls.clearAnimation();
        controls.setAlpha(1.0f);

        if(currentControls != null) {
            controls.removeCallbacks(currentControls);
        }

        currentControls = new ControlsShower(controls);

        controls.postDelayed(currentControls, 3000);
    }

    //Shows controls info
    class ControlsShower implements Runnable {

        View controls;

        public ControlsShower(View controls) {
            this.controls = controls;
        }

        @Override
        public void run() {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(1000);
            anim.setFillEnabled(true);
            anim.setFillAfter(true);
            controls.startAnimation(anim);
        }
    }
*/
    /*=================================================================*/
/*
    public void buildVideos() {
        new GetVideosTask().execute();
    }

    public void playPrevVideo() {
        if(playedVideos.size() > 0) {
            playedVideos.pop(); //Pop the current video
            playVideo(playedVideos.peek());
        } else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.INSTANCE, "No previous videos to play!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void playCurrentVideo() {
        if(playedVideos.size() > 0) {
            playVideo(playedVideos.peek());
        }
    }

    public void playNextVideo() {
        Video vid = null;
        boolean handledByPlaylist = false;
        if(sharedPrefs.getBoolean("enable_playlist", false)) {
            boolean found = false;
            while(!found) {
                //noinspection Convert2Diamond (Doesn't work without it)
                if (sharedPrefs.getStringSet("playlist", new HashSet<String>()).size() < 1) {
                    sharedPrefs.edit().putBoolean("enable_playlist", false).apply();
                    break;
                } else {
                    //noinspection Convert2Diamond (Doesn't work without it)
                    Set<String> playlistSet = sharedPrefs.getStringSet("playlist", new HashSet<String>());
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
                        sharedPrefs
                                .edit()
                                .remove("playlist")
                                .putStringSet("playlist", playlistSet)
                                .apply();
                    }
                }
            }
            if(found) {
                handledByPlaylist = true;
            }
        }
        if(!handledByPlaylist) {
            vid = Video.getRandomVideo(videos);
            switch(sharedPrefs.getString("prefVideoType", "all")) {
                case "openings":
                    //Loop till we get an opening
                    while (!vid.getName().toUpperCase().contains("OPENING")) {
                        vid = Video.getRandomVideo(videos);
                    }
                    break;
                case "endings":
                    //Loop till we get an ending
                    while (!vid.getName().toUpperCase().contains("ENDING")) {
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

    public void playVideo(final Video vid) {
        subtitleSeeker.deSync(); //Desync the subtitle seeker
        final IjkVideoView videoView = (IjkVideoView) findViewById(R.id.fullscreen_video);
        //Clear subtitles
        ((TextView) findViewById(R.id.subTextView)).setText("");
        videoView.stopPlayback();
        Log.i(LOG_TAG, "Playing video: " + vid.getFileURL());

        ProgressDialog progressDialog = null;
        if(!inBackground) {
            progressDialog = ProgressDialog.show(this, "Buffering Video", "Please wait, buffering video...", true, false);
        }

        final ProgressDialog finalProgressDialog = progressDialog;
        videoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final IMediaPlayer mp) {
                if(finalProgressDialog != null) {
                    finalProgressDialog.dismiss();
                }
                final TextView trackInfo = (TextView) findViewById(R.id.song_info);
                String trackString = "<b>" + vid.getSource() + "</b><br/>" + vid.getName();
                if (vid.getSubtitleSource() != null) {
                    trackString += "<br/>Subtitler: " + vid.getSubtitleSource();
                }
                trackInfo.setText(Html.fromHtml(trackString));
                trackInfo.setAlpha(1.0f);
                trackInfo.clearAnimation();
                trackInfo.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
                        anim.setDuration(1000);
                        anim.setFillEnabled(true);
                        anim.setFillAfter(true);
                        trackInfo.startAnimation(anim);
                    }
                }, 2000);
            }
        });
        try {
            //Enable subtitles if possible
            if(sharedPrefs.getBoolean("prefSubtitles", true) && vid.getSubtitleSource() != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        goSubtitles(vid);
                    }
                }, "AnimeOpenings > Subtitle DP").start();
            }
            videoView.setVideoURI(Uri.parse(vid.getFileURL()));
            videoView.seekTo(0);
            videoView.start();
        } catch(Exception ignored) {}
    }

    public void goSubtitles(Video vid) {
        try {
            Log.i("AnimeOpenings", "Preparing subtitles for video: " + vid.getFileURL());
            final TimedTextObject converted = Convert.downloadAndParseSubtitle(vid.getSubtitleURL(), vid.getFilenameSplit(), getCacheDir());
            if(converted == null) {
                throw new IOException("Subtitles are null!");
            }
            subtitleSeeker.sync(converted);
        } catch(Throwable t) {
            Log.w(LOG_TAG, "Subtitle parse/download error!", t);
            Crashlytics.logException(t);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            Log.i(LOG_TAG, "Opening settings!");
            Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(openSettingsIntent);
            final IjkVideoView videoView = (IjkVideoView) findViewById(R.id.fullscreen_video);
            if(videoView != null) {
                videoView.stopPlayback();
            }
            finish();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final IjkVideoView videoView = (IjkVideoView) findViewById(R.id.fullscreen_video);
            if(videoView != null) {
                videoView.stopPlayback();
            }
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    boolean inBackground = false;

    @Override
    protected void onStop() {
        super.onStop();

        if(isFinishing()) {
            inBackground = true;
        }
    }
}*/