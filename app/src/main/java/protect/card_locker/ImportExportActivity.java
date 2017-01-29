package protect.card_locker;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class ImportExportActivity extends AppCompatActivity
{
    private static final String TAG = "LoyaltyCardLocker";

    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;
    private static final int CHOOSE_EXPORT_FILE = 2;

    private ImportExportTask importExporter;

    private final File sdcardDir = Environment.getExternalStorageDirectory();
    private final File exportFile = new File(sdcardDir, "LoyaltyCardKeychain.csv");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_export_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // If the application does not have permissions to external
        // storage, ask for it now

        if (ContextCompat.checkSelfPermission(ImportExportActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ImportExportActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(ImportExportActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_EXTERNAL_STORAGE);
        }


        Button exportButton = (Button)findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startExport();
            }
        });


        // Check that there is an activity that can bring up a file chooser
        final Intent intentPickAction = new Intent(Intent.ACTION_PICK);
        intentPickAction.setData(Uri.parse("file://"));

        Button importFilesystem = (Button) findViewById(R.id.importOptionFilesystemButton);
        importFilesystem.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chooseFileWithIntent(intentPickAction);
            }
        });

        if(isCallable(getApplicationContext(), intentPickAction) == false)
        {
            findViewById(R.id.dividerImportFilesystem).setVisibility(View.GONE);
            findViewById(R.id.importOptionFilesystemTitle).setVisibility(View.GONE);
            findViewById(R.id.importOptionFilesystemExplanation).setVisibility(View.GONE);
            importFilesystem.setVisibility(View.GONE);
        }


        // Check that there is an application that can find content
        final Intent intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentGetContentAction.setType("*/*");

        Button importApplication = (Button) findViewById(R.id.importOptionApplicationButton);
        importApplication.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chooseFileWithIntent(intentGetContentAction);
            }
        });

        if(isCallable(getApplicationContext(), intentGetContentAction) == false)
        {
            findViewById(R.id.dividerImportApplication).setVisibility(View.GONE);
            findViewById(R.id.importOptionApplicationTitle).setVisibility(View.GONE);
            findViewById(R.id.importOptionApplicationExplanation).setVisibility(View.GONE);
            importApplication.setVisibility(View.GONE);
        }


        // This option, to import from the fixed location, should always be present

        Button importButton = (Button)findViewById(R.id.importOptionFixedButton);
        importButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startImport(exportFile);
            }
        });
    }

    private void startImport(File target)
    {
        ImportExportTask.TaskCompleteListener listener = new ImportExportTask.TaskCompleteListener()
        {
            @Override
            public void onTaskComplete(boolean success, File file)
            {
                onImportComplete(success, file);
            }
        };

        importExporter = new ImportExportTask(ImportExportActivity.this,
                true, DataFormat.CSV, target, listener);
        importExporter.execute();
    }

    private void startExport()
    {
        ImportExportTask.TaskCompleteListener listener = new ImportExportTask.TaskCompleteListener()
        {
            @Override
            public void onTaskComplete(boolean success, File file)
            {
                onExportComplete(success, file);
            }
        };

        importExporter = new ImportExportTask(ImportExportActivity.this,
                false, DataFormat.CSV, exportFile, listener);
        importExporter.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSIONS_EXTERNAL_STORAGE)
        {
            // If request is cancelled, the result arrays are empty.
            boolean success = grantResults.length > 0;

            for(int grant : grantResults)
            {
                if(grant != PackageManager.PERMISSION_GRANTED)
                {
                    success = false;
                }
            }

            if(success == false)
            {
                // External storage permission rejected, inform user that
                // import/export is prevented
                Toast.makeText(getApplicationContext(), R.string.noExternalStoragePermissionError,
                        Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected void onDestroy()
    {
        if(importExporter != null && importExporter.getStatus() != AsyncTask.Status.RUNNING)
        {
            importExporter.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onImportComplete(boolean success, File path)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(success)
        {
            builder.setTitle(R.string.importSuccessfulTitle);
        }
        else
        {
            builder.setTitle(R.string.importFailedTitle);
        }

        int messageId = success ? R.string.importedFrom : R.string.importFailed;

        final String template = getResources().getString(messageId);
        final String message = String.format(template, path.getAbsolutePath());
        builder.setMessage(message);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void onExportComplete(boolean success, final File path)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(success)
        {
            builder.setTitle(R.string.exportSuccessfulTitle);
        }
        else
        {
            builder.setTitle(R.string.exportFailedTitle);
        }

        int messageId = success ? R.string.exportedTo : R.string.exportFailed;

        final String template = getResources().getString(messageId);
        final String message = String.format(template, path.getAbsolutePath());
        builder.setMessage(message);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        if(success)
        {
            final CharSequence sendLabel = ImportExportActivity.this.getResources().getText(R.string.sendLabel);

            builder.setPositiveButton(sendLabel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Uri outputUri = Uri.fromFile(path);
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, outputUri);
                    sendIntent.setType("text/plain");

                    ImportExportActivity.this.startActivity(Intent.createChooser(sendIntent,
                            sendLabel));

                    dialog.dismiss();
                }
            });
        }

        builder.create().show();
    }

    /**
     * Determines if there is at least one activity that can perform the given intent
     */
    private boolean isCallable(Context context, final Intent intent)
    {
        PackageManager manager = context.getPackageManager();
        if(manager == null)
        {
            return false;
        }

        List<ResolveInfo> list = manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for(ResolveInfo info : list)
        {
            if(info.activityInfo.exported)
            {
                // There is one activity which is available to be called
                return true;
            }
        }

        return false;
    }

    private void chooseFileWithIntent(Intent intent)
    {
        try
        {
            startActivityForResult(intent, CHOOSE_EXPORT_FILE);
        }
        catch (ActivityNotFoundException e)
        {
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == CHOOSE_EXPORT_FILE)
        {
            String path = null;

            Uri uri = data.getData();
            if(uri != null && uri.toString().startsWith("/"))
            {
                uri = Uri.parse("file://" + uri.toString());
            }

            if(uri != null)
            {
                path = uri.getPath();
            }

            if(path != null)
            {
                Log.e(TAG, "Starting file import with: " + uri.toString());
                startImport(new File(path));
            }
            else
            {
                Log.e(TAG, "Fail to make sense of URI returned from activity: " + (uri != null ? uri.toString() : "null"));
            }
        }
        else
        {
            Log.w(TAG, "Failed onActivityResult(), result=" + resultCode);
        }
    }
}