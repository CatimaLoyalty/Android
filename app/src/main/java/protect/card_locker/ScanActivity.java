package protect.card_locker;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

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

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    private String cardId;
    private String addGroup;
    private boolean torch = false;

    private int buttonDefaultMinHeight;
    private int buttonDefaultMaxHeight;

    private ActivityResultLauncher<Intent> manualAddLauncher;
    // can't use the pre-made contract because that launches the file manager for image type instead of gallery
    private ActivityResultLauncher<Intent> photoPickerLauncher;

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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        extractIntentFields(getIntent());

        manualAddLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleActivityResult(Utils.SELECT_BARCODE_REQUEST, result.getResultCode(), result.getData()));
        photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleActivityResult(Utils.BARCODE_IMPORT_FROM_IMAGE_FILE, result.getResultCode(), result.getData()));
        customBarcodeScannerBinding.addFromImage.setOnClickListener(this::addFromImage);
        customBarcodeScannerBinding.addManually.setOnClickListener(this::addManually);

        barcodeScannerView = binding.zxingBarcodeScanner;

        // Even though we do the actual decoding with the barcodeScannerView
        // CaptureManager needs to be running to show the camera and scanning bar
        capture = new CatimaCaptureManager(this, barcodeScannerView);
        Intent captureIntent = new Intent();
        Bundle captureIntentBundle = new Bundle();
        captureIntentBundle.putBoolean(Intents.Scan.BEEP_ENABLED, false);
        captureIntent.putExtras(captureIntentBundle);
        capture.initializeFromIntent(captureIntent, savedInstanceState);
        saveDefaultUIValues();
        barcodeScannerView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                Intent scanResult = new Intent();
                Bundle scanResultBundle = new Bundle();
                scanResultBundle.putString(BarcodeSelectorActivity.BARCODE_CONTENTS, result.getText());
                scanResultBundle.putString(BarcodeSelectorActivity.BARCODE_FORMAT, result.getBarcodeFormat().name());
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
        capture.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            showCameraPermissionMissingText(false);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
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

    private void handleActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        BarcodeValues barcodeValues;

        try {
            barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this);
        } catch (NullPointerException e) {
            Toast.makeText(this, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
            return;
        }

        if (!barcodeValues.isEmpty()) {
            Intent manualResult = new Intent();
            Bundle manualResultBundle = new Bundle();
            manualResultBundle.putString(BarcodeSelectorActivity.BARCODE_CONTENTS, barcodeValues.content());
            manualResultBundle.putString(BarcodeSelectorActivity.BARCODE_FORMAT, barcodeValues.format());
            if (addGroup != null) {
                manualResultBundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, addGroup);
            }
            manualResult.putExtras(manualResultBundle);
            ScanActivity.this.setResult(RESULT_OK, manualResult);
            finish();
        }
    }

    public void addManually(View view) {
        Intent i = new Intent(getApplicationContext(), BarcodeSelectorActivity.class);
        if (cardId != null) {
            final Bundle b = new Bundle();
            b.putString("initialCardId", cardId);
            i.putExtras(b);
        }
        manualAddLauncher.launch(i);
    }

    public void addFromImage(View view) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        try {
            photoPickerLauncher.launch(photoPickerIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), R.string.failedLaunchingPhotoPicker, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    private void showCameraPermissionMissingText(boolean show) {
        customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedClickableArea.setOnClickListener(show ? v -> {
            navigateToSystemPermissionSetting();
        } : null);
        customBarcodeScannerBinding.cardInputContainer.setBackgroundColor(show ? obtainThemeAttribute(R.attr.colorSurface) : Color.TRANSPARENT);
        customBarcodeScannerBinding.cameraPermissionDeniedLayout.getRoot().setVisibility(show ? View.VISIBLE : View.GONE);

    }

    private void scaleScreen() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float ratio = (float) metrics.heightPixels / metrics.widthPixels;
        boolean shouldScaleSmaller = ratio <= 0.8f;
        boolean shouldScaleMedium = ratio <= 1.1f && !shouldScaleSmaller;
        if (shouldScaleMedium) {
            customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedIcon.setVisibility( View.GONE );
        } else {
            int buttonMinHeight = getResources().getDimensionPixelSize(R.dimen.scan_button_min_height);
            customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedTitle.setVisibility(shouldScaleSmaller ? View.GONE : View.VISIBLE);
            customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedIcon.setVisibility(shouldScaleSmaller ? View.GONE : View.VISIBLE);
            customBarcodeScannerBinding.cameraPermissionDeniedLayout.cameraPermissionDeniedMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(shouldScaleSmaller ? R.dimen.no_data_min_textSize : R.dimen.no_data_max_textSize));
            customBarcodeScannerBinding.addFromImage.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(shouldScaleSmaller ? R.dimen.scan_button_min_textSize : R.dimen.scan_button_max_textSize));
            customBarcodeScannerBinding.addManually.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(shouldScaleSmaller ? R.dimen.scan_button_min_textSize : R.dimen.scan_button_max_textSize));
            customBarcodeScannerBinding.addFromImage.setMinimumHeight(shouldScaleSmaller ? getResources().getDimensionPixelSize(R.dimen.scan_button_min_height) : buttonDefaultMinHeight);
            customBarcodeScannerBinding.addFromImage.setMinimumWidth(shouldScaleSmaller ? buttonMinHeight : buttonDefaultMinHeight);
            customBarcodeScannerBinding.addManually.setMinimumHeight(shouldScaleSmaller ? buttonMinHeight : buttonDefaultMinHeight);
            customBarcodeScannerBinding.addManually.setMinimumWidth(shouldScaleSmaller ? buttonMinHeight : buttonDefaultMinHeight);
            customBarcodeScannerBinding.addFromImage.setMinHeight(shouldScaleSmaller ? buttonMinHeight : buttonDefaultMinHeight);
            customBarcodeScannerBinding.addFromImage.setMinWidth(shouldScaleSmaller ? buttonMinHeight : buttonDefaultMinHeight);
            customBarcodeScannerBinding.addManually.setMinHeight(shouldScaleSmaller ? buttonMinHeight : buttonDefaultMinHeight);
            customBarcodeScannerBinding.addManually.setMinWidth(shouldScaleSmaller ? buttonMinHeight : buttonDefaultMinHeight);
        }
    }

    private void saveDefaultUIValues() {
        buttonDefaultMinHeight = customBarcodeScannerBinding.addFromImage.getMinHeight();
        buttonDefaultMaxHeight = customBarcodeScannerBinding.addFromImage.getMinWidth();
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
        if (requestCode == CaptureManager.getCameraPermissionReqCode())
            showCameraPermissionMissingText(grantResults[0] != PackageManager.PERMISSION_GRANTED);

    }

}
