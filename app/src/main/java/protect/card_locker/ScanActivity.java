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
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
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

import java.util.ArrayList;
import java.util.HashMap;
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
    private static final int PERMISSION_SCAN_ADD_FROM_PDF = 101;

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    private String cardId;
    private String addGroup;
    private boolean torch = false;

    private ActivityResultLauncher<Intent> manualAddLauncher;
    // can't use the pre-made contract because that launches the file manager for image type instead of gallery
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    static final String STATE_SCANNER_ACTIVE = "scannerActive";
    private boolean mScannerActive = true;
    private boolean mHasError = false;

    private void extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();
        cardId = b != null ? b.getString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID) : null;
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
        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleActivityResult(Utils.BARCODE_IMPORT_FROM_PDF_FILE, result.getResultCode(), result.getData()));
        customBarcodeScannerBinding.fabOtherOptions.setOnClickListener(view -> {
            setScannerActive(false);

            ArrayList<HashMap<String, Object>> list = new ArrayList<>();
            String[] texts = new String[]{
                getString(R.string.addWithoutBarcode),
                getString(R.string.addManually),
                getString(R.string.addFromImage),
                getString(R.string.addFromPdfFile),
            };
            Object[] icons = new Object[]{
                R.drawable.baseline_block_24,
                R.drawable.ic_edit,
                R.drawable.baseline_image_24,
                R.drawable.baseline_picture_as_pdf_24,
            };
            String[] columns = new String[]{"text", "icon"};

            for (int i = 0; i < texts.length; i++) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(columns[0], texts[i]);
                map.put(columns[1], icons[i]);
                list.add(map);
            }

            ListAdapter adapter = new SimpleAdapter(
                ScanActivity.this,
                list,
                R.layout.alertdialog_row_with_icon,
                columns,
                new int[]{R.id.textView, R.id.imageView}
            );

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ScanActivity.this);
            builder.setTitle(getString(R.string.add_a_card_in_a_different_way));
            builder.setAdapter(
                    adapter,
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
                            case 3:
                                addFromPdf();
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
        capture = new CatimaCaptureManager(this, barcodeScannerView, this::onCaptureManagerError);
        Intent captureIntent = new Intent();
        Bundle captureIntentBundle = new Bundle();
        captureIntentBundle.putBoolean(Intents.Scan.BEEP_ENABLED, false);
        captureIntent.putExtras(captureIntentBundle);
        capture.initializeFromIntent(captureIntent, savedInstanceState);

        barcodeScannerView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                LoyaltyCard loyaltyCard = new LoyaltyCard();
                loyaltyCard.setCardId(result.getText());
                loyaltyCard.setBarcodeType(CatimaBarcode.fromBarcode(result.getBarcodeFormat()));

                returnResult(new ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard));
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

        if (!Utils.deviceHasCamera(this)) {
            showCameraError(getString(R.string.noCameraFoundGuideText), false);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showCameraPermissionMissingText();
        } else {
            hideCameraError();
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

    private void returnResult(ParseResult parseResult) {
        Intent result = new Intent();
        Bundle bundle = parseResult.toLoyaltyCardBundle();
        if (addGroup != null) {
            bundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, addGroup);
        }
        result.putExtras(bundle);
        ScanActivity.this.setResult(RESULT_OK, result);
        finish();
    }

    private void handleActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        List<ParseResult> parseResultList = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this);

        if (parseResultList.isEmpty()) {
            setScannerActive(true);
            return;
        }

        Utils.makeUserChooseParseResultFromList(this, parseResultList, new ParseResultListDisambiguatorCallback() {
            @Override
            public void onUserChoseParseResult(ParseResult parseResult) {
                returnResult(parseResult);
            }

            @Override
            public void onUserDismissedSelector() {
                setScannerActive(true);
            }
        });
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
            LoyaltyCard loyaltyCard = new LoyaltyCard();
            loyaltyCard.setCardId(input.getText().toString());
            returnResult(new ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard));
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ScanActivity.this);
        builder.setTitle(R.string.add_manually_warning_title);
        builder.setMessage(R.string.add_manually_warning_message);
        builder.setPositiveButton(R.string.continue_, (dialog, which) -> {
            Intent i = new Intent(getApplicationContext(), BarcodeSelectorActivity.class);
            if (cardId != null) {
                final Bundle b = new Bundle();
                b.putString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID, cardId);
                i.putExtras(b);
            }
            manualAddLauncher.launch(i);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> setScannerActive(true));
        builder.setOnCancelListener(dialog -> setScannerActive(true));
        builder.show();
    }

    public void addFromImage() {
        PermissionUtils.requestStorageReadPermission(this, PERMISSION_SCAN_ADD_FROM_IMAGE);
    }

    public void addFromPdf() {
        PermissionUtils.requestStorageReadPermission(this, PERMISSION_SCAN_ADD_FROM_PDF);
    }

    private void addFromImageOrFileAfterPermission(String mimeType, ActivityResultLauncher<Intent> launcher, int chooserText, int errorMessage) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType(mimeType);
        Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentIntent.setType(mimeType);

        Intent chooserIntent = Intent.createChooser(photoPickerIntent, getString(chooserText));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { contentIntent });
        try {
            launcher.launch(chooserIntent);
        } catch (ActivityNotFoundException e) {
            setScannerActive(true);
            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    public void onCaptureManagerError(String errorMessage) {
        if (mHasError) {
            // We're already showing an error, ignore this new error
            return;
        }

        showCameraError(errorMessage, false);
    }

    private void showCameraPermissionMissingText() {
        showCameraError(getString(R.string.noCameraPermissionDirectToSystemSetting), true);
    }

    private void showCameraError(String message, boolean setOnClick) {
        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorMessage.setText(message);

        setCameraErrorState(true, setOnClick);
    }

    private void hideCameraError() {
        setCameraErrorState(false, false);
    }

    private void setCameraErrorState(boolean visible, boolean setOnClick) {
        mHasError = visible;

        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorClickableArea.setOnClickListener(visible && setOnClick ? v -> {
            navigateToSystemPermissionSetting();
        } : null);
        customBarcodeScannerBinding.cardInputContainer.setBackgroundColor(visible ? obtainThemeAttribute(com.google.android.material.R.attr.colorSurface) : Color.TRANSPARENT);
        customBarcodeScannerBinding.cameraErrorLayout.getRoot().setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void scaleScreen() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        float mediumSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,MEDIUM_SCALE_FACTOR_DIP,getResources().getDisplayMetrics());
        boolean shouldScaleSmaller = screenHeight < mediumSizePx;

        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorIcon.setVisibility(shouldScaleSmaller ? View.GONE : View.VISIBLE);
        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorTitle.setVisibility(shouldScaleSmaller ? View.GONE : View.VISIBLE);
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
            if (granted) {
                hideCameraError();
            } else {
                showCameraPermissionMissingText();
            }
        } else if (requestCode == PERMISSION_SCAN_ADD_FROM_IMAGE || requestCode == PERMISSION_SCAN_ADD_FROM_PDF) {
            if (granted) {
                if (requestCode == PERMISSION_SCAN_ADD_FROM_IMAGE) {
                    addFromImageOrFileAfterPermission("image/*", photoPickerLauncher, R.string.addFromImage, R.string.failedLaunchingPhotoPicker);
                } else {
                    addFromImageOrFileAfterPermission("application/pdf", pdfPickerLauncher, R.string.addFromPdfFile, R.string.failedLaunchingFileManager);
                }
            } else {
                setScannerActive(true);
                Toast.makeText(this, R.string.storageReadPermissionRequired, Toast.LENGTH_LONG).show();
            }
        }
    }
}
