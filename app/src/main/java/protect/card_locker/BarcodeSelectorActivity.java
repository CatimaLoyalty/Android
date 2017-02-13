package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;

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

    private Map<String, Integer> barcodeViewMap;
    private LinkedList<AsyncTask> barcodeGeneratorTasks = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.barcode_selector_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        barcodeViewMap = ImmutableMap.<String, Integer>builder()
                .put(BarcodeFormat.AZTEC.name(), R.id.aztecBarcode)
                .put(BarcodeFormat.CODE_39.name(), R.id.code39Barcode)
                .put(BarcodeFormat.CODE_128.name(), R.id.code128Barcode)
                .put(BarcodeFormat.CODABAR.name(), R.id.codabarBarcode)
                .put(BarcodeFormat.DATA_MATRIX.name(), R.id.datamatrixBarcode)
                .put(BarcodeFormat.EAN_8.name(), R.id.ean8Barcode)
                .put(BarcodeFormat.EAN_13.name(), R.id.ean13Barcode)
                .put(BarcodeFormat.ITF.name(), R.id.itfBarcode)
                .put(BarcodeFormat.PDF_417.name(), R.id.pdf417Barcode)
                .put(BarcodeFormat.QR_CODE.name(), R.id.qrcodeBarcode)
                .put(BarcodeFormat.UPC_A.name(), R.id.upcaBarcode)
                .build();

        EditText cardId = (EditText) findViewById(R.id.cardId);
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
                    ImageView image = (ImageView)findViewById(barcodeViewMap.get(key));
                    createBarcodeOption(image, key, s.toString());
                }
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

    private void createBarcodeOption(final ImageView image, final String formatType, final String cardId)
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
                        BarcodeImageWriterTask task = new BarcodeImageWriterTask(image, cardId, format);
                        barcodeGeneratorTasks.add(task);
                        task.execute();
                    }
                });
        }
        else
        {
            Log.d(TAG, "Generating barcode for type " + formatType);
            BarcodeImageWriterTask task = new BarcodeImageWriterTask(image, cardId, format);
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
