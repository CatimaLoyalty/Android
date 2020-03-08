package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;
import com.google.zxing.BarcodeFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

/**
 * This activity is callable and will allow a user to enter
 * barcode data and generate all barcodes possible for
 * the data. The user may then select any barcode, where its
 * data and type will be returned to the caller.
 */
public class BarcodeSelectorActivity extends AppCompatActivity
{
    private static final String TAG = "LoyaltyCardLocker";

    // Result this activity will return
    public static final String BARCODE_CONTENTS = "contents";
    public static final String BARCODE_FORMAT = "format";

    // These are all the barcode types that the zxing library
    // is able to generate a barcode for, and thus should be
    // the only barcodes which we should attempt to scan.
    public static final Collection<String> SUPPORTED_BARCODE_TYPES = Collections.unmodifiableList(
        Arrays.asList(
                BarcodeFormat.AZTEC.name(),
                BarcodeFormat.CODE_39.name(),
                BarcodeFormat.CODE_128.name(),
                BarcodeFormat.CODABAR.name(),
                BarcodeFormat.DATA_MATRIX.name(),
                BarcodeFormat.EAN_8.name(),
                BarcodeFormat.EAN_13.name(),
                BarcodeFormat.ITF.name(),
                BarcodeFormat.PDF_417.name(),
                BarcodeFormat.QR_CODE.name(),
                BarcodeFormat.UPC_A.name()
        ));

    private Map<String, Pair<Integer, Integer>> barcodeViewMap;
    private LinkedList<AsyncTask> barcodeGeneratorTasks = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.barcode_selector_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        barcodeViewMap = ImmutableMap.<String, Pair<Integer, Integer>>builder()
                .put(BarcodeFormat.AZTEC.name(), new Pair<>(R.id.aztecBarcode, R.id.aztecBarcodeText))
                .put(BarcodeFormat.CODE_39.name(), new Pair<>(R.id.code39Barcode, R.id.code39BarcodeText))
                .put(BarcodeFormat.CODE_128.name(), new Pair<>(R.id.code128Barcode, R.id.code128BarcodeText))
                .put(BarcodeFormat.CODABAR.name(), new Pair<>(R.id.codabarBarcode, R.id.codabarBarcodeText))
                .put(BarcodeFormat.DATA_MATRIX.name(), new Pair<>(R.id.datamatrixBarcode, R.id.datamatrixBarcodeText))
                .put(BarcodeFormat.EAN_8.name(), new Pair<>(R.id.ean8Barcode, R.id.ean8BarcodeText))
                .put(BarcodeFormat.EAN_13.name(), new Pair<>(R.id.ean13Barcode, R.id.ean13BarcodeText))
                .put(BarcodeFormat.ITF.name(), new Pair<>(R.id.itfBarcode, R.id.itfBarcodeText))
                .put(BarcodeFormat.PDF_417.name(), new Pair<>(R.id.pdf417Barcode, R.id.pdf417BarcodeText))
                .put(BarcodeFormat.QR_CODE.name(), new Pair<>(R.id.qrcodeBarcode, R.id.qrcodeBarcodeText))
                .put(BarcodeFormat.UPC_A.name(), new Pair<>(R.id.upcaBarcode, R.id.upcaBarcodeText))
                .build();

        EditText cardId = findViewById(R.id.cardId);
        cardId.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                // Noting to do
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                Log.d(TAG, "Entered text: " + s);

                // Stop any async tasks which may not have been started yet
                for(AsyncTask task : barcodeGeneratorTasks)
                {
                    task.cancel(false);
                }
                barcodeGeneratorTasks.clear();

                // Update barcodes
                for(String key : barcodeViewMap.keySet())
                {
                    ImageView image = findViewById(barcodeViewMap.get(key).first);
                    TextView text = findViewById(barcodeViewMap.get(key).second);
                    createBarcodeOption(image, key, s.toString(), text);
                }

                View noBarcodeButtonView = findViewById(R.id.noBarcode);
                setButtonListener(noBarcodeButtonView, s.toString());
                noBarcodeButtonView.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                // Noting to do
            }
        });

        final Bundle b = getIntent().getExtras();
        final String initialCardId = b != null ? b.getString("initialCardId") : null;

        if(initialCardId != null)
        {
            cardId.setText(initialCardId);
        }
    }

    private void setButtonListener(final View button, final String cardId)
    {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Selected no barcode");
                Intent result = new Intent();
                result.putExtra(BARCODE_FORMAT, "");
                result.putExtra(BARCODE_CONTENTS, cardId);
                BarcodeSelectorActivity.this.setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    private void createBarcodeOption(final ImageView image, final String formatType, final String cardId, final TextView text)
    {
        final BarcodeFormat format = BarcodeFormat.valueOf(formatType);
        if(format == null)
        {
            Log.w(TAG, "Unsupported barcode format: " + formatType);
            return;
        }

        image.setImageBitmap(null);
        image.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "Selected barcode type " + formatType);
                Intent result = new Intent();
                result.putExtra(BARCODE_FORMAT, formatType);
                result.putExtra(BARCODE_CONTENTS, cardId);
                BarcodeSelectorActivity.this.setResult(RESULT_OK, result);
                finish();
            }
        });

        if(image.getHeight() == 0)
        {
            // The size of the ImageView is not yet available as it has not
            // yet been drawn. Wait for it to be drawn so the size is available.
            image.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener()
                {
                    @Override
                    public void onGlobalLayout()
                    {
                        Log.d(TAG, "Global layout finished, type: + " + formatType + ", width: " + image.getWidth());
                        if (Build.VERSION.SDK_INT < 16)
                        {
                            image.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        else
                        {
                            image.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }

                        Log.d(TAG, "Generating barcode for type " + formatType);
                        BarcodeImageWriterTask task = new BarcodeImageWriterTask(image, cardId, format, text);
                        barcodeGeneratorTasks.add(task);
                        task.execute();
                    }
                });
        }
        else
        {
            Log.d(TAG, "Generating barcode for type " + formatType);
            BarcodeImageWriterTask task = new BarcodeImageWriterTask(image, cardId, format, text);
            barcodeGeneratorTasks.add(task);
            task.execute();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
