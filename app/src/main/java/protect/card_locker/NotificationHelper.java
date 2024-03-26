package protect.card_locker;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

public class NotificationHelper {

    // Do not change these IDs!
    public static final String CHANNEL_IMPORT = "import";
    public static final String CHANNEL_EXPORT = "export";

    public static final int IMPORT_ID = 100;
    public static final int EXPORT_ID = 101;

    public Notification.Builder createNotification(Context context, String channel, String title, String message) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(channel, getChannelName(channel), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        return new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(message);
    }

    private String getChannelName(String channel) {
        switch(channel) {
            case CHANNEL_IMPORT:
                return "Import";
            case CHANNEL_EXPORT:
                return "Export";
            default:
                throw new IllegalArgumentException("Unknown notification channel");
        }
    }
}
