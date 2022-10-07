package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import protect.card_locker.async.CompatCallable;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;
import protect.card_locker.importexport.ImportExportResultType;
import protect.card_locker.importexport.MultiFormatExporter;
import protect.card_locker.importexport.MultiFormatImporter;

public class ImportExportTask implements CompatCallable<ImportExportResult> {
    private static final String TAG = "Catima";

    private Activity activity;
    private Context context;
    private boolean doImport;
    private DataFormat format;
    private OutputStream outputStream;
    private InputStream inputStream;
    private char[] password;
    private TaskCompleteListener listener;

    private AlertDialog dialog;

    /**
     * Constructor which will setup a task for exporting to the given file
     */
    ImportExportTask(Activity activity, DataFormat format, OutputStream output, char[] password,
                     TaskCompleteListener listener, Context context) {
        super();
        this.activity = activity;
        this.context = context;
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
                     TaskCompleteListener listener, Context context) {
        super();
        this.activity = activity;
        this.context = context;
        this.doImport = true;
        this.format = format;
        this.inputStream = input;
        this.password = password;
        this.listener = listener;
    }

    private ImportExportResult performImport(Context context, InputStream stream, SQLiteDatabase database, char[] password) {
        ImportExportResult importResult = MultiFormatImporter.importData(context, database, stream, format, password);
        Log.i(TAG, "Import result: " + importResult);

        return importResult;
    }

    private ImportExportResult performExport(Context context, OutputStream stream, SQLiteDatabase database, char[] password) {
        ImportExportResult result;

        try {
            OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            result = MultiFormatExporter.exportData(context, database, stream, format, password);
            writer.close();
        } catch (IOException e) {
            result = new ImportExportResult(ImportExportResultType.GenericFailure, e.toString());
            Log.e(TAG, "Unable to export file", e);
        }

        Log.i(TAG, "Export result: " + result);

        return result;
    }

    public void onPreExecute() {
        int llPadding = 30;
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(context);
        tvText.setText(doImport ? R.string.importing : R.string.exporting);

        int textColor;
        if (Utils.isDarkModeEnabled(context)) {
            textColor = Color.WHITE;
        } else {
            textColor = Color.BLACK;
        }
        tvText.setTextColor(textColor);
        tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
        builder.setCancelable(true);
        builder.setView(ll);

        dialog = builder.create();
        dialog.show();
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
        listener.onTaskComplete((ImportExportResult) castResult, format);

        dialog.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Complete");
    }

    protected void onCancelled() {
        dialog.dismiss();
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
