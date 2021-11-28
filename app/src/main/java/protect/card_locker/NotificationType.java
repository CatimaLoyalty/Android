package protect.card_locker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationType {
    private final static String NOTIFICATION_IMPORT_EXPORT = "NOTIFICATION_IMPORT_EXPORT";

    public static String getImportExportChannel(Context context) {
        createNotificationChannel(context, NOTIFICATION_IMPORT_EXPORT, context.getString(R.string.importExport), context.getString(R.string.importExportDescription), NotificationManager.IMPORTANCE_HIGH);

        return NOTIFICATION_IMPORT_EXPORT;
    }

    private static void createNotificationChannel(Context context, String channelId, CharSequence name, String description, int importance) {
        // Create the NotificationType, but only on API 26+ because
        // the NotificationType class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
