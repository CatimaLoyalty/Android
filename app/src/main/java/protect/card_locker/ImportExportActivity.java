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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.databinding.ImportExportActivityBinding;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;
import protect.card_locker.importexport.ImportExportResultType;

public class ImportExportActivity extends CatimaAppCompatActivity {
    private ImportExportActivityBinding binding;
    private static final String TAG = "Catima";

    private ImportExportTask importExporter;

    private String importAlertTitle;
    private String importAlertMessage;
    private DataFormat importDataFormat;
    private String exportPassword;

    private ActivityResultLauncher<Intent> fileCreateLauncher;
    private ActivityResultLauncher<String> fileOpenLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    final private TaskHandler mTasks = new TaskHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ImportExportActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.importExport);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        enableToolbarBackButton();

        Intent fileIntent = getIntent();
        if (fileIntent != null && fileIntent.getType() != null) {
            chooseImportType(false, fileIntent.getData());
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
                startExport(writer, uri, exportPassword.toCharArray(), true);
            } catch (IOException e) {
                Log.e(TAG, "Failed to export file: " + result.toString(), e);
                onExportComplete(new ImportExportResult(ImportExportResultType.GenericFailure, result.toString()), uri);
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

        Button exportButton = binding.exportButton;
        exportButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(ImportExportActivity.this);
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
        Button importFilesystem = binding.importOptionFilesystemButton;
        importFilesystem.setOnClickListener(v -> chooseImportType(false, null));

        // Check that there is an app that data can be imported from
        Button importApplication = binding.importOptionApplicationButton;
        importApplication.setOnClickListener(v -> chooseImportType(true, null));
    }

    private void openFileForImport(Uri uri, char[] password) {
        try {
            InputStream reader = getContentResolver().openInputStream(uri);
            Log.e(TAG, "Starting file import with: " + uri.toString());
            startImport(reader, uri, importDataFormat, password, true);
        } catch (IOException e) {
            Log.e(TAG, "Failed to import file: " + uri.toString(), e);
            onImportComplete(new ImportExportResult(ImportExportResultType.GenericFailure, e.toString()), uri, importDataFormat);
        }
    }

    private void chooseImportType(boolean choosePicker,
                                  @Nullable Uri fileData) {

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

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
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

                    if (fileData != null) {
                        openFileForImport(fileData, null);
                        return;
                    }

                    new MaterialAlertDialogBuilder(this)
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
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
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

    private String buildResultDialogMessage(ImportExportResult result, boolean isImport) {
        int messageId;

        if (result.resultType() == ImportExportResultType.Success) {
            messageId = isImport ? R.string.importSuccessful : R.string.exportSuccessful;
        } else {
            messageId = isImport ? R.string.importFailed : R.string.exportFailed;
        }

        StringBuilder messageBuilder = new StringBuilder(getResources().getString(messageId));
        if (result.developerDetails() != null) {
            messageBuilder.append("\n\n");
            messageBuilder.append(getResources().getString(R.string.include_if_asking_support));
            messageBuilder.append("\n\n");
            messageBuilder.append(result.developerDetails());
        }

        return messageBuilder.toString();
    }

    private void onImportComplete(ImportExportResult result, Uri path, DataFormat dataFormat) {
        ImportExportResultType resultType = result.resultType();

        if (resultType == ImportExportResultType.BadPassword) {
            retryWithPassword(dataFormat, path);
            return;
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(resultType == ImportExportResultType.Success ? R.string.importSuccessfulTitle : R.string.importFailedTitle);
        builder.setMessage(buildResultDialogMessage(result, true));
        builder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void onExportComplete(ImportExportResult result, final Uri path) {
        ImportExportResultType resultType = result.resultType();

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(resultType == ImportExportResultType.Success ? R.string.exportSuccessfulTitle : R.string.exportFailedTitle);
        builder.setMessage(buildResultDialogMessage(result, false));
        builder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss());

        if (resultType == ImportExportResultType.Success) {
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
}