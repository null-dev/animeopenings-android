package gq.nulldev.animeopenings.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Project: AnimeOpenings
 * Created: 19/11/15
 * Author: nulldev
 */
public class NotificationListenerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = (String) getIntent().getExtras().get("DO");
        if (action.equals("volume")) {
            Log.i("AnimeOpenings", "NOTE: volume");
        } else if (action.equals("stopNotification")) {
            Log.i("AnimeOpenings", "NOTE: stopNotification");
        }
        finish();
    }
}
