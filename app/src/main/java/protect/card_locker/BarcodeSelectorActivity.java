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
import protect.card_locker.async.TaskHandler;

/**
 * This activity is callable and will allow a user to enter
 * barcode data and generate all barcodes possible for
 * the data. The user may then select any barcode, where its
 * data and type will be returned to the caller.
 */
public class BarcodeSelectorActivity extends CatimaAppCompatActivity implements BarcodeSelectorAdapter.BarcodeSelectorListener {
    private static final String TAG = "Catima";

    // Result this activity will return
    public static final String BARCODE_CONTENTS = "contents";
    public static final String BARCODE_FORMAT = "format";

    final private TaskHandler mTasks = new TaskHandler();

    private final Handler typingDelayHandler = new Handler(Looper.getMainLooper());
    public static final Integer INPUT_DELAY = 250;

    private ListView mBarcodeList;
    private BarcodeSelectorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.selectBarcodeTitle);
        setContentView(R.layout.barcode_selector_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText cardId = findViewById(R.id.cardId);
        mBarcodeList = findViewById(R.id.barcodes);

        cardId.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Delay the input processing so we avoid overload
                typingDelayHandler.removeCallbacksAndMessages(null);

                typingDelayHandler.postDelayed(() -> {
                    Log.d(TAG, "Entered text: " + s);

                    runOnUiThread(() -> {
                        generateBarcodes(s.toString());

                        View noBarcodeButtonView = findViewById(R.id.noBarcode);
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
        ArrayList<CatimaBarcodeWithValue> catimaBarcodeWithValues = new ArrayList<>();
        mAdapter = new BarcodeSelectorAdapter(this, catimaBarcodeWithValues, mTasks, this);
        for (BarcodeFormat barcodeFormat : CatimaBarcode.barcodeFormats) {
            CatimaBarcode catimaBarcode = CatimaBarcode.fromBarcode(barcodeFormat);
            mAdapter.add(new CatimaBarcodeWithValue(catimaBarcode, value));
        }
        mBarcodeList.setAdapter(mAdapter);
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
