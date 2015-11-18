package gq.nulldev.animeopenings.app.util;

import android.os.Handler;
import android.os.Looper;

/**
 * Project: AndroidUtils
 * Created: 21/10/15
 * Author: nulldev
 */
public class ConcurrencyUtils {
    public static void runOnUiThread(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
