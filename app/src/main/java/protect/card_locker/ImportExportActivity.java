package protect.card_locker;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class ImportExportActivity extends AppCompatActivity
{

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
                importExporter = new ImportExportTask(ImportExportActivity.this,
                        true, DataFormat.CSV);
                importExporter.execute();
            }
        });

        Button exportButton = (Button)findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                importExporter = new ImportExportTask(ImportExportActivity.this,
                        false, DataFormat.CSV);
                importExporter.execute();
            }
        });
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