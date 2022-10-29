package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import protect.card_locker.databinding.BarcodeSelectorActivityBinding;

/**
 * This activity is callable and will allow a user to enter
 * barcode data and generate all barcodes possible for
 * the data. The user may then select any barcode, where its
 * data and type will be returned to the caller.
 */
public class BarcodeSelectorActivity extends CatimaAppCompatActivity implements BarcodeSelectorAdapter.BarcodeSelectorListener {
    private BarcodeSelectorActivityBinding binding;
    private static final String TAG = "Catima";

    // Result this activity will return
    public static final String BARCODE_CONTENTS = "contents";
    public static final String BARCODE_FORMAT = "format";

    private final Handler typingDelayHandler = new Handler(Looper.getMainLooper());
    public static final Integer INPUT_DELAY = 250;

    private BarcodeSelectorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BarcodeSelectorActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.selectBarcodeTitle);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        enableToolbarBackButton();

        EditText cardId = binding.cardId;
        ListView mBarcodeList = binding.barcodes;
        mAdapter = new BarcodeSelectorAdapter(this, new ArrayList<>(), this);
        mBarcodeList.setAdapter(mAdapter);

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
        // Update barcodes
        ArrayList<CatimaBarcodeWithValue> barcodes = new ArrayList<>();
        for (BarcodeFormat barcodeFormat : CatimaBarcode.barcodeFormats) {
            CatimaBarcode catimaBarcode = CatimaBarcode.fromBarcode(barcodeFormat);
            barcodes.add(new CatimaBarcodeWithValue(catimaBarcode, value));
        }
        mAdapter.setBarcodes(barcodes);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRowClicked(int inputPosition, View view) {
        CatimaBarcodeWithValue barcodeWithValue = mAdapter.getItem(inputPosition);
        CatimaBarcode catimaBarcode = barcodeWithValue.catimaBarcode();

        if (!mAdapter.isValid(view)) {
            Toast.makeText(this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
            return;
        }

        String barcodeFormat = catimaBarcode.format().name();
        String value = barcodeWithValue.value();

        Log.d(TAG, "Selected barcode type " + barcodeFormat);

        Intent result = new Intent();
        result.putExtra(BARCODE_FORMAT, barcodeFormat);
        result.putExtra(BARCODE_CONTENTS, value);
        BarcodeSelectorActivity.this.setResult(RESULT_OK, result);
        finish();
    }
}
