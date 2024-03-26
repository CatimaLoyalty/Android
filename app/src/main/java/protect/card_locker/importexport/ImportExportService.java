package protect.card_locker.importexport;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import protect.card_locker.DBHelper;
import protect.card_locker.ImportExportActivity;
import protect.card_locker.ImportExportTask;
import protect.card_locker.NotificationHelper;
import protect.card_locker.R;
import protect.card_locker.async.TaskHandler;

public class ImportExportService extends Service {
    private final String TAG = "Catima";

    public static final String ACTION = "action";
    public static final String FORMAT = "format";
    public static final String PASSWORD = "password";

    public static final String ACTION_IMPORT = "import";
    public static final String ACTION_EXPORT = "export";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("CATIMA", "Started service");

        Uri uri = intent.getData();
        String action = intent.getStringExtra(ACTION);
        String format = intent.getStringExtra(FORMAT);
        String password = intent.getStringExtra(PASSWORD);

        if (action.equals(ACTION_IMPORT)) {
            Log.e("CATIMA", "Import requested");
            Notification.Builder notificationBuilder = new NotificationHelper().createNotification(this, NotificationHelper.CHANNEL_IMPORT, getString(R.string.importing), getString(R.string.importing));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            }

            startForeground(NotificationHelper.IMPORT_ID, notificationBuilder.build());
        } else {
            Log.e("CATIMA", "Export requested");
            Notification.Builder notificationBuilder = new NotificationHelper().createNotification(this, NotificationHelper.CHANNEL_EXPORT, getString(R.string.exporting), getString(R.string.exporting));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            }

            startForeground(NotificationHelper.EXPORT_ID, notificationBuilder.build());

            ImportExportResult result;

            OutputStream stream;
            try {
                stream = getContentResolver().openOutputStream(uri);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            final SQLiteDatabase database = new DBHelper(this).getWritableDatabase();

            try {
                OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                result = MultiFormatExporter.exportData(this, database, stream, DataFormat.valueOf(format), password.toCharArray());
                writer.close();
            } catch (IOException e) {
                result = new ImportExportResult(ImportExportResultType.GenericFailure, e.toString());
                Log.e(TAG, "Unable to export file", e);
            }

            Log.i(TAG, "Export result: " + result);
        }

        return START_REDELIVER_INTENT;
    }
}
