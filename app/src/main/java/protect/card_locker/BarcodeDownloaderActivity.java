package protect.card_locker;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BarcodeDownloaderActivity extends AppCompatActivity {

    private static final String TAG = "Catima";

    private DownloadManager downloadmanager;
    private long downloadId;
    private File downloadedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.prepareDownloadFileAndDirectory();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_barcode_downloader);
       /* ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/

        Uri intentDataUri = getIntent().getData();
        if (Objects.nonNull(intentDataUri)) {
            this.downloadmanager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            this.startDownload(intentDataUri);
        }
    }

    private void prepareDownloadFileAndDirectory() {

        String uniqueId = UUID.randomUUID().toString();

        File outputDir = new File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOWNLOADS);
        if (!outputDir.exists()) {
            try {
                outputDir.mkdirs();
            } catch (Exception e) {
                Log.e(TAG, "cannot create directory " + outputDir.getAbsolutePath(), e);
            }
        }
        downloadedFile = new File(outputDir + "/catima_dl_" + uniqueId + ".tmp");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        try {
            if (downloadedFile.exists()) {
                downloadedFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "error when deleting file " + downloadedFile.getAbsoluteFile(), e);
        }
    }

    private void startDownload(@NonNull Uri uri) {

        try {
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)// Visibility of the download Notification
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadedFile.getName())
                    //.setTitle(String.valueOf(downloadId))
                    //outputFile.getName())// Title of the Download Notification
                    .setDescription("Downloading")// Description of the Download Notification
                    //.setRequiresCharging(false)// Set if charging is required to begin the download
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true);// Set if download is allowed on roaming network

            this.downloadId = this.downloadmanager.enqueue(request);

            TextView nomView = findViewById(R.id.downloadMessage);
            nomView.setText("Downloading\n" + uri.toString());

            Log.i(TAG, "downloading file " + String.valueOf(downloadId));

        } catch (Exception e) {
            Log.e(TAG, "Unable to create temporary file");
        }
    }

    final private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadmanager.query(query);
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {

                        int colIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                        if (colIdx >= 0 && Objects.nonNull(cursor.getString(colIdx))) {

                            String fileLocalUri = cursor.getString(colIdx);
                            File mfile = new File(Uri.parse(fileLocalUri).getPath());

                            if (mfile.exists()) {
                                try {
                                    colIdx = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);

                                    String contentType = colIdx >= 0 ? cursor.getString(colIdx) : null;

                                    if (Objects.isNull(contentType) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        contentType = Files.probeContentType(mfile.toPath());
                                    }

                                    TextView nomView = findViewById(R.id.downloadMessage);
                                    nomView.setText("Download is succesfull.\nParsing file to find barcode in.");

                                    List<BarcodeValues> barcodes = new ArrayList<>();

                                    if (contentType.equals("application/pdf")) {
                                        barcodes = Utils.retrieveBarcodesFromPdf(context, Uri.parse(fileLocalUri));
                                    }

                                    Log.i(TAG, "found barcodes " + barcodes.size());

                                    Intent downloadResult = new Intent();
                                    Bundle bundle = new Bundle();
                                    bundle.putParcelableArrayList("barcodes", (ArrayList<BarcodeValues>) barcodes);
                                    downloadResult.putExtras(bundle);
                                    BarcodeDownloaderActivity.this.setResult(RESULT_OK, downloadResult);
                                    finish();

                                } catch (IOException e) {
                                    Log.e(TAG, "unable to probe content type");
                                }
                            }
                        }
                    }
                }


            }


        }

    };
}