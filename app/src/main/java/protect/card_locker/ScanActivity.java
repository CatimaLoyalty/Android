package protect.card_locker;

import static protect.card_locker.BarcodeSelectorActivity.BARCODE_CONTENTS;
import static protect.card_locker.BarcodeSelectorActivity.BARCODE_FORMAT;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

import protect.card_locker.databinding.CustomBarcodeScannerBinding;
import protect.card_locker.databinding.ScanActivityBinding;

/**
 * Custom Scannner Activity extending from Activity to display a custom layout form scanner view.
 * <p>
 * Based on https://github.com/journeyapps/zxing-android-embedded/blob/0fdfbce9fb3285e985bad9971c5f7c0a7a334e7b/sample/src/main/java/example/zxing/CustomScannerActivity.java
 * originally licensed under Apache 2.0
 */
public class ScanActivity extends CatimaAppCompatActivity {
    private ScanActivityBinding binding;
    private CustomBarcodeScannerBinding customBarcodeScannerBinding;
    private static final String TAG = "Catima";

    private static final int MEDIUM_SCALE_FACTOR_DIP = 460;
    private static final int COMPAT_SCALE_FACTOR_DIP = 320;

    private static final int PERMISSION_SCAN_ADD_FROM_IMAGE = 100;

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    private String cardId;
    private String addGroup;
    private boolean torch = false;

    private ActivityResultLauncher<Intent> manualAddLauncher;
    // can't use the pre-made contract because that launches the file manager for image type instead of gallery
    private ActivityResultLauncher<Intent> photoPickerLauncher;

    static final String STATE_SCANNER_ACTIVE = "scannerActive";
    private boolean mScannerActive = true;

