package gq.nulldev.animeopenings.app;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Project: AnimeOpenings
 * Created: 19/11/15
 * Author: nulldev
 */
public class MusicService extends Service {

    MediaPlayer player;
    ArrayList<Video> videos;
    Stack<Video> playedVideos;
    Video currentVideo;

    public MusicService(MediaPlayer player, ArrayList<Video> videos, Stack<Video> playedVideos, Video currentVideo) {
        this.player = player;
        this.videos = videos;
        this.playedVideos = playedVideos;
        this.currentVideo = currentVideo;
    }

    public MediaPlayer getPlayer() {
        return player;
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
