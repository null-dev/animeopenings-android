package gq.nulldev.animeopenings.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.*;
import com.crashlytics.android.Crashlytics;
import gq.nulldev.animeopenings.app.util.ConcurrencyUtils;
import gq.nulldev.animeopenings.app.util.SubtitleSeeker;
import io.fabric.sdk.android.Fabric;
import org.json.JSONException;
import subtitleFile.TimedTextObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ActivityNewVideo extends Activity {
    public final static String TAG = "AnimeOpenings";
    public final static Locale LOCALE = Locale.US;

    //UI Elements
    private ProgressBar bufferIndicator;
    private ImageButton settingsButton;
    private LinearLayout controlsBar;
    private TextView videoRangeText;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private SurfaceView surfaceView;
    private TextView songInfo;
    private TextView subtitleTextView;
    private LinearLayout topButtonBar;

    //Music service
    MusicService musicService;

    //Controls
    public boolean controlsShowing = true;

    //Handler
    private Handler handler;

    //Instance
    public static ActivityNewVideo INSTANCE;

    //Played videos
    public ArrayList<Video> videos;
    public Stack<Video> playedVideos = new Stack<>();

    //Currently playing video
    public Video currentVideo;

    //Gesture detector
    GestureDetector gestureDetector;

    //Playlist index
    int playlistIndex = 0;

    //Subtitles seeker
    SubtitleSeeker subtitleSeeker;

    //Media player
    MediaPlayer mediaPlayer = null;

    //Backgrounded?
    boolean inBackground = false;

    public final static int PLAY_ICON = android.R.drawable.ic_media_play;
    public final static int PAUSE_ICON = android.R.drawable.ic_media_pause;

    boolean paused = false;
    boolean initialized = false;

    HideControlsTask hideControlsTask;

    int position = -1;

    public SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Initialize fabric
        Fabric.with(this, new Crashlytics());

        super.onCreate(savedInstanceState);

        //Inflate XML
        setContentView(R.layout.activity_nv);

        INSTANCE = this;

        //Get shared prefs
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Setup gesture detector
        gestureDetector = new GestureDetector(this, new GestureListener(this));

        //Landscape orientation please
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //Setup handler
        handler = new Handler(Looper.getMainLooper());

        //Assign ui elements
        bufferIndicator = (ProgressBar) findViewById(R.id.bufferIndicator);
        settingsButton = (ImageButton) findViewById(R.id.btnSettings);
        controlsBar = (LinearLayout) findViewById(R.id.lowerBtnBar);
        videoRangeText = (TextView) findViewById(R.id.pbRange);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        updateSeekMax(100);
        playPauseButton = (ImageButton) findViewById(R.id.btnPlayPause);
        surfaceView = (SurfaceView) findViewById(R.id.fullscreen_video);
        songInfo = (TextView) findViewById(R.id.songInfo);
        subtitleTextView = (TextView) findViewById(R.id.subTextView);
        topButtonBar = (LinearLayout) findViewById(R.id.topBtnBar);

        //Default seek bar to 00:00/00:00
        updatePlaybackRangeText(0, 0);

        //Assign actions to buttons
        //Open settings on settings button click
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        //Make subtitle seeker
        subtitleSeeker = new SubtitleSeeker(mediaPlayer, subtitleTextView);

        //Gesture listener
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControls();
                }
                return true;
            }
        });

        //Allow seek bar to actually seek
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //Setup mediaplayer
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (!initialized) {
                    buildVideos();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                //Update seek bar
                                updateSeekPlayed(mediaPlayer.getCurrentPosition());
                                //Update played
                                updatePlaybackRangeText(mediaPlayer.getCurrentPosition(),
                                        mediaPlayer.getDuration());
                            }
                            handler.postDelayed(this, 500);
                        }
                    });
                    initialized = true;
                } else {
                    if (position == -1) {
                        playNextVideo();
                    } else {
                        playVideo(currentVideo);
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mediaPlayer != null) {
                    //TODO Background play
                    position = mediaPlayer.getCurrentPosition();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        });

        //Play pause video
        playPauseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (paused) {
                        mediaPlayer.start();
                        paused = false;
                    } else {
                        mediaPlayer.pause();
                        paused = true;
                    }
                    updatePlayPauseButton();
                }
            }
        });
    }

    MediaPlayer buildNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDisplay(surfaceView.getHolder());
        subtitleSeeker.setView(mediaPlayer);
        //Show buffer progress in seekbar
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                updateSeekBuffered(percent);
            }
        });
        //Show spinny thing when buffering
        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    bufferIndicator.setVisibility(View.VISIBLE);
                    return true;
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    bufferIndicator.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });
        //OnCompletionListener
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (preferences.getBoolean("prefLoopVideo", false)) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                } else {
                    playNextVideo();
                }
            }
        });
        //OnPreparedListener
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mp) {
                Log.i(TAG, "Prepared: " + mp.getDuration());
                if (position != -1) {
                    mp.seekTo(position);
                    mp.start();
                    position = -1;
                }
                //Update seek bar
                updateSeekMax(mp.getDuration());
                //Hide buffer loading indicator
                bufferIndicator.setVisibility(View.GONE);
                final TextView trackInfo = songInfo;
                String trackString = "<b>" + currentVideo.getSource() + "</b><br/>" + currentVideo.getName();
                if (currentVideo.getSubtitleSource() != null) {
                    trackString += "<br/>Subtitler: " + currentVideo.getSubtitleSource();
                }
                trackInfo.setText(Html.fromHtml(trackString));
                mediaPlayer.start();
                showControls();
            }
        });
        return mediaPlayer;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        buildVideos();
    }

    void updatePlayPauseButton() {
        if (paused) {
            playPauseButton.setImageDrawable(getResources().getDrawable(PLAY_ICON));
        } else {
            playPauseButton.setImageDrawable(getResources().getDrawable(PAUSE_ICON));
        }
    }

    void updateSeekMax(int max) {
        seekBar.setMax(max);
    }

    void updateSeekBuffered(int buffered) {
        int amountBuffered = buffered;
        //Division by zero protection
        if(amountBuffered < 1)
            amountBuffered = 1;
        seekBar.setSecondaryProgress((amountBuffered / 100) * seekBar.getMax());
    }

    void updateSeekPlayed(int percent) {
        if(mediaPlayer != null) {
            seekBar.setProgress(percent);
        }
    }

    void updatePlaybackRangeText(int cur, int max) {
        videoRangeText.setText(formatMs(cur) + "/" + formatMs(max));
    }

    void openSettings() {
        Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(openSettingsIntent);
    }

    public void buildVideos() {
        new GetVideosTask(this).execute();
    }

    void toggleControls() {
        if(controlsBar.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    void showControls() {
        //Cancel any previous hide tasks
        if(hideControlsTask != null) {
            hideControlsTask.cancel();
        }
        songInfo.setAlpha(1);
        songInfo.setVisibility(View.VISIBLE);
        songInfo.invalidate();
        controlsBar.setAlpha(1);
        controlsBar.setVisibility(View.VISIBLE);
        controlsBar.invalidate();
        topButtonBar.setAlpha(1);
        topButtonBar.setVisibility(View.VISIBLE);
        topButtonBar.invalidate();
        hideControlsTask = new HideControlsTask(controlsBar, songInfo, topButtonBar);
        handler.postDelayed(hideControlsTask, 1500);
        controlsShowing = true;
    }

    void hideControls() {
        if(hideControlsTask != null) {
            hideControlsTask.cancel();
        }
        songInfo.setAlpha(0);
        songInfo.setVisibility(View.GONE);
        songInfo.invalidate();
        controlsBar.setAlpha(0);
        controlsBar.setVisibility(View.GONE);
        controlsBar.invalidate();
        topButtonBar.setAlpha(0);
        topButtonBar.setVisibility(View.GONE);
        topButtonBar.invalidate();
        controlsShowing = false;
    }

    public void playPrevVideo() {
        if(playedVideos.size() > 0) {
            playedVideos.pop(); //Pop the current video
            playVideo(playedVideos.peek());
        } else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ActivityNewVideo.this, "No previous videos to play!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void playNextVideo() {
        Video vid = null;
        boolean handledByPlaylist = false;
        if(preferences.getBoolean("enable_playlist", false)) {
            boolean found = false;
            while(!found) {
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
            if(found) {
                handledByPlaylist = true;
            }
        }
        if(!handledByPlaylist) {
            vid = Video.getRandomVideo(videos);
            switch(preferences.getString("prefVideoType", "all")) {
                case "openings":
                    //Loop till we get an opening
                    while (!vid.getName().toUpperCase(LOCALE).contains("OPENING")) {
                        vid = Video.getRandomVideo(videos);
                    }
                    break;
                case "endings":
                    //Loop till we get an ending
                    while (!vid.getName().toUpperCase(LOCALE).contains("ENDING")) {
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
        currentVideo = vid;
        //Clear subtitles
        subtitleTextView.setText("");
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        Log.i(TAG, "Playing video: " + vid.getFileURL());

        paused = false;
        updatePlayPauseButton();

        try {
            bufferIndicator.setVisibility(View.VISIBLE);
            //Enable subtitles if possible
            if(preferences.getBoolean("prefSubtitles", true) && vid.getSubtitleSource() != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        goSubtitles(vid);
                    }
                }, "AnimeOpenings > Subtitle DP").start();
            }
            if(mediaPlayer == null)
                buildNewMediaPlayer();
            mediaPlayer.setDataSource(ActivityNewVideo.this, Uri.parse(vid.getFileURL()));
            mediaPlayer.prepareAsync();
        } catch(Exception ignored) {}
    }

    public void goSubtitles(Video vid) {
        try {
            Log.i(TAG, "Preparing subtitles for video: " + vid.getFileURL());
            final TimedTextObject converted = Convert.downloadAndParseSubtitle(vid.getSubtitleURL(), vid.getFilenameSplit(), getCacheDir());
            if(converted == null) {
                throw new IOException("Subtitles are null!");
            }
            subtitleSeeker.sync(converted);
        } catch(Throwable t) {
            Log.w(TAG, "Subtitle parse/download error!", t);
            Crashlytics.logException(t);
        }
    }

    public void onSwipeRight() {
        playNextVideo();
    }

    public void onSwipeLeft() {
        playPrevVideo();
    }

    String formatMs(int ms) {
        return String.format(LOCALE, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms))
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        INSTANCE = null;
    }
}


final class GestureListener extends GestureDetector.SimpleOnGestureListener {

    ActivityNewVideo activityNewVideo;

    private static final int SWIPE_THRESHOLD = 200;
    private static final int SWIPE_VELOCITY_THRESHOLD = 200;

    public GestureListener(ActivityNewVideo activityNewVideo) {
        this.activityNewVideo = activityNewVideo;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        activityNewVideo.onSwipeRight();
                    } else {
                        activityNewVideo.onSwipeLeft();
                    }
                    result = true;
                }
            }
