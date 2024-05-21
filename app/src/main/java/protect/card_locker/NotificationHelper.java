package protect.card_locker;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NotificationHelper {

    // Do not change these IDs!
    public static final String CHANNEL_IMPORT = "import";

    public static final String CHANNEL_EXPORT = "export";

    public static final int IMPORT_ID = 100;
    public static final int IMPORT_PROGRESS_ID = 101;
    public static final int EXPORT_ID = 103;
    public static final int EXPORT_PROGRESS_ID = 104;


    public static Notification.Builder createNotificationBuilder(@NonNull Context context, @NonNull String channel, @NonNull int icon, @NonNull String title, @Nullable String message) {
        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(icon)
                .setTicker(title)
                .setContentTitle(title);

        if (message != null) {
            notificationBuilder.setContentText(message);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(channel, getChannelName(channel), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);

            notificationBuilder.setChannelId(channel);
        }

        return notificationBuilder;
    }

    public static void sendNotification(@NonNull Context context, @NonNull int notificationId, @NonNull Notification notification) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, notification);
    }

    private static String getChannelName(@NonNull String channel) {
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
