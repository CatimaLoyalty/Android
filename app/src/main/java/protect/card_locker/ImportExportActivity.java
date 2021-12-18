package protect.card_locker;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;

public class ImportExportActivity extends CatimaAppCompatActivity {
    private static final String TAG = "Catima";

    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;

    private ImportExportTask importExporter;

    private String importAlertTitle;
    private String importAlertMessage;
    private DataFormat importDataFormat;
    private String exportPassword;

    private ActivityResultLauncher<Intent> fileCreateLauncher;
    private ActivityResultLauncher<String> fileOpenLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    final private TaskHandler mTasks = new TaskHandler();

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    private static final int NOTIFICATION_IMPORT = 1;
    private static final int NOTIFICATION_EXPORT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.importExport);
        setContentView(R.layout.import_export_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // If the application does not have permissions to external
        // storage, ask for it now

        if (ContextCompat.checkSelfPermission(ImportExportActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ImportExportActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ImportExportActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_EXTERNAL_STORAGE);
        }

        // would use ActivityResultContracts.CreateDocument() but mime type cannot be set
        fileCreateLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent intent = result.getData();
            if (intent == null) {
                Log.e(TAG, "Activity returned NULL data");
                return;
            }
            Uri uri = intent.getData();
            if (uri == null) {
                Log.e(TAG, "Activity returned NULL uri");
                return;
            }
            try {
                OutputStream writer = getContentResolver().openOutputStream(uri);
                Log.e(TAG, "Starting file export with: " + result.toString());
                startExport(writer, uri, exportPassword != null ? exportPassword.toCharArray() : null, true);
            } catch (IOException e) {
                Log.e(TAG, "Failed to export file: " + result.toString(), e);
                onExportComplete(ImportExportResult.GenericFailure, uri);
            }

        });
        fileOpenLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
            if (result == null) {
                Log.e(TAG, "Activity returned NULL data");
                return;
            }
            openFileForImport(result, null);
        });
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent intent = result.getData();
            if (intent == null) {
                Log.e(TAG, "Activity returned NULL data");
                return;
            }
            Uri uri = intent.getData();
            if (uri == null) {
                Log.e(TAG, "Activity returned NULL uri");
                return;
            }
            openFileForImport(intent.getData(), null);
        });

        // Check that there is a file manager available
        final Intent intentCreateDocumentAction = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intentCreateDocumentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentCreateDocumentAction.setType("application/zip");
        intentCreateDocumentAction.putExtra(Intent.EXTRA_TITLE, "catima.zip");

        Button exportButton = findViewById(R.id.exportButton);
        exportButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ImportExportActivity.this);
            builder.setTitle(R.string.exportPassword);

            FrameLayout container = new FrameLayout(ImportExportActivity.this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = 50;
            params.rightMargin = 50;

            final EditText input = new EditText(ImportExportActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setLayoutParams(params);
            input.setHint(R.string.exportPasswordHint);

            container.addView(input);
            builder.setView(container);
            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                exportPassword = input.getText().toString();
                try {
                    fileCreateLauncher.launch(intentCreateDocumentAction);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), R.string.failedOpeningFileManager, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "No activity found to handle intent", e);
                }
            });
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.show();

        });

        // Check that there is a file manager available
        Button importFilesystem = findViewById(R.id.importOptionFilesystemButton);
        importFilesystem.setOnClickListener(v -> chooseImportType(false));

        // Check that there is an app that data can be imported from
        Button importApplication = findViewById(R.id.importOptionApplicationButton);
        importApplication.setOnClickListener(v -> chooseImportType(true));
    }

    private void openFileForImport(Uri uri, char[] password) {
        try {
            InputStream reader = getContentResolver().openInputStream(uri);
            Log.e(TAG, "Starting file import with: " + uri.toString());
            startImport(reader, uri, importDataFormat, password, true);
        } catch (IOException e) {
            Log.e(TAG, "Failed to import file: " + uri.toString(), e);
            onImportComplete(ImportExportResult.GenericFailure, uri, importDataFormat);
        }
    }

    private void chooseImportType(boolean choosePicker) {
        List<CharSequence> betaImportOptions = new ArrayList<>();
        betaImportOptions.add("Fidme");
        betaImportOptions.add("Stocard");
        List<CharSequence> importOptions = new ArrayList<>();

        for (String importOption : getResources().getStringArray(R.array.import_types_array)) {
            if (betaImportOptions.contains(importOption)) {
                importOption = importOption + " (BETA)";
            }

            importOptions.add(importOption);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.chooseImportType)
                .setItems(importOptions.toArray(new CharSequence[importOptions.size()]), (dialog, which) -> {
                    switch (which) {
                        // Catima
                        case 0:
                            importAlertTitle = getString(R.string.importCatima);
                            importAlertMessage = getString(R.string.importCatimaMessage);
                            importDataFormat = DataFormat.Catima;
                            break;
                        // Fidme
                        case 1:
                            importAlertTitle = getString(R.string.importFidme);
                            importAlertMessage = getString(R.string.importFidmeMessage);
                            importDataFormat = DataFormat.Fidme;
                            break;
                        // Loyalty Card Keychain
                        case 2:
                            importAlertTitle = getString(R.string.importLoyaltyCardKeychain);
                            importAlertMessage = getString(R.string.importLoyaltyCardKeychainMessage);
                            importDataFormat = DataFormat.Catima;
                            break;
                        // Stocard
                        case 3:
                            importAlertTitle = getString(R.string.importStocard);
                            importAlertMessage = getString(R.string.importStocardMessage);
                            importDataFormat = DataFormat.Stocard;
                            break;
                        // Voucher Vault
                        case 4:
                            importAlertTitle = getString(R.string.importVoucherVault);
                            importAlertMessage = getString(R.string.importVoucherVaultMessage);
                            importDataFormat = DataFormat.VoucherVault;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown DataFormat");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle(importAlertTitle)
                            .setMessage(importAlertMessage)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        if (choosePicker) {
                                            final Intent intentPickAction = new Intent(Intent.ACTION_PICK);
                                            filePickerLauncher.launch(intentPickAction);
                                        } else {
                                            fileOpenLauncher.launch("*/*");
                                        }
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(getApplicationContext(), R.string.failedOpeningFileManager, Toast.LENGTH_LONG).show();
                                        Log.e(TAG, "No activity found to handle intent", e);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                });
        builder.show();
    }

    private void startProgressNotification(boolean importing) {
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this, NotificationType.getImportExportChannel(this));
        mBuilder.setContentTitle(getString(importing ? R.string.importing : R.string.exporting))
                .setContentText(null)
                .setSmallIcon(R.drawable.ic_import_export_white_24dp)
                .setColor(getThemeColor())
                .setProgress(0, 0, true);
        mNotifyManager.notify(importing ? NOTIFICATION_IMPORT : NOTIFICATION_EXPORT, mBuilder.build());
    }

    private void endProgressNotification(boolean importing, ImportExportResult result, PendingIntent sendIntent) {
        String notificationTitle;
        String notificationMessage;

        if (result.equals(ImportExportResult.Success)) {
            notificationTitle = getString(importing ? R.string.importSuccessfulTitle : R.string.exportSuccessfulTitle);
            notificationMessage = getString(importing ? R.string.importSuccessful : R.string.exportSuccessful);
        } else {
            int reason = R.string.unknown_failure;
            if (result.equals(ImportExportResult.BadPassword)) {
                reason = R.string.incorrect_password;
            }

            notificationTitle = getString(importing ? R.string.importFailedTitle : R.string.exportFailedTitle);
            notificationMessage = String.format(getString(importing ? R.string.importFailed : R.string.exportFailed), getString(reason));
        }

        mBuilder.setContentTitle(notificationTitle)
                .setContentText(notificationMessage)
                .setProgress(0,0, false);

        if (sendIntent != null) {
            mBuilder.addAction(R.drawable.ic_share, getString(R.string.sendLabel), sendIntent);
        }

        mNotifyManager.notify(importing ? NOTIFICATION_IMPORT : NOTIFICATION_EXPORT, mBuilder.build());
    }

    private void startImport(final InputStream target, final Uri targetUri, final DataFormat dataFormat, final char[] password, final boolean closeWhenDone) {
        mTasks.flushTaskList(TaskHandler.TYPE.IMPORT, true, false, false);
        ImportExportTask.TaskCompleteListener listener = (result, dataFormat1) -> {
            onImportComplete(result, targetUri, dataFormat1);
            if (closeWhenDone) {
                try {
                    target.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        };

        startProgressNotification(true);
        importExporter = new ImportExportTask(ImportExportActivity.this,
                dataFormat, target, password, listener);
        mTasks.executeTask(TaskHandler.TYPE.IMPORT, importExporter);
    }

    private void startExport(final OutputStream target, final Uri targetUri, char[] password, final boolean closeWhenDone) {
        mTasks.flushTaskList(TaskHandler.TYPE.EXPORT, true, false, false);
        ImportExportTask.TaskCompleteListener listener = (result, dataFormat) -> {
            onExportComplete(result, targetUri);
            if (closeWhenDone) {
                try {
                    target.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        };

        startProgressNotification(false);
        importExporter = new ImportExportTask(ImportExportActivity.this,
                DataFormat.Catima, target, password, listener);
        mTasks.executeTask(TaskHandler.TYPE.EXPORT, importExporter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            boolean success = grantResults.length > 0;

            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    success = false;
                }
            }

            if (!success) {
                // External storage permission rejected, inform user that
                // import/export is prevented
                Toast.makeText(getApplicationContext(), R.string.noExternalStoragePermissionError,
                        Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected void onDestroy() {
        mTasks.flushTaskList(TaskHandler.TYPE.IMPORT, true, false, false);
        mTasks.flushTaskList(TaskHandler.TYPE.EXPORT, true, false, false);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void retryWithPassword(DataFormat dataFormat, Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.passwordRequired);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            openFileForImport(uri, input.getText().toString().toCharArray());
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());

        builder.show();
    }

    private void onImportComplete(ImportExportResult result, Uri path, DataFormat dataFormat) {
        endProgressNotification(true, result, null);

        if (result == ImportExportResult.BadPassword) {
            retryWithPassword(dataFormat, path);
        }
    }

    private void onExportComplete(ImportExportResult result, final Uri path) {
        PendingIntent pendingIntent = null;

        if (result == ImportExportResult.Success) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, path);
            sendIntent.setType("text/csv");

            // set flag to give temporary permission to external app to use the FileProvider
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_EXPORT, sendIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        endProgressNotification(false, result, pendingIntent);
    }
}