//            else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
//                if (diffY > 0) {
//                    MainActivity.INSTANCE.onSwipeBottom();
//                } else {
//                    MainActivity.INSTANCE.onSwipeTop();
//                }
//            }
            result = true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }
}

class HideControlsTask implements Runnable {

    boolean canceled;
    LinearLayout controlsBar;
    TextView songInfo;
    LinearLayout topButtonBar;

    public HideControlsTask(LinearLayout controlsBar, TextView songInfo, LinearLayout topButtonBar) {
        this.controlsBar = controlsBar;
        this.songInfo = songInfo;
        this.topButtonBar = topButtonBar;
    }

    @Override
    public void run() {
        if(!canceled) {
            animateView(controlsBar);
            animateView(songInfo);
            animateView(topButtonBar);
        }
        controlsBar = null;
        songInfo = null;
        topButtonBar = null;
    }

    void animateView(final View view) {
        final AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(1000);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if(!canceled) {
                    view.setAlpha(0);
                    view.setVisibility(View.GONE);
                    view.invalidate();
                    anim.reset();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(anim);
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public void cancel() {
        this.canceled = true;
    }
}

class GetVideosTask extends AsyncTask<Void, Void, ArrayList<Video>> {

    ProgressDialog dialog;
    ActivityNewVideo activityNewVideo;

    public GetVideosTask(ActivityNewVideo activityNewVideo) {
        this.activityNewVideo = activityNewVideo;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(activityNewVideo, "Refreshing Video List", "We are getting the latest openings from openings.moe!", true, false);
    }

    @Override
    protected void onPostExecute(ArrayList<Video> videos) {
        super.onPostExecute(videos);
        if(videos != null) {
//            Log.d(LOG_TAG, Arrays.toString(videos.toArray()));
            dialog.dismiss();
            activityNewVideo.videos = videos;
            ConcurrencyUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activityNewVideo.playNextVideo();
                }
            });
        }
    }

    @Override
    protected ArrayList<Video> doInBackground(Void... params) {
        try {
            return Video.getAvailableVideos(activityNewVideo);
        } catch (IOException | JSONException e) {
            Log.e(ActivityNewVideo.TAG, "Server contact failed!");
            ConcurrencyUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activityNewVideo, "Could not contact openings server, are you offline?", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    activityNewVideo.finish();
                }
            });
            e.printStackTrace();
        }
        return null;
    }
}