package protect.card_locker;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import androidx.core.app.NotificationCompat;
import protect.card_locker.async.CompatCallable;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;
import protect.card_locker.importexport.MultiFormatExporter;
import protect.card_locker.importexport.MultiFormatImporter;

public class ImportExportTask implements CompatCallable<ImportExportResult> {
    private static final String TAG = "Catima";

    private final Activity activity;
    private final boolean doImport;
    private final DataFormat format;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final char[] password;
    private final TaskCompleteListener listener;

    /**
     * Constructor which will setup a task for exporting to the given file
     */
    ImportExportTask(Activity activity, DataFormat format, OutputStream output, char[] password,
                     TaskCompleteListener listener) {
        super();
        this.activity = activity;
        this.doImport = false;
        this.format = format;
        this.outputStream = output;
        this.password = password;
        this.listener = listener;
    }

    /**
     * Constructor which will setup a task for importing from the given InputStream.
     */
    ImportExportTask(Activity activity, DataFormat format, InputStream input, char[] password,
                     TaskCompleteListener listener) {
        super();
        this.activity = activity;
        this.doImport = true;
        this.format = format;
        this.inputStream = input;
        this.password = password;
        this.listener = listener;
    }

    private ImportExportResult performImport(Context context, InputStream stream, SQLiteDatabase database, char[] password) {
        ImportExportResult result = MultiFormatImporter.importData(context, database, stream, format, password);

        Log.i(TAG, "Import result: " + result.name());

        return result;
    }

    private ImportExportResult performExport(Context context, OutputStream stream, SQLiteDatabase database, char[] password) {
        ImportExportResult result = ImportExportResult.GenericFailure;

        try {
            OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            result = MultiFormatExporter.exportData(context, database, stream, format, password);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to export file", e);
        }

        Log.i(TAG, "Export result: " + result);

        return result;
    }

    protected ImportExportResult doInBackground(Void... nothing) {
        final SQLiteDatabase database = new DBHelper(activity).getWritableDatabase();
        ImportExportResult result;

        if (doImport) {
            result = performImport(activity.getApplicationContext(), inputStream, database, password);
        } else {
            result = performExport(activity.getApplicationContext(), outputStream, database, password);
        }

        database.close();

        return result;
    }

    public void onPostExecute(Object castResult) {
        ImportExportResult result = (ImportExportResult) castResult;

        listener.onTaskComplete(result, format);

        Log.i(TAG, (doImport ? "Import" : "Export") + " Complete");
    }

    @Override
    public void onPreExecute() {
    }

    protected void stop() {
        // Whelp
    }

    @Override
    public ImportExportResult call() {
        return doInBackground();
    }

    interface TaskCompleteListener {
        void onTaskComplete(ImportExportResult result, DataFormat format);
    }

}
