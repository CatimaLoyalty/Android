package protect.card_locker.importexport;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import protect.card_locker.DBHelper;
import protect.card_locker.NotificationHelper;
import protect.card_locker.R;

public class ImportExportWorker extends Worker {
    private final String TAG = "Catima";

    public static final String INPUT_URI = "uri";
    public static final String INPUT_ACTION = "action";
    public static final String INPUT_FORMAT = "format";
    public static final String INPUT_PASSWORD = "password";

    public static final String ACTION_IMPORT = "import";
    public static final String ACTION_EXPORT = "export";

    public static final String OUTPUT_ERROR_REASON = "errorReason";
    public static final String ERROR_GENERIC = "errorTypeGeneric";
    public static final String ERROR_PASSWORD_REQUIRED = "errorTypePasswordRequired";
    public static final String OUTPUT_ERROR_DETAILS = "errorDetails";

    public ImportExportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e("CATIMA", "Started import/export worker");

        Context context = getApplicationContext();

        Data inputData = getInputData();

        String uriString = inputData.getString(INPUT_URI);
        String action = inputData.getString(INPUT_ACTION);
        String format = inputData.getString(INPUT_FORMAT);
        String password = inputData.getString(INPUT_PASSWORD);

        if (action.equals(ACTION_IMPORT)) {
            Log.e("CATIMA", "Import requested");

            setForegroundAsync(createForegroundInfo(NotificationHelper.CHANNEL_IMPORT, NotificationHelper.IMPORT_PROGRESS_ID, R.string.importing));

            ImportExportResult result;

            InputStream stream;
            try {
                stream = context.getContentResolver().openInputStream(Uri.parse(uriString));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            final SQLiteDatabase database = new DBHelper(context).getWritableDatabase();

            try {
                InputStreamReader writer = new InputStreamReader(stream, StandardCharsets.UTF_8);
                result = MultiFormatImporter.importData(context, database, stream, DataFormat.valueOf(format), password.toCharArray());
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to import file", e);
                NotificationHelper.sendNotification(context, NotificationHelper.IMPORT_ID, NotificationHelper.createNotificationBuilder(context, NotificationHelper.CHANNEL_IMPORT, R.drawable.ic_import_export_white_24dp, context.getString(R.string.importFailedTitle), e.getLocalizedMessage()).build());

                Data failureData = new Data.Builder()
                        .putString(OUTPUT_ERROR_REASON, ERROR_GENERIC)
                        .putString(OUTPUT_ERROR_DETAILS, e.getLocalizedMessage())
                        .putString(INPUT_URI, uriString)
                        .putString(INPUT_ACTION, action)
                        .putString(INPUT_FORMAT, format)
                        .putString(INPUT_PASSWORD, password)
                        .build();

                return Result.failure(failureData);
            }

            Log.i(TAG, "Import result: " + result);

            if (result.resultType() == ImportExportResultType.Success) {
                NotificationHelper.sendNotification(context, NotificationHelper.IMPORT_ID, NotificationHelper.createNotificationBuilder(context, NotificationHelper.CHANNEL_IMPORT, R.drawable.ic_import_export_white_24dp, context.getString(R.string.importSuccessfulTitle), context.getString(R.string.importSuccessful)).build());

                return Result.success();
            } else if (result.resultType() == ImportExportResultType.BadPassword) {
                Log.e(TAG, "Needs password, unhandled for now");
                NotificationHelper.sendNotification(context, NotificationHelper.IMPORT_ID, NotificationHelper.createNotificationBuilder(context, NotificationHelper.CHANNEL_IMPORT, R.drawable.ic_import_export_white_24dp, context.getString(R.string.importing), context.getString(R.string.passwordRequired)).build());

                Data failureData = new Data.Builder()
                        .putString(OUTPUT_ERROR_REASON, ERROR_PASSWORD_REQUIRED)
                        .putString(OUTPUT_ERROR_DETAILS, result.developerDetails())
                        .putString(INPUT_URI, uriString)
                        .putString(INPUT_ACTION, action)
                        .putString(INPUT_FORMAT, format)
                        .putString(INPUT_PASSWORD, password)
                        .build();

                return Result.failure(failureData);
            } else {
                NotificationHelper.sendNotification(context, NotificationHelper.IMPORT_ID, NotificationHelper.createNotificationBuilder(context, NotificationHelper.CHANNEL_IMPORT, R.drawable.ic_import_export_white_24dp, context.getString(R.string.importFailedTitle), context.getString(R.string.importFailed)).build());

                Data failureData = new Data.Builder()
                        .putString(OUTPUT_ERROR_REASON, ERROR_GENERIC)
                        .putString(OUTPUT_ERROR_DETAILS, result.developerDetails())
                        .putString(INPUT_URI, uriString)
                        .putString(INPUT_ACTION, action)
                        .putString(INPUT_FORMAT, format)
                        .putString(INPUT_PASSWORD, password)
                        .build();

                return Result.failure(failureData);
            }
        } else {
            Log.e("CATIMA", "Export requested");

            setForegroundAsync(createForegroundInfo(NotificationHelper.CHANNEL_EXPORT, NotificationHelper.EXPORT_PROGRESS_ID, R.string.exporting));

            ImportExportResult result;

            OutputStream stream;
            try {
                stream = context.getContentResolver().openOutputStream(Uri.parse(uriString));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            final SQLiteDatabase database = new DBHelper(context).getReadableDatabase();

            try {
                OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                result = MultiFormatExporter.exportData(context, database, stream, DataFormat.valueOf(format), password.toCharArray());
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to export file", e);
                NotificationHelper.sendNotification(context, NotificationHelper.EXPORT_ID, NotificationHelper.createNotificationBuilder(context, NotificationHelper.CHANNEL_EXPORT, R.drawable.ic_import_export_white_24dp, context.getString(R.string.exportFailedTitle), e.getLocalizedMessage()).build());

                Data failureData = new Data.Builder()
                        .putString(OUTPUT_ERROR_REASON, ERROR_GENERIC)
                        .putString(OUTPUT_ERROR_DETAILS, e.getLocalizedMessage())
                        .putString(INPUT_URI, uriString)
                        .putString(INPUT_ACTION, action)
                        .putString(INPUT_FORMAT, format)
                        .putString(INPUT_PASSWORD, password)
                        .build();

                return Result.failure(failureData);
            }

            Log.i(TAG, "Export result: " + result);

            if (result.resultType() == ImportExportResultType.Success) {
                NotificationHelper.sendNotification(context, NotificationHelper.EXPORT_ID, NotificationHelper.createNotificationBuilder(context, NotificationHelper.CHANNEL_EXPORT, R.drawable.ic_import_export_white_24dp, context.getString(R.string.exportSuccessfulTitle), context.getString(R.string.exportSuccessful)).build());

                return Result.success();
            } else {
                NotificationHelper.sendNotification(context, NotificationHelper.EXPORT_ID, NotificationHelper.createNotificationBuilder(context, NotificationHelper.CHANNEL_EXPORT, R.drawable.ic_import_export_white_24dp, context.getString(R.string.exportFailedTitle), context.getString(R.string.exportFailed)).build());

                Data failureData = new Data.Builder()
                        .putString(OUTPUT_ERROR_REASON, ERROR_GENERIC)
                        .putString(OUTPUT_ERROR_DETAILS, result.developerDetails())
                        .putString(INPUT_URI, uriString)
                        .putString(INPUT_ACTION, action)
                        .putString(INPUT_FORMAT, format)
                        .putString(INPUT_PASSWORD, password)
                        .build();

                return Result.failure(failureData);
            }
        }
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String channel, int notificationId, int title) {
        Context context = getApplicationContext();

        String cancel = context.getString(R.string.cancel);
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());

        Notification.Builder notificationBuilder = NotificationHelper.createNotificationBuilder(context, channel, R.drawable.ic_import_export_white_24dp, context.getString(title), null);

        Notification notification = notificationBuilder
                .setOngoing(true)
                // Add the cancel action to the notification which can
                // be used to cancel the worker
                .addAction(android.R.drawable.ic_delete, cancel, intent)
                .build();

        return new ForegroundInfo(notificationId, notification);
    }
}
