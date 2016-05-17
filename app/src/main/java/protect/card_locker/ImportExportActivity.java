package protect.card_locker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ImportExportActivity extends AppCompatActivity
{
    private static final int PERMISSIONS_EXTERNAL_STORAGE_IMPORT = 1;
    private static final int PERMISSIONS_EXTERNAL_STORAGE_EXPORT = 2;

    ImportExportTask importExporter;

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

        Button importButton = (Button)findViewById(R.id.importButton);
        importButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ContextCompat.checkSelfPermission(ImportExportActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    startImport();
                }
                else
                {
                    ActivityCompat.requestPermissions(ImportExportActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSIONS_EXTERNAL_STORAGE_IMPORT);
                }
            }
        });

        Button exportButton = (Button)findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ContextCompat.checkSelfPermission(ImportExportActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    startExport();
                }
                else
                {
                    ActivityCompat.requestPermissions(ImportExportActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_EXTERNAL_STORAGE_EXPORT);
                }
            }
        });
    }

    private void startImport()
    {
        importExporter = new ImportExportTask(ImportExportActivity.this,
                true, DataFormat.CSV);
        importExporter.execute();
    }

    private void startExport()
    {
        importExporter = new ImportExportTask(ImportExportActivity.this,
                false, DataFormat.CSV);
        importExporter.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSIONS_EXTERNAL_STORAGE_IMPORT ||
           requestCode == PERMISSIONS_EXTERNAL_STORAGE_EXPORT)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // permission was granted.
                if(requestCode == PERMISSIONS_EXTERNAL_STORAGE_IMPORT)
                {
                    startImport();
                }
                else
                {
                    startExport();
                }
            }
            else
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
}