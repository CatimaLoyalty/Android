package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import protect.card_locker.databinding.ScanActivityBinding;

/**
 * Custom Scannner Activity extending from Activity to display a custom layout form scanner view.
 *
 * Based on https://github.com/journeyapps/zxing-android-embedded/blob/0fdfbce9fb3285e985bad9971c5f7c0a7a334e7b/sample/src/main/java/example/zxing/CustomScannerActivity.java
 * originally licensed under Apache 2.0
 */
public class ScanActivity extends CatimaAppCompatActivity {
    private ScanActivityBinding binding;
    private static final String TAG = "Catima";

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    private String cardId;
    private String addGroup;
    private boolean torch = false;

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
        setTitle(R.string.scanCardBarcode);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        extractIntentFields(getIntent());

        findViewById(R.id.add_from_image).setOnClickListener(this::addFromImage);
        findViewById(R.id.add_manually).setOnClickListener(this::addManually);

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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getMenuInflater().inflate(R.menu.scan_menu, menu);
        }

        barcodeScannerView.setTorchOff();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
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
        startActivityForResult(i, Utils.SELECT_BARCODE_REQUEST);
    }

    public void addFromImage(View view) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, Utils.BARCODE_IMPORT_FROM_IMAGE_FILE);
    }
}
