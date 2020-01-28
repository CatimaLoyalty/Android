package protect.card_locker;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.TextViewCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;

import protect.card_locker.preferences.Settings;


public class LoyaltyCardViewActivity extends AppCompatActivity
{
    private static final String TAG = "CardLocker";
    private static final double LUMINANCE_MIDPOINT = 0.5;

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

    String cardIdString;
    BarcodeFormat format;

    boolean backgroundNeedsDarkIcons;
    boolean barcodeIsFullscreen = false;
    ViewGroup.LayoutParams barcodeImageState;

    private void extractIntentFields(Intent intent)
    {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt("id") : 0;
        Log.d(TAG, "View activity: id=" + loyaltyCardId);
    }

    private Drawable getIcon(int icon, boolean dark)
    {
        Drawable unwrappedIcon = AppCompatResources.getDrawable(this, icon);
        Drawable wrappedIcon = DrawableCompat.wrap(unwrappedIcon);
        if(dark)
        {
            DrawableCompat.setTint(wrappedIcon, Color.BLACK);
        }
        else
        {
            DrawableCompat.setTintList(wrappedIcon, null);
        }

        return wrappedIcon;
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

        // Allow making barcode fullscreen on tap
        barcodeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(barcodeIsFullscreen)
                {
                    setFullscreen(false);
                }
                else
                {
                    setFullscreen(true);
                }
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

        if(barcodeIsFullscreen)
        {
            // Completely reset state
            //
            // This prevents the barcode from taking up the entire screen
            // on resume and thus being stretched out of proportion.
            recreate();
        }

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
        format = !formatString.isEmpty() ? BarcodeFormat.valueOf(formatString) : null;
        cardIdString = loyaltyCard.cardId;

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

        // If the background is very bright, we should use dark icons
        backgroundNeedsDarkIcons = (ColorUtils.calculateLuminance(backgroundHeaderColor) > LUMINANCE_MIDPOINT);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setHomeAsUpIndicator(getIcon(R.drawable.ic_arrow_back_white, backgroundNeedsDarkIcons));
        }

        // Make notification area light if dark icons are needed
        if(Build.VERSION.SDK_INT >= 23)
        {
            window.getDecorView().setSystemUiVisibility(backgroundNeedsDarkIcons ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
        }
        if(Build.VERSION.SDK_INT >= 21)
        {
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        // Set shadow colour of store text so even same color on same color would be readable
        storeName.setShadowLayer(1, 1, 1, backgroundNeedsDarkIcons ? Color.BLACK : Color.WHITE);

        if(format != null)
        {
            findViewById(R.id.barcode).setVisibility(View.VISIBLE);
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
                                barcodeImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);

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
        else
        {
            findViewById(R.id.barcode).setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (barcodeIsFullscreen)
        {
            setFullscreen(false);
            return;
        }

        super.onBackPressed();
        return;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.card_view_menu, menu);

        // Always calculate lockscreen icon, it may need a black color
        boolean lockBarcodeScreenOrientation = settings.getLockBarcodeScreenOrientation();
        MenuItem item = menu.findItem(R.id.action_lock_unlock);
        setOrientatonLock(item, lockBarcodeScreenOrientation);
        if(lockBarcodeScreenOrientation)
        {
            item.setVisible(false);
        }

        menu.findItem(R.id.action_share).setIcon(getIcon(R.drawable.ic_share_white, backgroundNeedsDarkIcons));
        menu.findItem(R.id.action_edit).setIcon(getIcon(R.drawable.ic_mode_edit_white_24dp, backgroundNeedsDarkIcons));

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
        }

        return super.onOptionsItemSelected(item);
    }

    private void setOrientatonLock(MenuItem item, boolean lock)
    {
        if(lock)
        {

            item.setIcon(getIcon(R.drawable.ic_lock_outline_white_24dp, backgroundNeedsDarkIcons));
            item.setTitle(R.string.unlockScreen);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        else
        {
            item.setIcon(getIcon(R.drawable.ic_lock_open_white_24dp, backgroundNeedsDarkIcons));
            item.setTitle(R.string.lockScreen);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    /**
     * When enabled, hides the status bar and moves the barcode to the top of the screen.
     *
     * The purpose of this function is to make sure the barcode can be scanned from the phone
     * by machines which offer no space to insert the complete device.
     */
    private void setFullscreen(boolean enable)
    {
        ActionBar actionBar = getSupportActionBar();
        if(enable && !barcodeIsFullscreen)
        {
            // Save previous barcodeImage state
            barcodeImageState = barcodeImage.getLayoutParams();

            // Hide actionbar
            if(actionBar != null)
            {
                actionBar.hide();
            }

            // Hide collapsingToolbar
            collapsingToolbarLayout.setVisibility(View.GONE);

            // Set Android to fullscreen mode
            getWindow().getDecorView().setSystemUiVisibility(
                getWindow().getDecorView().getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
            );

            // Make barcode take all space
            barcodeImage.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            ));

            // Move barcode to top
            barcodeImage.setScaleType(ImageView.ScaleType.FIT_START);

            // Prevent centering
            barcodeImage.setAdjustViewBounds(false);

            // Set current state
            barcodeIsFullscreen = true;
        }
        else if(!enable && barcodeIsFullscreen)
        {
            // Show actionbar
            if(actionBar != null)
            {
                actionBar.show();
            }

            // Show collapsingToolbar
            collapsingToolbarLayout.setVisibility(View.VISIBLE);

            // Unset fullscreen mode
            getWindow().getDecorView().setSystemUiVisibility(
                getWindow().getDecorView().getSystemUiVisibility()
                & ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                & ~View.SYSTEM_UI_FLAG_FULLSCREEN
            );

            // Turn barcode back to normal
            barcodeImage.setLayoutParams(barcodeImageState);

            // Fix barcode centering
            barcodeImage.setAdjustViewBounds(true);

            // Set current state
            barcodeIsFullscreen = false;
        }
    }
}
