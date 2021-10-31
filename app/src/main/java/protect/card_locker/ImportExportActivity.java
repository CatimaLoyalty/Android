package protect.card_locker;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.databinding.ImportExportActivityBinding;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;

public class ImportExportActivity extends CatimaAppCompatActivity {
    private ImportExportActivityBinding binding;
    private static final String TAG = "Catima";

    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;
    private static final int CHOOSE_EXPORT_LOCATION = 2;
    private static final int IMPORT = 3;

    private ImportExportTask importExporter;

    private String importAlertTitle;
    private String importAlertMessage;
    private DataFormat importDataFormat;
    private String exportPassword;

    final private TaskHandler mTasks = new TaskHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ImportExportActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.importExport);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
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

        // Check that there is a file manager available
        final Intent intentCreateDocumentAction = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intentCreateDocumentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentCreateDocumentAction.setType("application/zip");
        intentCreateDocumentAction.putExtra(Intent.EXTRA_TITLE, "catima.zip");

        Button exportButton = binding.exportButton;
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    chooseFileWithIntent(intentCreateDocumentAction, CHOOSE_EXPORT_LOCATION);
                });
                builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
                builder.show();

            }
        });

        // Check that there is a file manager available
        final Intent intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentGetContentAction.setType("*/*");

        Button importFilesystem = binding.importOptionFilesystemButton;
        importFilesystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImportType(intentGetContentAction);
            }
        });

        // Check that there is an app that data can be imported from
        final Intent intentPickAction = new Intent(Intent.ACTION_PICK);

        Button importApplication = binding.importOptionApplicationButton;
        importApplication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImportType(intentPickAction);
            }
        });
    }

    private void chooseImportType(Intent baseIntent) {
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
                                    chooseFileWithIntent(baseIntent, IMPORT);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                });
        builder.show();
    }

    private void startImport(final InputStream target, final Uri targetUri, final DataFormat dataFormat, final char[] password, final boolean closeWhenDone) {
        mTasks.flushTaskList(TaskHandler.TYPE.IMPORT, true, false, false);
        ImportExportTask.TaskCompleteListener listener = new ImportExportTask.TaskCompleteListener() {
            @Override
            public void onTaskComplete(ImportExportResult result, DataFormat dataFormat) {
                onImportComplete(result, targetUri, dataFormat);
                if (closeWhenDone) {
                    try {
                        target.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        };

        importExporter = new ImportExportTask(ImportExportActivity.this,
                dataFormat, target, password, listener);
        mTasks.executeTask(TaskHandler.TYPE.IMPORT, importExporter);
    }

    private void startExport(final OutputStream target, final Uri targetUri, char[] password, final boolean closeWhenDone) {
        mTasks.flushTaskList(TaskHandler.TYPE.EXPORT, true, false, false);
        ImportExportTask.TaskCompleteListener listener = new ImportExportTask.TaskCompleteListener() {
            @Override
            public void onTaskComplete(ImportExportResult result, DataFormat dataFormat) {
                onExportComplete(result, targetUri);
                if (closeWhenDone) {
                    try {
                        target.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        };

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
            activityResultParser(IMPORT, RESULT_OK, uri, input.getText().toString().toCharArray());
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());

        builder.show();
    }

    private void onImportComplete(ImportExportResult result, Uri path, DataFormat dataFormat) {
        if (result == ImportExportResult.BadPassword) {
            retryWithPassword(dataFormat, path);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int messageId;

        if (result == ImportExportResult.Success) {
            builder.setTitle(R.string.importSuccessfulTitle);
            messageId = R.string.importSuccessful;
        } else {
            builder.setTitle(R.string.importFailedTitle);
            messageId = R.string.importFailed;
        }

        final String message = getResources().getString(messageId);

        builder.setMessage(message);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void onExportComplete(ImportExportResult result, final Uri path) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int messageId;

        if (result == ImportExportResult.Success) {
            builder.setTitle(R.string.exportSuccessfulTitle);
            messageId = R.string.exportSuccessful;
        } else {
            builder.setTitle(R.string.exportFailedTitle);
            messageId = R.string.exportFailed;
        }

        final String message = getResources().getString(messageId);

        builder.setMessage(message);
        builder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss());

        if (result == ImportExportResult.Success) {
            final CharSequence sendLabel = ImportExportActivity.this.getResources().getText(R.string.sendLabel);

            builder.setPositiveButton(sendLabel, (dialog, which) -> {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, path);
                sendIntent.setType("text/csv");

                // set flag to give temporary permission to external app to use the FileProvider
                sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                ImportExportActivity.this.startActivity(Intent.createChooser(sendIntent,
                        sendLabel));

                dialog.dismiss();
            });
        }

        builder.create().show();
    }

    private void chooseFileWithIntent(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), R.string.failedOpeningFileManager, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    private void activityResultParser(int requestCode, int resultCode, Uri uri, char[] password) {
        if (resultCode != RESULT_OK) {
            Log.w(TAG, "Failed onActivityResult(), result=" + resultCode);
            return;
        }

        if (uri == null) {
            Log.e(TAG, "Activity returned a NULL URI");
            return;
        }

        try {
            if (requestCode == CHOOSE_EXPORT_LOCATION) {

                OutputStream writer;
                if (uri.getScheme() != null) {
                    writer = getContentResolver().openOutputStream(uri);
                } else {
                    writer = new FileOutputStream(new File(uri.toString()));
                }
                Log.e(TAG, "Starting file export with: " + uri.toString());
                startExport(writer, uri, exportPassword.toCharArray(), true);
            } else {
                InputStream reader;
                if (uri.getScheme() != null) {
                    reader = getContentResolver().openInputStream(uri);
                } else {
                    reader = new FileInputStream(new File(uri.toString()));
                }

                Log.e(TAG, "Starting file import with: " + uri.toString());

                startImport(reader, uri, importDataFormat, password, true);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to import/export file: " + uri.toString(), e);
            if (requestCode == CHOOSE_EXPORT_LOCATION) {
                onExportComplete(ImportExportResult.GenericFailure, uri);
            } else {
                onImportComplete(ImportExportResult.GenericFailure, uri, importDataFormat);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            Log.e(TAG, "Activity returned NULL data");
            return;
        }

        activityResultParser(requestCode, resultCode, data.getData(), null);
    }
}