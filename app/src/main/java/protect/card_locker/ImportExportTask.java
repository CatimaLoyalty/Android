package protect.card_locker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

class ImportExportTask extends AsyncTask<Void, Void, Void>
{
    private static final String TAG = "LoyaltyCardLocker";

    private static final String TARGET_FILE = "LoyaltyCardLocker.csv";

    private Activity activity;
    private boolean doImport;
    private DataFormat format;

    private ProgressDialog progress;

    public ImportExportTask(Activity activity, boolean doImport, DataFormat format)
    {
        super();
        this.activity = activity;
        this.doImport = doImport;
        this.format = format;
    }

    private void toastWithArg(int stringId, String argument)
    {
        final String template = activity.getResources().getString(stringId);
        final String message = String.format(template, argument);

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performImport(File importFile, DBHelper db)
    {
        if(importFile.exists() == false)
        {
            toastWithArg(R.string.fileMissing, importFile.getAbsolutePath());
            return;
        }

        boolean result = false;

        try
        {
            FileInputStream fileReader = new FileInputStream(importFile);
            InputStreamReader reader = new InputStreamReader(fileReader, Charset.forName("UTF-8"));
            result = MultiFormatImporter.importData(db, reader, format);
            reader.close();
        }
        catch(IOException e)
        {
            Log.e(TAG, "Unable to import file", e);
        }

        int messageId = result ? R.string.importedFrom : R.string.importFailed;
        toastWithArg(messageId, importFile.getAbsolutePath());
    }

    private void performExport(File exportFile, DBHelper db)
    {
        boolean result = false;

        try
        {
            FileOutputStream fileWriter = new FileOutputStream(exportFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileWriter, Charset.forName("UTF-8"));
            result = MultiFormatExporter.exportData(db, writer, format);
            writer.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Unable to export file", e);
        }

        int messageId = result ? R.string.exportedTo : R.string.exportFailed;
        toastWithArg(messageId, exportFile.getAbsolutePath());
    }

    protected void onPreExecute()
    {
        progress = new ProgressDialog(activity);
        progress.setTitle(doImport ? R.string.importing : R.string.exporting);

        progress.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                ImportExportTask.this.cancel(true);
            }
        });

        progress.show();
    }

    protected Void doInBackground(Void... nothing)
    {
        final File sdcardDir = Environment.getExternalStorageDirectory();
        final File importExportFile = new File(sdcardDir, TARGET_FILE);
        final DBHelper db = new DBHelper(activity);

        if(doImport)
        {
            performImport(importExportFile, db);
        }
        else
        {
            performExport(importExportFile, db);
        }

        return null;
    }

    protected void onPostExecute(Void result)
    {
        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Complete");
    }

    protected void onCancelled()
    {
        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Cancelled");
    }
}
