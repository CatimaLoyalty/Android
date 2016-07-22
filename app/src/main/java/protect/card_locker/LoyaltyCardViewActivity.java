package protect.card_locker;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class LoyaltyCardViewActivity extends AppCompatActivity
{
    private static final String TAG = "CardLocker";

    private static final int SELECT_BARCODE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.loyalty_card_view_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final Bundle b = getIntent().getExtras();
        final int loyaltyCardId = b != null ? b.getInt("id") : 0;
        final boolean updateLoyaltyCard = b != null && b.getBoolean("update", false);
        final boolean viewLoyaltyCard = b != null && b.getBoolean("view", false);

        Log.i(TAG, "To view card: " + loyaltyCardId);

        if(viewLoyaltyCard)
        {
            // The brightness value is on a scale from [0, ..., 1], where
            // '1' is the brightest. We attempt to maximize the brightness
            // to help barcode readers scan the barcode.
            Window window = getWindow();
            if(window != null)
            {
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.screenBrightness = 1F;
                window.setAttributes(attributes);
            }
        }

        final EditText storeField = (EditText) findViewById(R.id.storeName);
        final EditText noteField = (EditText) findViewById(R.id.note);
        final EditText cardIdField = (EditText) findViewById(R.id.cardId);
        final EditText barcodeTypeField = (EditText) findViewById(R.id.barcodeType);
        final ImageView barcodeImage = (ImageView) findViewById(R.id.barcode);
        final View barcodeIdLayout = findViewById(R.id.barcodeIdLayout);
        final View barcodeTypeLayout = findViewById(R.id.barcodeTypeLayout);
        final View barcodeImageLayout = findViewById(R.id.barcodeLayout);
        final View barcodeCaptureLayout = findViewById(R.id.barcodeCaptureLayout);

        final Button captureButton = (Button) findViewById(R.id.captureButton);
        final Button enterButton = (Button) findViewById(R.id.enterButton);
        final Button saveButton = (Button) findViewById(R.id.saveButton);
        final Button cancelButton = (Button) findViewById(R.id.cancelButton);

        final DBHelper db = new DBHelper(this);

        if(updateLoyaltyCard || viewLoyaltyCard)
        {
            final LoyaltyCard loyaltyCard = db.getLoyaltyCard(loyaltyCardId);

            if(storeField.getText().length() == 0)
            {
                storeField.setText(loyaltyCard.store);
            }

            if(noteField.getText().length() == 0)
            {
                noteField.setText(loyaltyCard.note);
            }

            if(cardIdField.getText().length() == 0)
            {
                cardIdField.setText(loyaltyCard.cardId);
            }

            if(barcodeTypeField.getText().length() == 0)
            {
                barcodeTypeField.setText(loyaltyCard.barcodeType);
            }

            if(viewLoyaltyCard)
            {
                storeField.setEnabled(false);
                noteField.setEnabled(false);
            }

            if(updateLoyaltyCard)
            {
                setTitle(R.string.editCardTitle);
            }
            else
            {
                barcodeCaptureLayout.setVisibility(View.GONE);
                captureButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                setTitle(R.string.viewCardTitle);
            }
        }
        else
        {
            setTitle(R.string.addCardTitle);
        }

        if(cardIdField.getText().length() == 0)
        {
            barcodeIdLayout.setVisibility(View.GONE);
        }

        barcodeTypeLayout.setVisibility(View.GONE);

        if(cardIdField.getText().length() > 0 && barcodeTypeField.getText().length() > 0)
        {
            String formatString = barcodeTypeField.getText().toString();
            final BarcodeFormat format = BarcodeFormat.valueOf(formatString);
            final String cardIdString = cardIdField.getText().toString();

            if(barcodeImage.getHeight() == 0)
            {
                Log.d(TAG, "ImageView size is not known known at start, waiting for load");
                // The size of the ImageView is not yet available as it has not
                // yet been drawn. Wait for it to be drawn so the size is available.
                barcodeImage.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener()
                    {
                        @Override
                        public void onGlobalLayout()
                        {
                            if (Build.VERSION.SDK_INT < 16)
                            {
                                barcodeImage.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                            else
                            {
                                barcodeImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }

                            Log.d(TAG, "ImageView size now known");
                            new BarcodeImageWriterTask(barcodeImage, cardIdString, format).execute();
                        }
                    });
            }
            else
            {
                Log.d(TAG, "ImageView size known known, creating barcode");
                new BarcodeImageWriterTask(barcodeImage, cardIdString, format).execute();
            }

            barcodeIdLayout.setVisibility(View.VISIBLE);
            barcodeImageLayout.setVisibility(View.VISIBLE);
        }

        View.OnClickListener captureCallback = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                IntentIntegrator integrator = new IntentIntegrator(LoyaltyCardViewActivity.this);
                integrator.setDesiredBarcodeFormats(BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES);

                String prompt = getResources().getString(R.string.scanCardBarcode);
                integrator.setPrompt(prompt);
                integrator.initiateScan();
            }
        };

        captureButton.setOnClickListener(captureCallback);

        enterButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent i = new Intent(getApplicationContext(), BarcodeSelectorActivity.class);
                startActivityForResult(i, SELECT_BARCODE_REQUEST);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                String store = storeField.getText().toString();
                String note = noteField.getText().toString();
                String cardId = cardIdField.getText().toString();
                String barcodeType = barcodeTypeField.getText().toString();

                if(store.isEmpty())
                {
                    Snackbar.make(v, R.string.noStoreError, Snackbar.LENGTH_LONG).show();
                    return;
                }

                if(cardId.isEmpty() || barcodeType.isEmpty())
                {
                    Snackbar.make(v, R.string.noCardIdError, Snackbar.LENGTH_LONG).show();
                    return;
                }

                if(updateLoyaltyCard)
                {
                    db.updateLoyaltyCard(loyaltyCardId, store, note, cardId, barcodeType);
                    Log.i(TAG, "Updated " + loyaltyCardId + " to " + cardId);
                }
                else
                {
                    db.insertLoyaltyCard(store, note, cardId, barcodeType);
                }

                finish();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        final Bundle b = getIntent().getExtras();
        final boolean updateLoyaltyCard = b != null && b.getBoolean("update", false);
        final boolean viewLoyaltyCard = b != null && b.getBoolean("view", false);

        if(viewLoyaltyCard)
        {
            getMenuInflater().inflate(R.menu.card_edit_menu, menu);
        }
        else if(updateLoyaltyCard)
        {
            getMenuInflater().inflate(R.menu.card_delete_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        final Bundle b = getIntent().getExtras();
        final int loyaltyCardId = b != null ? b.getInt("id") : 0;

        switch(id)
        {
            case android.R.id.home:
                finish();
                break;

            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.deleteTitle);
                builder.setMessage(R.string.deleteConfirmation);
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Log.e(TAG, "Deleting card: " + loyaltyCardId);

                        DBHelper db = new DBHelper(LoyaltyCardViewActivity.this);
                        db.deleteLoyaltyCard(loyaltyCardId);
                        finish();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            case R.id.action_edit:
                Intent intent = new Intent(getApplicationContext(), LoyaltyCardViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("id", loyaltyCardId);
                bundle.putBoolean("update", true);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        String contents = null;
        String format = null;

        IntentResult result =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null)
        {
            Log.i(TAG, "Received barcode information from capture");
            contents = result.getContents();
            format = result.getFormatName();
        }

        if(requestCode == SELECT_BARCODE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            Log.i(TAG, "Received barcode information from capture");

            contents = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_CONTENTS);
            format = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_FORMAT);
        }

        if(contents != null && contents.isEmpty() == false &&
                format != null && format.isEmpty() == false)
        {
            Log.i(TAG, "Read barcode id: " + contents);
            Log.i(TAG, "Read format: " + format);

            final EditText cardIdField = (EditText) findViewById(R.id.cardId);
            cardIdField.setText(contents);
            final EditText barcodeTypeField = (EditText) findViewById(R.id.barcodeType);
            barcodeTypeField.setText(format);
            onResume();
        }
    }
}