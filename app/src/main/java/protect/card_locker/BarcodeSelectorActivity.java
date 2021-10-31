package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.databinding.BarcodeSelectorActivityBinding;

/**
 * This activity is callable and will allow a user to enter
 * barcode data and generate all barcodes possible for
 * the data. The user may then select any barcode, where its
 * data and type will be returned to the caller.
 */
public class BarcodeSelectorActivity extends CatimaAppCompatActivity {
    private BarcodeSelectorActivityBinding binding;
    private static final String TAG = "Catima";

    // Result this activity will return
    public static final String BARCODE_CONTENTS = "contents";
    public static final String BARCODE_FORMAT = "format";

    private Map<String, Pair<Integer, Integer>> barcodeViewMap;

    final private TaskHandler mTasks = new TaskHandler();

    private final Handler typingDelayHandler = new Handler(Looper.getMainLooper());
    public static final Integer INPUT_DELAY = 250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BarcodeSelectorActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.selectBarcodeTitle);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        barcodeViewMap = new HashMap<>();
        barcodeViewMap.put(BarcodeFormat.AZTEC.name(), new Pair<>(R.id.aztecBarcode, R.id.aztecBarcodeText));
        barcodeViewMap.put(BarcodeFormat.CODE_39.name(), new Pair<>(R.id.code39Barcode, R.id.code39BarcodeText));
        barcodeViewMap.put(BarcodeFormat.CODE_128.name(), new Pair<>(R.id.code128Barcode, R.id.code128BarcodeText));
        barcodeViewMap.put(BarcodeFormat.CODABAR.name(), new Pair<>(R.id.codabarBarcode, R.id.codabarBarcodeText));
        barcodeViewMap.put(BarcodeFormat.DATA_MATRIX.name(), new Pair<>(R.id.datamatrixBarcode, R.id.datamatrixBarcodeText));
        barcodeViewMap.put(BarcodeFormat.EAN_8.name(), new Pair<>(R.id.ean8Barcode, R.id.ean8BarcodeText));
        barcodeViewMap.put(BarcodeFormat.EAN_13.name(), new Pair<>(R.id.ean13Barcode, R.id.ean13BarcodeText));
        barcodeViewMap.put(BarcodeFormat.ITF.name(), new Pair<>(R.id.itfBarcode, R.id.itfBarcodeText));
        barcodeViewMap.put(BarcodeFormat.PDF_417.name(), new Pair<>(R.id.pdf417Barcode, R.id.pdf417BarcodeText));
        barcodeViewMap.put(BarcodeFormat.QR_CODE.name(), new Pair<>(R.id.qrcodeBarcode, R.id.qrcodeBarcodeText));
        barcodeViewMap.put(BarcodeFormat.UPC_A.name(), new Pair<>(R.id.upcaBarcode, R.id.upcaBarcodeText));
        barcodeViewMap.put(BarcodeFormat.UPC_E.name(), new Pair<>(R.id.upceBarcode, R.id.upceBarcodeText));

        EditText cardId = binding.cardId;

        cardId.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Delay the input processing so we avoid overload
                typingDelayHandler.removeCallbacksAndMessages(null);

                typingDelayHandler.postDelayed(() -> {
                    Log.d(TAG, "Entered text: " + s);

                    runOnUiThread(() -> {
                        generateBarcodes(s.toString());

                        View noBarcodeButtonView = binding.noBarcode;
                        setButtonListener(noBarcodeButtonView, s.toString());
                        noBarcodeButtonView.setEnabled(s.length() > 0);
                    });
                }, INPUT_DELAY);
            }
        });

        final Bundle b = getIntent().getExtras();
        final String initialCardId = b != null ? b.getString("initialCardId") : null;

        if (initialCardId != null) {
            cardId.setText(initialCardId);
        } else {
            generateBarcodes("");
        }
    }

    private void generateBarcodes(String value) {
        // Attempt to stop any async tasks which may not have been started yet
        // TODO this can be very much optimized by only generating Barcodes visible to the User
        mTasks.flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false);

        // Update barcodes
        for (Map.Entry<String, Pair<Integer, Integer>> entry : barcodeViewMap.entrySet()) {
            ImageView image = findViewById(entry.getValue().first);
            TextView text = findViewById(entry.getValue().second);
            createBarcodeOption(image, entry.getKey(), value, text);
        }
    }

    private void setButtonListener(final View button, final String cardId) {
        button.setOnClickListener(view -> {
            Log.d(TAG, "Selected no barcode");
            Intent result = new Intent();
            result.putExtra(BARCODE_FORMAT, "");
            result.putExtra(BARCODE_CONTENTS, cardId);
            BarcodeSelectorActivity.this.setResult(RESULT_OK, result);
            finish();
        });
    }

    private void createBarcodeOption(final ImageView image, final String formatType, final String cardId, final TextView text) {
        final CatimaBarcode format = CatimaBarcode.fromName(formatType);

        image.setImageBitmap(null);
        image.setOnClickListener(v -> {
            Log.d(TAG, "Selected barcode type " + formatType);

            if (!((boolean) image.getTag())) {
                Toast.makeText(BarcodeSelectorActivity.this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
                return;
            }

            Intent result = new Intent();
            result.putExtra(BARCODE_FORMAT, formatType);
            result.putExtra(BARCODE_CONTENTS, cardId);
            BarcodeSelectorActivity.this.setResult(RESULT_OK, result);
            finish();
        });

        if (image.getHeight() == 0) {
            // The size of the ImageView is not yet available as it has not
            // yet been drawn. Wait for it to be drawn so the size is available.
            image.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Log.d(TAG, "Global layout finished, type: + " + formatType + ", width: " + image.getWidth());
                            image.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            Log.d(TAG, "Generating barcode for type " + formatType);

                            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getApplicationContext(), image, cardId, format, text, true, null);
                            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
                        }
                    });
        } else {
            Log.d(TAG, "Generating barcode for type " + formatType);
            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getApplicationContext(), image, cardId, format, text, true, null);
            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
