package gq.nulldev.animeopenings.app;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import subtitleFile.FormatASS;
import subtitleFile.TimedTextObject;

/**
 * Converts subtitles to a parsable format.
 */
public class Convert {

    static OkHttpClient client = new OkHttpClient();

    public static TimedTextObject downloadAndParseSubtitle(String url, String filename, File cacheDir) throws IOException {
        cacheDir.mkdirs();
        File out = new File(cacheDir, filename + ".ass");
        //Download first
        if(!out.exists()) {
            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            if(response.isSuccessful()) {
                File tempAss = new File(cacheDir, filename + ".ass");
                InputStream inStream = response.body().byteStream();
                if (tempAss.exists()) {
                    tempAss.delete();
                }
                tempAss.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(tempAss);
                int next;
                while ((next = inStream.read()) != -1) {
                    outputStream.write(next);
                }
                inStream.close();
                outputStream.close();
            } else {
                return null;
            }
        }
        FormatASS ass = new FormatASS();
        return ass.parseFile(out.getName(), new FileInputStream(out));
    }

}