    private void extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();
        cardId = b != null ? b.getString(LoyaltyCardEditActivity.BUNDLE_CARDID) : null;
        addGroup = b != null ? b.getString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP) : null;
        Log.d(TAG, "Scan activity: id=" + cardId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ScanActivityBinding.inflate(getLayoutInflater());
        customBarcodeScannerBinding = CustomBarcodeScannerBinding.bind(binding.zxingBarcodeScanner);
        setTitle(R.string.scanCardBarcode);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        enableToolbarBackButton();

        extractIntentFields(getIntent());

        manualAddLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleActivityResult(Utils.SELECT_BARCODE_REQUEST, result.getResultCode(), result.getData()));
        photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleActivityResult(Utils.BARCODE_IMPORT_FROM_IMAGE_FILE, result.getResultCode(), result.getData()));
        customBarcodeScannerBinding.fabOtherOptions.setOnClickListener(view -> {
            setScannerActive(false);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ScanActivity.this);
            builder.setTitle(getString(R.string.add_a_card_in_a_different_way));
            builder.setItems(
                    new CharSequence[]{
                            getString(R.string.addWithoutBarcode),
                            getString(R.string.addManually),
                            getString(R.string.addFromImage)
                    },
                    (dialogInterface, i) -> {
                        switch (i) {
                            case 0:
                                addWithoutBarcode();
                                break;
                            case 1:
                                addManually();
                                break;
                            case 2:
                                addFromImage();
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown 'Add a card in a different way' dialog option");
                        }
                    }
            );
            builder.setOnCancelListener(dialogInterface -> setScannerActive(true));
            builder.show();
        });

        barcodeScannerView = binding.zxingBarcodeScanner;

        // Even though we do the actual decoding with the barcodeScannerView
        // CaptureManager needs to be running to show the camera and scanning bar
        capture = new CatimaCaptureManager(this, barcodeScannerView);
        Intent captureIntent = new Intent();
        Bundle captureIntentBundle = new Bundle();
        captureIntentBundle.putBoolean(Intents.Scan.BEEP_ENABLED, false);
        captureIntent.putExtras(captureIntentBundle);
        capture.initializeFromIntent(captureIntent, savedInstanceState);

        barcodeScannerView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                Intent scanResult = new Intent();
                Bundle scanResultBundle = new Bundle();
                scanResultBundle.putString(BARCODE_CONTENTS, result.getText());
                scanResultBundle.putString(BARCODE_FORMAT, result.getBarcodeFormat().name());
                if (addGroup != null) {
                    scanResultBundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, addGroup);
                }
                scanResult.putExtras(scanResultBundle);
                ScanActivity.this.setResult(RESULT_OK, scanResult);
                finish();
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mScannerActive) {
            capture.onResume();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCameraPermissionMissingText(false);
        }
        scaleScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        capture.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(STATE_SCANNER_ACTIVE, mScannerActive);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mScannerActive = savedInstanceState.getBoolean(STATE_SCANNER_ACTIVE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getMenuInflater().inflate(R.menu.scan_menu, menu);
        }

        barcodeScannerView.setTorchOff();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_toggle_flashlight) {
            if (torch) {
                torch = false;
                barcodeScannerView.setTorchOff();
                item.setTitle(R.string.turn_flashlight_on);
                item.setIcon(R.drawable.ic_flashlight_off_white_24dp);
            } else {
                torch = true;
                barcodeScannerView.setTorchOn();
                item.setTitle(R.string.turn_flashlight_off);
                item.setIcon(R.drawable.ic_flashlight_on_white_24dp);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setScannerActive(boolean isActive) {
        if (isActive) {
            barcodeScannerView.resume();
        } else {
            barcodeScannerView.pause();
        }
        mScannerActive = isActive;
    }

    private void returnResult(String barcodeContents, String barcodeFormat) {
        Intent manualResult = new Intent();
        Bundle manualResultBundle = new Bundle();
        manualResultBundle.putString(BARCODE_CONTENTS, barcodeContents);
        manualResultBundle.putString(BARCODE_FORMAT, barcodeFormat);
        if (addGroup != null) {
            manualResultBundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, addGroup);
        }
        manualResult.putExtras(manualResultBundle);
        ScanActivity.this.setResult(RESULT_OK, manualResult);
        finish();
    }

    private void handleActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this);

        if (barcodeValues.isEmpty()) {
            setScannerActive(true);
            return;
        }

        returnResult(barcodeValues.content(), barcodeValues.format());
    }

    private void addWithoutBarcode() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);

        builder.setOnCancelListener(dialogInterface -> setScannerActive(true));

        // Header
        builder.setTitle(R.string.addWithoutBarcode);

        // Layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int contentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
        params.leftMargin = contentPadding;
        params.topMargin = contentPadding / 2;
        params.rightMargin = contentPadding;

        // Description
        TextView currentTextview = new TextView(this);
        currentTextview.setText(getString(R.string.enter_card_id));
        currentTextview.setLayoutParams(params);
        layout.addView(currentTextview);

        // EditText with spacing
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLayoutParams(params);
        layout.addView(input);

        // Set layout
        builder.setView(layout);

        // Buttons
        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            returnResult(input.getText().toString(), "");
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();

        // Now that the dialog exists, we can bind something that affects the OK button
        input.addTextChangedListener(new SimpleTextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    input.setError(getString(R.string.card_id_must_not_be_empty));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    input.setError(null);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });

        dialog.show();

        // Disable button (must be done **after** dialog is shown to prevent crash
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        // Set focus on input field
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    public void addManually() {
        Intent i = new Intent(getApplicationContext(), BarcodeSelectorActivity.class);
        if (cardId != null) {
            final Bundle b = new Bundle();
            b.putString("initialCardId", cardId);
            i.putExtras(b);
        }
        manualAddLauncher.launch(i);
    }

    public void addFromImage() {
        PermissionUtils.requestStorageReadPermission(this, PERMISSION_SCAN_ADD_FROM_IMAGE);
    }

    private void addFromImageAfterPermission() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(photoPickerIntent, getString(R.string.addFromImage));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { contentIntent });
        try {
            photoPickerLauncher.launch(chooserIntent);
        } catch (ActivityNotFoundException e) {
            setScannerActive(true);
            Toast.makeText(getApplicationContext(), R.string.failedLaunchingPhotoPicker, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    private void showCameraPermissionMissingText(boolean show) {
        customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedClickableArea.setOnClickListener(show ? v -> {
            navigateToSystemPermissionSetting();
        } : null);
        customBarcodeScannerBinding.cardInputContainer.setBackgroundColor(show ? obtainThemeAttribute(com.google.android.material.R.attr.colorSurface) : Color.TRANSPARENT);
        customBarcodeScannerBinding.cameraPermissionDeniedLayout.getRoot().setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void scaleScreen() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        float mediumSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,MEDIUM_SCALE_FACTOR_DIP,getResources().getDisplayMetrics());
        boolean shouldScaleSmaller = screenHeight < mediumSizePx;

        customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedIcon.setVisibility(shouldScaleSmaller ? View.GONE : View.VISIBLE);
        customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedTitle.setVisibility(shouldScaleSmaller ? View.GONE : View.VISIBLE);
    }

    private int obtainThemeAttribute(int attribute) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attribute, typedValue, true);
        return typedValue.data;
    }

    private void navigateToSystemPermissionSetting() {
        Intent permissionIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
        permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(permissionIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        onMockedRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onMockedRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == CaptureManager.getCameraPermissionReqCode()) {
            showCameraPermissionMissingText(!granted);
        } else if (requestCode == PERMISSION_SCAN_ADD_FROM_IMAGE) {
            if (granted) {
                addFromImageAfterPermission();
            } else {
                setScannerActive(true);
                Toast.makeText(this, R.string.storageReadPermissionRequired, Toast.LENGTH_LONG).show();
            }
        }
    }
}
