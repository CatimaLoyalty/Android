package protect.card_locker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Notification extends BroadcastReceiver {

    Context context;
    public static final String CHANNEL_ID = "101";
    String cardName;

    public Notification() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotificationChannel (){

        String channelName = context.getString(R.string.reminder);
        String des = context.getString(R.string.channelDescription);
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,channelName,importance);
        channel.setDescription(des);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

    }

    public void showNotification() {

        Intent intent = new Intent(context,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,101,intent,0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(context.getString(R.string.notificationContentTitle))
                .setContentText(context.getString(R.string.card_text)+ cardName+context.getString(R.string.is_about_to_expire))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat managerCompat =  NotificationManagerCompat.from(context);
        managerCompat.notify(Integer.parseInt(CHANNEL_ID),builder.build());

    }


    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;
        cardName = intent.getStringExtra("cardName");

        Log.i("Notifications ", "onReceive: broadcast received ");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("Notifications ", "onReceive: notificationChannelCreated ");
            // calls this method, only when minimum API level is 26.
            createNotificationChannel();
        }

        showNotification();
    }
}
