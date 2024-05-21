package protect.card_locker;

import android.content.ActivityNotFoundException;
import android.content.Context;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import protect.card_locker.databinding.ImportExportActivityBinding;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportWorker;

public class ImportExportActivity extends CatimaAppCompatActivity {
    private ImportExportActivityBinding binding;
    private static final String TAG = "Catima";

    private String importAlertTitle;
    private String importAlertMessage;
    private DataFormat importDataFormat;
    private String exportPassword;

    private ActivityResultLauncher<Intent> fileCreateLauncher;
    private ActivityResultLauncher<String> fileOpenLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    private static final int PERMISSION_REQUEST_EXPORT = 100;
    private static final int PERMISSION_REQUEST_IMPORT = 101;

    private OneTimeWorkRequest mRequestedWorkRequest;

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

            Data exportRequestData = new Data.Builder()
                    .putString(ImportExportWorker.INPUT_URI, uri.toString())
                    .putString(ImportExportWorker.INPUT_ACTION, ImportExportWorker.ACTION_EXPORT)
                    .putString(ImportExportWorker.INPUT_FORMAT, DataFormat.Catima.name())
                    .putString(ImportExportWorker.INPUT_PASSWORD, exportPassword)
                    .build();

            mRequestedWorkRequest = new OneTimeWorkRequest.Builder(ImportExportWorker.class)
                    .setInputData(exportRequestData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build();

            PermissionUtils.requestPostNotificationsPermission(this, PERMISSION_REQUEST_EXPORT);
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

            final TextInputLayout textInputLayout = new TextInputLayout(ImportExportActivity.this);
            textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(50, 10, 50, 0);
            textInputLayout.setLayoutParams(params);

            final EditText input = new EditText(ImportExportActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setHint(R.string.exportPasswordHint);

            textInputLayout.addView(input);
            container.addView(textInputLayout);
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

    public static OneTimeWorkRequest buildImportRequest(DataFormat dataFormat, Uri uri, char[] password) {
        Data importRequestData = new Data.Builder()
                .putString(ImportExportWorker.INPUT_URI, uri.toString())
                .putString(ImportExportWorker.INPUT_ACTION, ImportExportWorker.ACTION_IMPORT)
                .putString(ImportExportWorker.INPUT_FORMAT, dataFormat.name())
                .putString(ImportExportWorker.INPUT_PASSWORD, Arrays.toString(password))
                .build();

        return new OneTimeWorkRequest.Builder(ImportExportWorker.class)
                .setInputData(importRequestData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
    }

    private void openFileForImport(Uri uri, char[] password) {
        mRequestedWorkRequest = buildImportRequest(importDataFormat, uri, password);

        PermissionUtils.requestPostNotificationsPermission(this, PERMISSION_REQUEST_IMPORT);
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
                            .setPositiveButton(R.string.ok, (dialog1, which1) -> {
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
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                });
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void retryWithPassword(Context context, DataFormat dataFormat, Uri uri) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.passwordRequired);

        FrameLayout container = new FrameLayout(context);

        final TextInputLayout textInputLayout = new TextInputLayout(context);
        textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 10, 50, 0);
        textInputLayout.setLayoutParams(params);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.exportPasswordHint);

        textInputLayout.addView(input);
        container.addView(textInputLayout);
        builder.setView(container);

        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            OneTimeWorkRequest importRequest = ImportExportActivity.buildImportRequest(dataFormat, uri, input.getText().toString().toCharArray());
            WorkManager.getInstance(context).enqueueUniqueWork(ImportExportWorker.ACTION_IMPORT, ExistingWorkPolicy.REPLACE, importRequest);
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        onMockedRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onMockedRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        Integer failureReason = null;

        if (requestCode == PERMISSION_REQUEST_EXPORT) {
            if (granted) {
                WorkManager.getInstance(this).enqueueUniqueWork(ImportExportWorker.ACTION_EXPORT, ExistingWorkPolicy.REPLACE, mRequestedWorkRequest);

                Toast.makeText(this, R.string.exportStartedCheckNotifications, Toast.LENGTH_LONG).show();

                // Import/export started
                setResult(RESULT_OK);
                finish();

                return;
            }

            failureReason = R.string.postNotificationsPermissionRequired;
        } else if (requestCode == PERMISSION_REQUEST_IMPORT) {
            if (granted) {
                WorkManager.getInstance(this).enqueueUniqueWork(ImportExportWorker.ACTION_IMPORT, ExistingWorkPolicy.REPLACE, mRequestedWorkRequest);

                // Import/export started
                setResult(RESULT_OK);
                finish();

                return;
            }

            failureReason = R.string.postNotificationsPermissionRequired;
        }

        if (failureReason != null) {
            Toast.makeText(this, failureReason, Toast.LENGTH_LONG).show();
        }
    }
}