package protect.card_locker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import protect.card_locker.async.CompatCallable;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;
import protect.card_locker.importexport.MultiFormatExporter;
import protect.card_locker.importexport.MultiFormatImporter;

public class ImportExportTask implements CompatCallable<ImportExportResult> {
    private static final String TAG = "Catima";

    private Activity activity;
    private boolean doImport;
    private DataFormat format;
    private OutputStream outputStream;
    private InputStream inputStream;
    private char[] password;
    private TaskCompleteListener listener;

    private ProgressDialog progress;

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

    private ImportExportResult performImport(Context context, InputStream stream, DBHelper db, char[] password) {
        ImportExportResult importResult = MultiFormatImporter.importData(context, db, stream, format, password);

        Log.i(TAG, "Import result: " + importResult.name());

        return importResult;
    }

    private ImportExportResult performExport(Context context, OutputStream stream, DBHelper db, char[] password) {
        ImportExportResult result = ImportExportResult.GenericFailure;

        try {
            OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            result = MultiFormatExporter.exportData(context, db, stream, format, password);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to export file", e);
        }

        Log.i(TAG, "Export result: " + result);

        return result;
    }

    public void onPreExecute() {
        progress = new ProgressDialog(activity);
        progress.setTitle(doImport ? R.string.importing : R.string.exporting);

        progress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ImportExportTask.this.stop();
            }
        });

        progress.show();
    }

    protected ImportExportResult doInBackground(Void... nothing) {
        final DBHelper db = new DBHelper(activity);
        ImportExportResult result;

        if (doImport) {
            result = performImport(activity.getApplicationContext(), inputStream, db, password);
        } else {
            result = performExport(activity.getApplicationContext(), outputStream, db, password);
        }

        return result;
    }

    public void onPostExecute(Object castResult) {
        listener.onTaskComplete((ImportExportResult) castResult, format);

        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Complete");
    }

    protected void onCancelled() {
        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Cancelled");
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
