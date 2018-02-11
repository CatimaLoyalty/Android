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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class LoyaltyCardEditActivity extends AppCompatActivity
{
    private static final String TAG = "CardLocker";

    private static final int SELECT_BARCODE_REQUEST = 1;

    EditText storeFieldEdit;
    EditText noteFieldEdit;
    TextView cardIdFieldView;
    View cardIdDivider;
    View cardIdTableRow;
    TextView barcodeTypeField;
    ImageView barcodeImage;
    View barcodeImageLayout;
    View barcodeCaptureLayout;

    Button captureButton;
    Button enterButton;

    int loyaltyCardId;
    boolean updateLoyaltyCard;

    DBHelper db;

    private void extractIntentFields(Intent intent)
    {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt("id") : 0;
        updateLoyaltyCard = b != null && b.getBoolean("update", false);

        Log.d(TAG, "View activity: id=" + loyaltyCardId
                + ", updateLoyaltyCard=" + Boolean.toString(updateLoyaltyCard));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.loyalty_card_edit_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        extractIntentFields(getIntent());

        db = new DBHelper(this);

        storeFieldEdit = findViewById(R.id.storeNameEdit);
        noteFieldEdit = findViewById(R.id.noteEdit);
        cardIdFieldView = findViewById(R.id.cardIdView);
        cardIdDivider = findViewById(R.id.cardIdDivider);
        cardIdTableRow = findViewById(R.id.cardIdTableRow);
        barcodeTypeField = findViewById(R.id.barcodeType);
        barcodeImage = findViewById(R.id.barcode);
        barcodeImageLayout = findViewById(R.id.barcodeLayout);
        barcodeCaptureLayout = findViewById(R.id.barcodeCaptureLayout);

        captureButton = findViewById(R.id.captureButton);
        enterButton = findViewById(R.id.enterButton);
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);

        // Reset these fields, so they are re-populated in onResume().
        storeFieldEdit.setText("");
        noteFieldEdit.setText("");
        cardIdFieldView.setText("");
        barcodeTypeField.setText("");
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

        if(updateLoyaltyCard)
        {
            final LoyaltyCard loyaltyCard = db.getLoyaltyCard(loyaltyCardId);
            if(loyaltyCard == null)
            {
                Log.w(TAG, "Could not lookup loyalty card " + loyaltyCardId);
                Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if(storeFieldEdit.getText().length() == 0)
            {
                storeFieldEdit.setText(loyaltyCard.store);
            }

            if(noteFieldEdit.getText().length() == 0)
            {
                noteFieldEdit.setText(loyaltyCard.note);
            }

            if(cardIdFieldView.getText().length() == 0)
            {
                cardIdFieldView.setText(loyaltyCard.cardId);
            }

            if(barcodeTypeField.getText().length() == 0)
            {
                barcodeTypeField.setText(loyaltyCard.barcodeType);
            }

            setTitle(R.string.editCardTitle);
        }
        else
        {
            setTitle(R.string.addCardTitle);
        }

        if(cardIdFieldView.getText().length() > 0 && barcodeTypeField.getText().length() > 0)
        {
            String formatString = barcodeTypeField.getText().toString();
            final BarcodeFormat format = BarcodeFormat.valueOf(formatString);
            final String cardIdString = cardIdFieldView.getText().toString();

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

            barcodeImageLayout.setVisibility(View.VISIBLE);
        }

        View.OnClickListener captureCallback = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                IntentIntegrator integrator = new IntentIntegrator(LoyaltyCardEditActivity.this);
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

                String cardId = cardIdFieldView.getText().toString();
                if(cardId.length() > 0)
                {
                    final Bundle b = new Bundle();
                    b.putString("initialCardId", cardId);
                    i.putExtras(b);
                }

                startActivityForResult(i, SELECT_BARCODE_REQUEST);
            }
        });

        if(cardIdFieldView.getText().length() > 0)
        {
            cardIdDivider.setVisibility(View.VISIBLE);
            cardIdTableRow.setVisibility(View.VISIBLE);
            enterButton.setText(R.string.editCard);
        }
        else
        {
            cardIdDivider.setVisibility(View.GONE);
            cardIdTableRow.setVisibility(View.GONE);
            enterButton.setText(R.string.enterCard);
        }
    }

    private void doSave()
    {
        String store = storeFieldEdit.getText().toString();
        String note = noteFieldEdit.getText().toString();
        String cardId = cardIdFieldView.getText().toString();
        String barcodeType = barcodeTypeField.getText().toString();

        if(store.isEmpty())
        {
            Snackbar.make(storeFieldEdit, R.string.noStoreError, Snackbar.LENGTH_LONG).show();
            return;
        }

        if(cardId.isEmpty() || barcodeType.isEmpty())
        {
            Snackbar.make(cardIdFieldView, R.string.noCardIdError, Snackbar.LENGTH_LONG).show();
            return;
        }

        if(updateLoyaltyCard)
        {
            db.updateLoyaltyCard(loyaltyCardId, store, note, cardId, barcodeType);
            Log.i(TAG, "Updated " + loyaltyCardId + " to " + cardId);
        }
        else
        {
            loyaltyCardId = (int)db.insertLoyaltyCard(store, note, cardId, barcodeType);
        }

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(updateLoyaltyCard)
        {
            getMenuInflater().inflate(R.menu.card_update_menu, menu);
        }
        else
        {
            getMenuInflater().inflate(R.menu.card_add_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

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

                        DBHelper db = new DBHelper(LoyaltyCardEditActivity.this);
                        db.deleteLoyaltyCard(loyaltyCardId);

                        ShortcutHelper.removeShortcut(LoyaltyCardEditActivity.this, loyaltyCardId);

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

            case R.id.action_save:
                doSave();
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

            TextView cardIdView = findViewById(R.id.cardIdView);
            cardIdView.setText(contents);

            final TextView barcodeTypeField = findViewById(R.id.barcodeType);
            barcodeTypeField.setText(format);
            onResume();
        }
    }
}
