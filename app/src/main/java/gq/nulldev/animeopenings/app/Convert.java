package gq.nulldev.animeopenings.app;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import subtitleFile.*;

import java.io.*;


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
        TimedTextObject timedTextObject = ass.parseFile(out.getName(), new FileInputStream(out));
        /*Response response = client.newCall(new Request.Builder().url(url).build()).execute();
        if(response.isSuccessful()) {
            File tempAss = new File(cacheDir, filename);
            InputStream inStream = response.body().byteStream();
            if(tempAss.exists()) {
                tempAss.delete();
            }
            tempAss.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(tempAss);
            int next;
            while((next = inStream.read()) != -1) {
                outputStream.write(next);
            }
            inStream.close();
            outputStream.close();
            FormatASS ass = new FormatASS();
            if(out.exists()) {
                return out;
            }
            out.createNewFile();
            PrintWriter outWriter = new PrintWriter(new FileWriter(out));
            TimedTextObject timedTextObject = ass.parseFile(tempAss.getName(), new FileInputStream(tempAss));
            for(String line : timedTextObject.toSRT()) {
                outWriter.println(line);
            }
            tempAss.delete();
            outWriter.close();
            return out;
        }*/
        return timedTextObject;
    }

}
