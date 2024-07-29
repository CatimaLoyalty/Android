package protect.card_locker;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class BarcodeFromUrlDownloader extends AppCompatActivity {

    private static final String TAG = "Catima";

    DownloadManager downloadmanager;
    Context context;
    long downloadId;

    public BarcodeFromUrlDownloader(Context context){
        this.context = context;
        this.downloadmanager =  (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void download(@NonNull Uri uri){

        downloadId = this.start(uri);

        if (Objects.nonNull(downloadId)){

            this.waiting();
        }
    }

    public Long start(@NonNull Uri uri){

        try {
            File outputDir = this.context.getCacheDir(); // context being the Activity pointer
            File outputFile = File.createTempFile("prefix", ".extension", outputDir);

            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)// Visibility of the download Notification
                    .setDestinationUri(Uri.fromFile(outputFile))// Uri of the destination file
                    .setTitle(outputFile.getName())// Title of the Download Notification
                    .setDescription("Downloading")// Description of the Download Notification
                    //.setRequiresCharging(false)// Set if charging is required to begin the download
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true);// Set if download is allowed on roaming network

            long downloadReference = this.downloadmanager.enqueue(request);
            Log.i(TAG, "downloading file " + String.valueOf(downloadReference));

            return downloadReference;

        } catch (IOException e){
            Log.e(TAG, "Unable to create temporary file");
        }

        return null;
    }

    private void waiting(){

        // using query method
        boolean finishDownload = false;
        int progress;
        while (!finishDownload) {
            Cursor cursor = this.downloadmanager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_FAILED: {
                        finishDownload = true;
                        break;
                    }
                    case DownloadManager.STATUS_PAUSED:
                        break;
                    case DownloadManager.STATUS_PENDING:
                        break;
                    case DownloadManager.STATUS_RUNNING: {
                        final long total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (total >= 0) {
                            final long downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            progress = (int) ((downloaded * 100L) / total);
                            // if you use downloadmanger in async task, here you can use like this to display progress.
                            // Don't forget to do the division in long to get more digits rather than double.
                            //  publishProgress((int) ((downloaded * 100L) / total));
                        }
                        break;
                    }
                    case DownloadManager.STATUS_SUCCESSFUL: {
                        progress = 100;
                        // if you use aysnc task
                        // publishProgress(100);
                        finishDownload = true;
                        break;
                    }
                }
            }
        }
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
                Toast.makeText(context, "Download Completed", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
