package gq.nulldev.animeopenings.app;

import android.content.Context;
import android.preference.PreferenceManager;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Project: AnimeOpenings
 * Created: 02/10/15
 * Author: nulldev
 */
public class Video {

    public static final String BASE_URL = "openings.moe";

    public static final String LIST_URL = "http://" + BASE_URL + "/api/list.php";
    public static final String VIDEO_URL_BASE = "http://" + BASE_URL + "/video/";
    public static final String SUBTITLE_URL_BASE = "http://" + BASE_URL + "/subtitles/";
    public static final String EGG_APPEND = "?eggs";

    public static final String SUBTITLE_EXT = ".ass";

    static Random random = new Random();

    String name;
    String source;
    String file;
    String subtitleSource = null;
    String subtitleURL = null;
    String filenameSplit = null;

    public static ArrayList<Video> getAvailableVideos(Context context) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        String url = LIST_URL;
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefEasterEggs", true)) {
            url += EGG_APPEND;
        }
        Request request = new Request.Builder().url(url).build();

        Response response = client.newCall(request).execute();

        if(!response.isSuccessful()) {
            throw new IOException("Response was not successful!");
        }

        JSONArray jsonArray = new JSONArray(response.body().string());

        ArrayList<Video> videos = new ArrayList<>();

        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            Video video = new Video(object.getString("title"), object.getString("source"), object.getString("file"));

            String[] split = video.getFile().split("\\.");
            if(split.length > 0) {
                video.setFilenameSplit(split[0]);

                //Add subtitles
                String subs = object.getString("subtitles");
                if(subs != null && !subs.equals("0")) {
                    video.setSubtitleSource(subs);
                    video.setSubtitleURL(SUBTITLE_URL_BASE + video.getFilenameSplit() + SUBTITLE_EXT);
                }
            }

            videos.add(video);
        }

        return videos;
    }

    public static Video getRandomVideo(ArrayList<Video> videos) {
        return videos.get(random.nextInt(videos.size()));
    }

    public Video(String name, String source, String file) {
        this.name = name;
        this.source = source;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFileURL() {
        return VIDEO_URL_BASE + file;
    }

    public String getSubtitleSource() {
        return subtitleSource;
    }

    public void setSubtitleSource(String subtitleSource) {
        this.subtitleSource = subtitleSource;
    }

    public String getSubtitleURL() {
        return subtitleURL;
    }

    public void setSubtitleURL(String subtitleURL) {
        this.subtitleURL = subtitleURL;
    }

    public String getFilenameSplit() {
        return filenameSplit;
    }

    public void setFilenameSplit(String filenameSplit) {
        this.filenameSplit = filenameSplit;
    }

    @Override
    public String toString() {
        return "Video{" +
                "name='" + name + '\'' +
                ", source='" + source + '\'' +
                ", file='" + file + '\'' +
                '}';
    }

    public static List<ListSeriesItem> sortVideos(ArrayList<Video> videos) {
        HashMap<String, ListSeriesItem> sorted = new HashMap<>();
        for(Video video : videos) {
            if(!sorted.containsKey(video.getSource())) {
                sorted.put(video.getSource(), new ListSeriesItem(video.getSource()));
            }
            sorted.get(video.getSource()).getChildren().add(new ListVideoItem(video));
        }
        return new ArrayList<>(sorted.values());
    }

    public static class ListVideoItem {
        Video video;
        boolean isSelected = false;

        public ListVideoItem(Video video) {
            this.video = video;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setIsSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }

        public Video getVideo() {
            return video;
        }

        public void setVideo(Video video) {
            this.video = video;
        }
    }

    public static class ListSeriesItem {
        String name;
        ArrayList<ListVideoItem> children;

        public ListSeriesItem(String name) {
            this.name = name;
            this.children = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ArrayList<ListVideoItem> getChildren() {
            return children;
        }

        public void setChildren(ArrayList<ListVideoItem> children) {
            this.children = children;
        }
    }
}
