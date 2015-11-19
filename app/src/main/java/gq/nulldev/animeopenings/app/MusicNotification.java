package gq.nulldev.animeopenings.app;

/**
 * Project: AnimeOpenings
 * Created: 19/11/15
 * Author: nulldev
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

/**
 * Music controls!
 */
public class MusicNotification {
    Context context;
    NotificationManager notificationManager;
    private RemoteViews remoteView;

    public MusicNotification(Context context) {
        this.context = context;
        //Get notification manager
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle("Music Controller")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true);

        remoteView = new RemoteViews(context.getPackageName(), R.layout.notification_music);

        //set the button listeners
        setListeners(remoteView);
        builder.setContent(remoteView);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(2, builder.build());
    }

    public void setListeners(RemoteViews view){
        //listener 1
        Intent volume = new Intent(context,NotificationListenerActivity.class);
        volume.putExtra("DO", "volume");
        PendingIntent btn1 = PendingIntent.getActivity(context, 0, volume, 0);
        view.setOnClickPendingIntent(R.id.btn1, btn1);

        //listener 2
        Intent stop = new Intent(context, NotificationListenerActivity.class);
        stop.putExtra("DO", "stop");
        PendingIntent btn2 = PendingIntent.getActivity(context, 1, stop, 0);
        view.setOnClickPendingIntent(R.id.btn2, btn2);
    }

    public void cancel() {
        notificationManager.cancel(2);
    }
}
