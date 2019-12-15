package protect.card_locker;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import protect.card_locker.preferences.Settings;


public class LoyaltyCardViewActivity extends AppCompatActivity
{
    private static final String TAG = "CardLocker";

    TextView cardIdFieldView;
    TextView noteView;
    View noteViewDivider;
    TextView storeName;
    ImageView barcodeImage;
    View collapsingToolbarLayout;
    int loyaltyCardId;
    LoyaltyCard loyaltyCard;
    boolean rotationEnabled;
    DBHelper db;
    ImportURIHelper importURIHelper;
    Settings settings;

    private void extractIntentFields(Intent intent)
    {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt("id") : 0;
        Log.d(TAG, "View activity: id=" + loyaltyCardId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        settings = new Settings(this);

        extractIntentFields(getIntent());

        setContentView(R.layout.loyalty_card_view_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        db = new DBHelper(this);
        importURIHelper = new ImportURIHelper(this);

        cardIdFieldView = findViewById(R.id.cardIdView);
        noteView = findViewById(R.id.noteView);
        noteViewDivider = findViewById(R.id.noteViewDivider);
        storeName = findViewById(R.id.storeName);
        barcodeImage = findViewById(R.id.barcode);
        collapsingToolbarLayout = findViewById(R.id.collapsingToolbarLayout);

        rotationEnabled = true;
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

        // The brightness value is on a scale from [0, ..., 1], where
        // '1' is the brightest. We attempt to maximize the brightness
        // to help barcode readers scan the barcode.
        Window window = getWindow();
        if(window != null && settings.useMaxBrightnessDisplayingBarcode())
        {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.screenBrightness = 1F;
            window.setAttributes(attributes);
        }

        loyaltyCard = db.getLoyaltyCard(loyaltyCardId);
        if(loyaltyCard == null)
        {
            Log.w(TAG, "Could not lookup loyalty card " + loyaltyCardId);
            Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String formatString = loyaltyCard.barcodeType;
        final BarcodeFormat format = BarcodeFormat.valueOf(formatString);
        final String cardIdString = loyaltyCard.cardId;

        cardIdFieldView.setText(loyaltyCard.cardId);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(cardIdFieldView,
                getResources().getInteger(R.integer.settings_card_id_min_font_size_sp)-1, settings.getCardIdFontSize(),
                1, TypedValue.COMPLEX_UNIT_SP);

        if(loyaltyCard.note.length() > 0)
        {
            noteView.setText(loyaltyCard.note);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(noteView,
                    getResources().getInteger(R.integer.settings_card_note_min_font_size_sp)-1,
                    settings.getCardNoteFontSize(), 1, TypedValue.COMPLEX_UNIT_SP);
        }
        else
        {
            noteView.setVisibility(View.GONE);
            noteViewDivider.setVisibility(View.GONE);
        }

        storeName.setText(loyaltyCard.store);
        storeName.setTextSize(settings.getCardTitleFontSize());

        int textColor;
        if(loyaltyCard.headerTextColor != null)
        {
            textColor = loyaltyCard.headerTextColor;
        }
        else
        {
            textColor = Color.WHITE;
        }
        storeName.setTextColor(textColor);

        int backgroundHeaderColor;
        if(loyaltyCard.headerColor != null)
        {
            backgroundHeaderColor = loyaltyCard.headerColor;
        }
        else
        {
            backgroundHeaderColor = LetterBitmap.getDefaultColor(this, loyaltyCard.store);
        }

        collapsingToolbarLayout.setBackgroundColor(backgroundHeaderColor);

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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.card_view_menu, menu);

        if(settings.getLockBarcodeScreenOrientation())
        {
            MenuItem item = menu.findItem(R.id.action_lock_unlock);
            setOrientatonLock(item, true);
            item.setVisible(false);
        }

        if(loyaltyCard != null && !loyaltyCard.extras.getAllValues(Locale.getDefault().getLanguage()).isEmpty())
        {
            MenuItem item = menu.findItem(R.id.action_view_extras);
            item.setVisible(true);
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

            case R.id.action_share:
                importURIHelper.startShareIntent(loyaltyCard);
                return true;

            case R.id.action_edit:
                Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("id", loyaltyCardId);
                bundle.putBoolean("update", true);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                return true;

            case R.id.action_lock_unlock:
                if(rotationEnabled)
                {
                    setOrientatonLock(item, true);
                }
                else
                {
                    setOrientatonLock(item, false);
                }
                rotationEnabled = !rotationEnabled;
                return true;

            case R.id.action_view_extras:
                try
                {
                    displayExtrasDialog();
                }
                catch (JSONException ex)
                {
                    Toast.makeText(this, R.string.failedShowingExtras, Toast.LENGTH_LONG).show();
                    finish();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setOrientatonLock(MenuItem item, boolean lock)
    {
        if(lock)
        {
            item.setIcon(R.drawable.ic_lock_outline_white_24dp);
            item.setTitle(R.string.unlockScreen);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        else
        {
            item.setIcon(R.drawable.ic_lock_open_white_24dp);
            item.setTitle(R.string.lockScreen);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    private void displayExtrasDialog() throws JSONException
    {
        StringBuilder items = new StringBuilder();

        HashMap<String, String> extraValues = loyaltyCard.extras.getAllValues(Locale.getDefault().getLanguage());
        for(String key : extraValues.keySet())
        {
            items.append(extraValues.get(key) + "\n");
        }

        new AlertDialog.Builder(this)
            .setMessage(items.toString())
            .setCancelable(true)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            })
            .show();
    }
}