package protect.card_locker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.databinding.LoyaltyCardViewLayoutBinding;
import protect.card_locker.preferences.Settings;

public class LoyaltyCardViewActivity extends CatimaAppCompatActivity implements BarcodeImageWriterResultCallback {
    private LoyaltyCardViewLayoutBinding binding;
    private static final String TAG = "Catima";

    int loyaltyCardId;
    ArrayList<Integer> cardList;

    LoyaltyCard loyaltyCard;
    List<Group> loyaltyCardGroups;
    boolean rotationEnabled;
    SQLiteDatabase database;
    ImportURIHelper importURIHelper;
    Settings settings;

    String cardIdString;
    String barcodeIdString;
    CatimaBarcode format;

    Bitmap frontImageBitmap;
    Bitmap backImageBitmap;

    boolean backgroundNeedsDarkIcons;
    boolean isFullscreen = false;
    ImageView barcodeRenderTarget;
    int mainImageIndex = 0;
    List<ImageType> imageTypes;

    static final String STATE_IMAGEINDEX = "imageIndex";
    static final String STATE_FULLSCREEN = "isFullscreen";

    static final String BUNDLE_ID = "id";
    static final String BUNDLE_CARDLIST = "cardList";
    static final String BUNDLE_TRANSITION_RIGHT = "transition_right";

    final private TaskHandler mTasks = new TaskHandler();
    Runnable barcodeImageGenerationFinishedCallback;

    private long initTime = System.currentTimeMillis();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (settings.useVolumeKeysForNavigation()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                // Navigate to the previous card
                if (initTime < (System.currentTimeMillis() - 1000)) {
                    prevNextCard(false);
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                // Navigate to the next card
                if (initTime < (System.currentTimeMillis() - 1000)) {
                    prevNextCard(true);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onMainImageTap() {
        // If we're in fullscreen, leave fullscreen
        if (isFullscreen) {
            setFullscreen(false);
            return;
        }

        ImageType imageType = imageTypes.get(mainImageIndex);

        // If the barcode is shown, switch to fullscreen layout
        if (imageType == ImageType.BARCODE) {
            setFullscreen(true);

            return;
        }

        // If this is an image, open it in the gallery.
        openImageInGallery(imageType);
    }

    private void openImageInGallery(ImageType imageType) {
        File file = null;

        switch (imageType) {
            case ICON:
                file = Utils.retrieveCardImageAsFile(this, loyaltyCardId, ImageLocationType.icon);
                break;
            case IMAGE_FRONT:
                file = Utils.retrieveCardImageAsFile(this, loyaltyCardId, ImageLocationType.front);
                break;
            case IMAGE_BACK:
                file = Utils.retrieveCardImageAsFile(this, loyaltyCardId, ImageLocationType.back);
                break;
            case BARCODE:
                Toast.makeText(this, R.string.barcodeLongPressMessage, Toast.LENGTH_SHORT).show();
                return;
            default:
                // Empty default case for now to keep the spotBugsRelease job happy
        }

        // Do nothing if there is no file
        if (file == null) {
            Toast.makeText(this, R.string.failedToRetrieveImageFile, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file), "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Display a toast message if an image viewer is not installed on device
            Toast.makeText(this, R.string.failedLaunchingPhotoPicker, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onBarcodeImageWriterResult(boolean success) {
        if (!success) {
            imageTypes.remove(ImageType.BARCODE);

            setStateBasedOnImageTypes();

            // Call correct drawMainImage
            setFullscreen(isFullscreen);

            Toast.makeText(LoyaltyCardViewActivity.this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
        }
    }

    enum ImageType {
        NONE,
        ICON,
        BARCODE,
        IMAGE_FRONT,
        IMAGE_BACK
    }

    private void extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt(BUNDLE_ID) : 0;
        cardList = b != null ? b.getIntegerArrayList(BUNDLE_CARDLIST) : null;
        Log.d(TAG, "View activity: id=" + loyaltyCardId);
    }

    private void setScalerGuideline(int zoomLevel) {
        float scale = zoomLevel / 100f;

        if (format != null && format.isSquare()) {
            binding.scalerGuideline.setGuidelinePercent(0.75f * scale);
        } else {
            binding.scalerGuideline.setGuidelinePercent(0.5f * scale);
        }
    }

    private void setScalerWidthGuideline(int zoomLevelWidth) {
        float halfscale = zoomLevelWidth / 200f;

        binding.scalerEndwidthguideline.setGuidelinePercent(0.5f + halfscale);
        binding.scalerStartwidthguideline.setGuidelinePercent(0.5f - halfscale);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Bundle incomingIntentExtras = getIntent().getExtras();

            if (incomingIntentExtras == null) {
                Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            int transitionRight = incomingIntentExtras.getInt(BUNDLE_TRANSITION_RIGHT, -1);
            if (transitionRight == 1) {
                // right side transition
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else if (transitionRight == 0) {
                // left side transition
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        }

        super.onCreate(savedInstanceState);
        binding = LoyaltyCardViewLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Utils.applyWindowInsets(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        settings = new Settings(this);

        if (savedInstanceState != null) {
            mainImageIndex = savedInstanceState.getInt(STATE_IMAGEINDEX);
            isFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN);
        }

        extractIntentFields(getIntent());

        setContentView(binding.getRoot());

        database = new DBHelper(this).getWritableDatabase();
        importURIHelper = new ImportURIHelper(this);

        binding.barcodeScaler.setOnSeekBarChangeListener(setOnSeekBarChangeListenerUnifiedFunction());
        binding.barcodeWidthscaler.setOnSeekBarChangeListener(setOnSeekBarChangeListenerUnifiedFunction());

        rotationEnabled = true;

        binding.fullscreenButtonMinimize.setOnClickListener(v -> setFullscreen(false));

        binding.fabEdit.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt(LoyaltyCardEditActivity.BUNDLE_ID, loyaltyCardId);
            bundle.putBoolean(LoyaltyCardEditActivity.BUNDLE_UPDATE, true);
            intent.putExtras(bundle);
            startActivity(intent);
        });
        binding.fabEdit.bringToFront();

        binding.bottomAppBarInfoButton.setOnClickListener(view -> showInfoDialog());
        binding.bottomAppBarPreviousButton.setOnClickListener(view -> prevNextCard(false));
        binding.bottomAppBarNextButton.setOnClickListener(view -> prevNextCard(true));
        binding.bottomAppBarUpdateBalanceButton.setOnClickListener(view -> showBalanceUpdateDialog());

        binding.iconContainer.setOnClickListener(view -> {
            if (loyaltyCard.getImageThumbnail(this) != null) {
                openImageInGallery(ImageType.ICON);
            } else {
                Toast.makeText(LoyaltyCardViewActivity.this, R.string.icon_header_click_text, Toast.LENGTH_LONG).show();
            }
        });
        binding.iconContainer.setOnLongClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt(LoyaltyCardEditActivity.BUNDLE_ID, loyaltyCardId);
            bundle.putBoolean(LoyaltyCardEditActivity.BUNDLE_UPDATE, true);
            bundle.putBoolean(LoyaltyCardEditActivity.BUNDLE_OPEN_SET_ICON_MENU, true);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();

            return true;
        });

        binding.mainImage.setOnClickListener(view -> onMainImageTap());
        // This long-press was originally only intended for when Talkback was used but sadly limiting
        // this doesn't seem to work well
        binding.mainImage.setOnLongClickListener(view -> {
            setMainImage(true, true);
            return true;
        });
        binding.fullscreenImage.setOnClickListener(view -> onMainImageTap());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFullscreen) {
                    setFullscreen(false);
                } else {
                    finish();
                }
            }
        });
    }

    private SeekBar.OnSeekBarChangeListener setOnSeekBarChangeListenerUnifiedFunction() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    Log.d(TAG, "non user triggered onProgressChanged, ignoring, progress is " + progress);
                    return;
                }
                Log.d(TAG, "Progress is " + progress);
                if (seekBar.getId() == binding.barcodeScaler.getId()) {
                    Log.d(TAG, "Max is " + binding.barcodeScaler.getMax());
                    float scale = (float) progress / (float) binding.barcodeScaler.getMax();
                    Log.d(TAG, "Scaling to " + scale);
                }
                else {
                    Log.d(TAG, "Max is " + binding.barcodeWidthscaler.getMax());
                    float scale = (float) progress / (float) binding.barcodeWidthscaler.getMax();
                    Log.d(TAG, "Scaling to " + scale);
                }
                if (seekBar.getId() == binding.barcodeScaler.getId()) {
                    loyaltyCard.zoomLevel = progress;
                    setScalerGuideline(loyaltyCard.zoomLevel);
                }
                else {
                    loyaltyCard.zoomLevelWidth = progress;
                    setScalerWidthGuideline(loyaltyCard.zoomLevelWidth);
                }

                DBHelper.updateLoyaltyCardZoomLevel(database, loyaltyCardId, loyaltyCard.zoomLevel, loyaltyCard.zoomLevelWidth);
                drawMainImage(mainImageIndex, true, isFullscreen);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    private SpannableStringBuilder padSpannableString(SpannableStringBuilder spannableStringBuilder) {
        if (spannableStringBuilder.length() > 0) {
            spannableStringBuilder.append("\n\n");
        }

        return spannableStringBuilder;
    }

    private boolean hasBalance(LoyaltyCard loyaltyCard) {
        return !loyaltyCard.balance.equals(new BigDecimal(0));
    }

    private void showInfoDialog() {
        AlertDialog.Builder infoDialog = new MaterialAlertDialogBuilder(this);

        int dialogContentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
        infoDialog.setTitle(loyaltyCard.store);

        TextView infoTextview = new TextView(this);
        infoTextview.setPadding(
                dialogContentPadding,
                dialogContentPadding / 2,
                dialogContentPadding,
                0
        );
        infoTextview.setAutoLinkMask(Linkify.ALL);
        infoTextview.setTextIsSelectable(true);

        SpannableStringBuilder infoText = new SpannableStringBuilder();
        if (!loyaltyCard.note.isEmpty()) {
            infoText.append(loyaltyCard.note);
        }

        if (loyaltyCardGroups.size() > 0) {
            List<String> groupNames = new ArrayList<>();
            for (Group group : loyaltyCardGroups) {
                groupNames.add(group._id);
            }

            padSpannableString(infoText);
            infoText.append(getString(R.string.groupsList, TextUtils.join(", ", groupNames)));
        }

        if (hasBalance(loyaltyCard)) {
            padSpannableString(infoText);
            infoText.append(getString(R.string.balanceSentence, Utils.formatBalance(this, loyaltyCard.balance, loyaltyCard.balanceType)));
        }

        appendDateInfo(infoText, loyaltyCard.validFrom, (Utils::isNotYetValid), R.string.validFromSentence, R.string.validFromSentence);

        appendDateInfo(infoText, loyaltyCard.expiry, (Utils::hasExpired), R.string.expiryStateSentenceExpired, R.string.expiryStateSentence);

        infoTextview.setText(infoText);

        infoDialog.setView(infoTextview);
        infoDialog.setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
        infoDialog.create().show();
    }

    private void appendDateInfo(SpannableStringBuilder infoText, Date date, Predicate<Date> dateCheck, @StringRes int dateCheckTrueString, @StringRes int dateCheckFalseString) {
        if (date != null) {
            String formattedDate = DateFormat.getDateInstance(DateFormat.LONG).format(date);

            padSpannableString(infoText);
            if (dateCheck.test(date)) {
                int start = infoText.length();
                infoText.append(getString(dateCheckTrueString, formattedDate));
                infoText.setSpan(new ForegroundColorSpan(Color.RED), start, infoText.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                infoText.append(getString(dateCheckFalseString, formattedDate));
            }
        }
    }

    private void showBalanceUpdateDialog() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);

        // Header
        builder.setTitle(R.string.updateBalanceTitle);

        // Layout
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int contentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
        params.leftMargin = contentPadding;
        params.topMargin = contentPadding / 2;
        params.rightMargin = contentPadding;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView currentTextview = new TextView(this);
        currentTextview.setText(getString(R.string.currentBalanceSentence, Utils.formatBalance(this, loyaltyCard.balance, loyaltyCard.balanceType)));
        layout.addView(currentTextview);

        final TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789,."));
        input.setHint(R.string.updateBalanceHint);

        layout.addView(input);
        layout.setLayoutParams(params);
        container.addView(layout);

        // Set layout
        builder.setView(container);

        // Buttons
        builder.setPositiveButton(R.string.spend, (dialogInterface, i) -> {
            // Calculate and update balance
            try {
                BigDecimal balanceChange = Utils.parseBalance(input.getText().toString(), loyaltyCard.balanceType);
                BigDecimal newBalance = loyaltyCard.balance.subtract(balanceChange).max(new BigDecimal(0));
                DBHelper.updateLoyaltyCardBalance(database, loyaltyCardId, newBalance);
            } catch (ParseException e) {
                Toast.makeText(getApplicationContext(), R.string.amountParsingFailed, Toast.LENGTH_LONG).show();
            }

            // Reload state
            this.onResume();

            // Show new balance
            Toast.makeText(getApplicationContext(), getString(R.string.newBalanceSentence, Utils.formatBalance(this, loyaltyCard.balance, loyaltyCard.balanceType)), Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton(R.string.receive, (dialogInterface, i) -> {
            // Calculate and update balance
            try {
                BigDecimal balanceChange = Utils.parseBalance(input.getText().toString(), loyaltyCard.balanceType);
                BigDecimal newBalance = loyaltyCard.balance.add(balanceChange);
                DBHelper.updateLoyaltyCardBalance(database, loyaltyCardId, newBalance);
            } catch (ParseException e) {
                Toast.makeText(getApplicationContext(), R.string.amountParsingFailed, Toast.LENGTH_LONG).show();
            }

            // Reload state
            this.onResume();

            // Show new balance
            Toast.makeText(getApplicationContext(), getString(R.string.newBalanceSentence, Utils.formatBalance(this, loyaltyCard.balance, loyaltyCard.balanceType)), Toast.LENGTH_LONG).show();
        });
        builder.setNeutralButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();

        // Now that the dialog exists, we can bind something that affects the buttons
        input.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                BigDecimal balanceChange;

                try {
                    balanceChange = Utils.parseBalance(s.toString(), loyaltyCard.balanceType);
                } catch (ParseException e) {
                    input.setError(getString(R.string.amountParsingFailed));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                    return;
                }

                input.setError(null);
                if (balanceChange.equals(new BigDecimal(0))) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                }
            }
        });

        dialog.show();

        // Disable buttons (must be done **after** dialog is shown to prevent crash
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

        // Set focus on input field
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    private void setBottomAppBarButtonState() {
        if (!loyaltyCard.note.isEmpty() || !loyaltyCardGroups.isEmpty() || hasBalance(loyaltyCard) || loyaltyCard.validFrom != null || loyaltyCard.expiry != null) {
            binding.bottomAppBarInfoButton.setVisibility(View.VISIBLE);
        } else {
            binding.bottomAppBarInfoButton.setVisibility(View.GONE);
        }

        if (cardList == null || cardList.size() == 1) {
            binding.bottomAppBarPreviousButton.setVisibility(View.GONE);
            binding.bottomAppBarNextButton.setVisibility(View.GONE);
        } else {
            binding.bottomAppBarPreviousButton.setVisibility(View.VISIBLE);
            binding.bottomAppBarNextButton.setVisibility(View.VISIBLE);
        }

        binding.bottomAppBarUpdateBalanceButton.setVisibility(hasBalance(loyaltyCard) ? View.VISIBLE : View.GONE);
    }

    private void prevNextCard(boolean next) {
        // If we're in RTL layout, we want the "left" button to be "next" instead of "previous"
        // So we swap next around
        boolean transitionRight = next;
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            next = !next;
        }

        int cardListPosition = cardList.indexOf(loyaltyCardId);

        if (next) {
            if (cardListPosition == cardList.size() - 1) {
                cardListPosition = 0;
            } else {
                cardListPosition = cardListPosition + 1;
            }
        } else {
            if (cardListPosition == 0) {
                cardListPosition = cardList.size() - 1;
            } else {
                cardListPosition = cardListPosition - 1;
            }
        }

        loyaltyCardId = cardList.get(cardListPosition);

        // Restart activity with new card id and index
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        b.putInt(BUNDLE_ID, loyaltyCardId);
        b.putInt(BUNDLE_TRANSITION_RIGHT, transitionRight ? 1 : 0);
        intent.putExtras(b);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_IMAGEINDEX, mainImageIndex);
        savedInstanceState.putBoolean(STATE_FULLSCREEN, isFullscreen);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    protected void onResume() {
        activityOverridesNavBarColor = true;
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

        Window window = getWindow();
        if (window != null) {
            // Hide the keyboard if still shown (could be the case when returning from edit activity
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            );

            WindowManager.LayoutParams attributes = window.getAttributes();

            // The brightness value is on a scale from [0, ..., 1], where
            // '1' is the brightest. We attempt to maximize the brightness
            // to help barcode readers scan the barcode.
            if (settings.useMaxBrightnessDisplayingBarcode()) {
                attributes.screenBrightness = 1F;
            }

            if (settings.getKeepScreenOn()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            if (settings.getDisableLockscreenWhileViewingCard()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true);
                } else {
                    showWhenLockedSdkLessThan27(window);
                }
            }

            window.setAttributes(attributes);
        }

        loyaltyCard = DBHelper.getLoyaltyCard(this, database, loyaltyCardId);
        if (loyaltyCard == null) {
            Log.w(TAG, "Could not lookup loyalty card " + loyaltyCardId);
            Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(loyaltyCard.store);

        loyaltyCardGroups = DBHelper.getLoyaltyCardGroups(database, loyaltyCardId);

        showHideElementsForScreenSize();

        format = loyaltyCard.barcodeType;
        cardIdString = loyaltyCard.cardId;
        barcodeIdString = loyaltyCard.barcodeId;

        binding.mainImageDescription.setText(loyaltyCard.cardId);

        // Display full text on click in case it doesn't fit in a single line
        binding.mainImageDescription.setOnClickListener(v -> {
            if (mainImageIndex != 0) {
                // Don't show cardId dialog, we're displaying something else
                return;
            }

            TextView cardIdView = new TextView(LoyaltyCardViewActivity.this);
            cardIdView.setText(loyaltyCard.cardId);
            cardIdView.setTextIsSelectable(true);
            int contentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
            cardIdView.setPadding(contentPadding, contentPadding / 2, contentPadding, 0);

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(LoyaltyCardViewActivity.this);
            builder.setTitle(R.string.cardId);
            builder.setView(cardIdView);
            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        int backgroundHeaderColor = Utils.getHeaderColor(this, loyaltyCard);

        // Also apply colours to UI elements
        int darkenedColor = ColorUtils.blendARGB(backgroundHeaderColor, Color.BLACK, 0.1f);
        binding.barcodeScaler.setProgressTintList(ColorStateList.valueOf(darkenedColor));
        binding.barcodeScaler.setThumbTintList(ColorStateList.valueOf(darkenedColor));
        binding.barcodeWidthscaler.setProgressTintList(ColorStateList.valueOf(darkenedColor));
        binding.barcodeWidthscaler.setThumbTintList(ColorStateList.valueOf(darkenedColor));

        // Set bottomAppBar and system navigation bar color
        binding.bottomAppBar.setBackgroundColor(darkenedColor);
        Utils.setNavigationBarColor(null, window, darkenedColor, Utils.needsDarkForeground(darkenedColor));

        int complementaryColor = Utils.getComplementaryColor(darkenedColor);
        binding.fabEdit.setBackgroundTintList(ColorStateList.valueOf(complementaryColor));
        Drawable editButtonIcon = binding.fabEdit.getDrawable();
        editButtonIcon.mutate();
        editButtonIcon.setTint(Utils.needsDarkForeground(complementaryColor) ? Color.BLACK : Color.WHITE);
        binding.fabEdit.setImageDrawable(editButtonIcon);

        Bitmap icon = loyaltyCard.getImageThumbnail(this);
        Utils.setIconOrTextWithBackground(this, loyaltyCard, icon, binding.iconImage, binding.iconText, 1);

        // If the background is very bright, we should use dark icons
        backgroundNeedsDarkIcons = Utils.needsDarkForeground(backgroundHeaderColor);

        fixBottomAppBarImageButtonColor(binding.bottomAppBarInfoButton);
        fixBottomAppBarImageButtonColor(binding.bottomAppBarPreviousButton);
        fixBottomAppBarImageButtonColor(binding.bottomAppBarNextButton);
        fixBottomAppBarImageButtonColor(binding.bottomAppBarUpdateBalanceButton);
        setBottomAppBarButtonState();

        boolean isBarcodeSupported;
        if (format != null && !format.isSupported()) {
            isBarcodeSupported = false;

            Toast.makeText(this, getString(R.string.unsupportedBarcodeType), Toast.LENGTH_LONG).show();
        } else if (format == null) {
            isBarcodeSupported = false;
        } else {
            isBarcodeSupported = true;
        }

        imageTypes = new ArrayList<>();

        if (isBarcodeSupported) {
            imageTypes.add(ImageType.BARCODE);
        }

        frontImageBitmap = loyaltyCard.getImageFront(this);
        if (frontImageBitmap != null) {
            imageTypes.add(ImageType.IMAGE_FRONT);
        }

        backImageBitmap = loyaltyCard.getImageBack(this);
        if (backImageBitmap != null) {
            imageTypes.add(ImageType.IMAGE_BACK);
        }

        setStateBasedOnImageTypes();

        setFullscreen(isFullscreen);

        DBHelper.updateLoyaltyCardLastUsed(database, loyaltyCard.id);

        invalidateOptionsMenu();

        ShortcutHelper.updateShortcuts(this, loyaltyCard);
    }

    private void setStateBasedOnImageTypes() {
        // Decrease the card holder size to only fit the value if there is no barcode
        // This looks much cleaner
        ViewGroup.LayoutParams cardHolderLayoutParams = binding.cardHolder.getLayoutParams();
        if (imageTypes.isEmpty()) {
            cardHolderLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            cardHolderLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        binding.cardHolder.setLayoutParams(cardHolderLayoutParams);

        // Ensure buttons and accessibility are correct
        setMainImagePreviousNextButtons();
        setMainImageAccessibility();
    }

    @SuppressWarnings("deprecation")
    private void showWhenLockedSdkLessThan27(Window window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    private void fixBottomAppBarImageButtonColor(ImageButton imageButton) {
        imageButton.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(backgroundNeedsDarkIcons ? Color.BLACK : Color.WHITE, BlendModeCompat.SRC_ATOP));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_view_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (loyaltyCard != null) {
            // Update star status
            if (loyaltyCard.starStatus == 1) {
                menu.findItem(R.id.action_star_unstar).setIcon(R.drawable.ic_starred);
                menu.findItem(R.id.action_star_unstar).setTitle(R.string.unstar);
            } else {
                menu.findItem(R.id.action_star_unstar).setIcon(R.drawable.ic_unstarred);
                menu.findItem(R.id.action_star_unstar).setTitle(R.string.star);
            }

            // Update archive/unarchive button
            if (loyaltyCard.archiveStatus != 0) {
                menu.findItem(R.id.action_unarchive).setVisible(true);
                menu.findItem(R.id.action_archive).setVisible(false);
            } else {
                menu.findItem(R.id.action_unarchive).setVisible(false);
                menu.findItem(R.id.action_archive).setVisible(true);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.action_share) {
            try {
                importURIHelper.startShareIntent(Arrays.asList(loyaltyCard));
            } catch (UnsupportedEncodingException e) {
                Toast.makeText(LoyaltyCardViewActivity.this, R.string.failedGeneratingShareURL, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            return true;
        } else if (id == R.id.action_duplicate) {
            Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt(LoyaltyCardEditActivity.BUNDLE_ID, loyaltyCardId);
            bundle.putBoolean(LoyaltyCardEditActivity.BUNDLE_DUPLICATE_ID, true);
            intent.putExtras(bundle);
            startActivity(intent);

            return true;
        } else if (id == R.id.action_star_unstar) {
            DBHelper.updateLoyaltyCardStarStatus(database, loyaltyCardId, loyaltyCard.starStatus == 0 ? 1 : 0);

            new ListWidget().updateAll(LoyaltyCardViewActivity.this);

            // Re-init loyaltyCard with new data from DB
            onResume();
            invalidateOptionsMenu();

            return true;
        } else if (id == R.id.action_archive) {
            DBHelper.updateLoyaltyCardArchiveStatus(database, loyaltyCardId, 1);
            Toast.makeText(LoyaltyCardViewActivity.this, R.string.archived, Toast.LENGTH_LONG).show();

            ShortcutHelper.removeShortcut(LoyaltyCardViewActivity.this, loyaltyCardId);
            new ListWidget().updateAll(LoyaltyCardViewActivity.this);

            // Re-init loyaltyCard with new data from DB
            onResume();
            invalidateOptionsMenu();

            return true;
        } else if (id == R.id.action_unarchive) {
            DBHelper.updateLoyaltyCardArchiveStatus(database, loyaltyCardId, 0);
            Toast.makeText(LoyaltyCardViewActivity.this, R.string.unarchived, Toast.LENGTH_LONG).show();

            // Re-init loyaltyCard with new data from DB
            onResume();
            invalidateOptionsMenu();

            return true;
        } else if (id == R.id.action_delete) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.deleteTitle);
            builder.setMessage(R.string.deleteConfirmation);
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                Log.e(TAG, "Deleting card: " + loyaltyCardId);

                DBHelper.deleteLoyaltyCard(database, LoyaltyCardViewActivity.this, loyaltyCardId);

                ShortcutHelper.removeShortcut(LoyaltyCardViewActivity.this, loyaltyCardId);
                new ListWidget().updateAll(LoyaltyCardViewActivity.this);

                finish();
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showHideElementsForScreenSize() {
        int orientation = getResources().getConfiguration().orientation;
        // Detect square-ish screens like the Unihertz Titan
        boolean isSmallHeight = getResources().getDisplayMetrics().heightPixels < (getResources().getDisplayMetrics().widthPixels * 1.5);

        // Treat sqaure-ish screens as
        if (orientation == Configuration.ORIENTATION_LANDSCAPE || isSmallHeight) {
            Log.d(TAG, "Detected landscape mode or square-ish screen");
            binding.iconContainer.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "Detected portrait mode on non square-ish screen");
            binding.iconContainer.setVisibility(View.VISIBLE);
        }

        enableToolbarBackButton();
    }

    private void drawBarcode(boolean addPadding) {
        mTasks.flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false);

        if (format != null) {
            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(
                    getApplicationContext(),
                    barcodeRenderTarget,
                    barcodeIdString != null ? barcodeIdString : cardIdString,
                    format,
                    null,
                    false,
                    this,
                    addPadding,
                    isFullscreen);
            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
        }
    }

    private void redrawBarcodeAfterResize(boolean addPadding) {
        if (format != null) {
            barcodeRenderTarget.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            barcodeRenderTarget.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            Log.d(TAG, "ImageView size now known");
                            drawBarcode(addPadding);
                        }
                    });
        }
    }

    private void drawMainImage(int index, boolean waitForResize, boolean isFullscreen) {
        if (imageTypes.isEmpty()) {
            barcodeRenderTarget.setVisibility(View.GONE);
            binding.mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            binding.mainImageDescription.setTextColor(MaterialColors.getColor(binding.mainImageDescription, com.google.android.material.R.attr.colorOnSurfaceVariant));

            binding.mainImageDescription.setText(loyaltyCard.cardId);
            return;
        }

        ImageType wantedImageType = imageTypes.get(index);

        if (wantedImageType == ImageType.BARCODE) {
            barcodeRenderTarget.setBackgroundColor(Color.WHITE);
            binding.mainCardView.setCardBackgroundColor(Color.WHITE);
            binding.mainImageDescription.setTextColor(getResources().getColor(R.color.md_theme_light_onSurfaceVariant));

            if (waitForResize) {
                redrawBarcodeAfterResize(!isFullscreen);
            } else {
                drawBarcode(!isFullscreen);
            }

            binding.mainImageDescription.setText(loyaltyCard.cardId);
            barcodeRenderTarget.setContentDescription(getString(R.string.barcodeImageDescriptionWithType, format.prettyName()));
        } else if (wantedImageType == ImageType.IMAGE_FRONT) {
            barcodeRenderTarget.setImageBitmap(frontImageBitmap);
            barcodeRenderTarget.setBackgroundColor(Color.TRANSPARENT);
            binding.mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            binding.mainImageDescription.setTextColor(MaterialColors.getColor(binding.mainImageDescription, com.google.android.material.R.attr.colorOnSurfaceVariant));

            binding.mainImageDescription.setText(getString(R.string.frontImageDescription));
            barcodeRenderTarget.setContentDescription(getString(R.string.frontImageDescription));
        } else if (wantedImageType == ImageType.IMAGE_BACK) {
            barcodeRenderTarget.setImageBitmap(backImageBitmap);
            barcodeRenderTarget.setBackgroundColor(Color.TRANSPARENT);
            binding.mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            binding.mainImageDescription.setTextColor(MaterialColors.getColor(binding.mainImageDescription, com.google.android.material.R.attr.colorOnSurfaceVariant));

            binding.mainImageDescription.setText(getString(R.string.backImageDescription));
            barcodeRenderTarget.setContentDescription(getString(R.string.backImageDescription));
        } else {
            throw new IllegalArgumentException("Unknown image type: " + wantedImageType);
        }

        barcodeRenderTarget.setVisibility(View.VISIBLE);
    }

    private void setMainImage(boolean next, boolean overflow) {
        int newIndex = mainImageIndex + (next ? 1 : -1);

        if (newIndex >= imageTypes.size() && overflow) {
        newIndex = 0;
        }

        if (newIndex == -1 || newIndex >= imageTypes.size()) {
            return;
        }

        mainImageIndex = newIndex;

        drawMainImage(newIndex, false, isFullscreen);

        setMainImagePreviousNextButtons();
        setMainImageAccessibility();
    }

    private void setMainImageAccessibility() {
        // Single-click actions
        if (mainImageIndex == 0) {
            ViewCompat.replaceAccessibilityAction(
                    binding.mainImage,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                    getString(R.string.moveBarcodeToTopOfScreen),
                    null
            );
        } else {
            int accessibilityClickAction;
            if (mainImageIndex == 1) {
                accessibilityClickAction = R.string.openFrontImageInGalleryApp;
            } else if (mainImageIndex == 2) {
                accessibilityClickAction = R.string.openBackImageInGalleryApp;
            } else {
                throw new IndexOutOfBoundsException("setMainImageAccessibility was out of range (action_click)");
            }

            ViewCompat.replaceAccessibilityAction(
                    binding.mainImage,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                    getString(accessibilityClickAction),
                    null
            );
        }

        // Long-press actions
        int accessibilityLongPressAction;
        if (mainImageIndex == 0) {
            accessibilityLongPressAction = R.string.switchToFrontImage;
        } else if (mainImageIndex == 1) {
            accessibilityLongPressAction = R.string.switchToBackImage;
        } else if (mainImageIndex == 2) {
            accessibilityLongPressAction = R.string.switchToBarcode;
        } else {
            throw new IndexOutOfBoundsException("setMainImageAccessibility was out of range (action_long_click)");
        }

        ViewCompat.replaceAccessibilityAction(
                binding.mainImage,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                getString(accessibilityLongPressAction),
                null
        );
    }

    private void setMainImagePreviousNextButtons() {
        // Ensure the main image index is valid. After a card update, some images (front/back/barcode)
        // may have been removed, so the index should not exceed the number of available images.
        if(mainImageIndex > imageTypes.size() - 1){
            mainImageIndex = 0;
        }

        if (imageTypes.size() < 2) {
            binding.mainLeftButton.setVisibility(View.INVISIBLE);
            binding.mainRightButton.setVisibility(View.INVISIBLE);

            binding.mainLeftButton.setOnClickListener(null);
            binding.mainRightButton.setOnClickListener(null);

            return;
        }

        final ImageButton prevButton;
        final ImageButton nextButton;

        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            prevButton = binding.mainRightButton;
            nextButton = binding.mainLeftButton;
        } else {
            prevButton = binding.mainLeftButton;
            nextButton = binding.mainRightButton;
        }

        // Enable left button if we can go further left
        if (mainImageIndex > 0) {
            prevButton.setVisibility(View.VISIBLE);
            prevButton.setOnClickListener(view -> setMainImage(false, false));
        } else {
            prevButton.setVisibility(View.INVISIBLE);
            prevButton.setOnClickListener(null);
        }

        // Enable right button if we can go further right
        if (mainImageIndex < (imageTypes.size() - 1)) {
            nextButton.setVisibility(View.VISIBLE);
            nextButton.setOnClickListener(view -> setMainImage(true, false));
        } else {
            nextButton.setVisibility(View.INVISIBLE);
            nextButton.setOnClickListener(null);
        }
    }

    /**
     * When enabled, hides the status bar and moves the barcode to the top of the screen.
     * <p>
     * The purpose of this function is to make sure the barcode can be scanned from the phone
     * by machines which offer no space to insert the complete device.
     */
    private void setFullscreen(boolean enabled) {
        ActionBar actionBar = getSupportActionBar();
        isFullscreen = enabled;

        if (enabled && !imageTypes.isEmpty()) {
            Log.d(TAG, "Move into fullscreen");

            barcodeRenderTarget = binding.fullscreenImage;

            // Show only fullscreen view
            binding.container.setVisibility(View.GONE);
            binding.fullscreenLayout.setVisibility(View.VISIBLE);

            // Only show width slider if the barcode isn't square (square barcodes will resize height and width together)
            // or if the internals of the barcode are squares, like DATA_MATRIX
            binding.setWidthLayout.setVisibility((format.isSquare() || format.format() == BarcodeFormat.DATA_MATRIX) ? View.GONE : View.VISIBLE);

            drawMainImage(mainImageIndex, true, isFullscreen);

            binding.barcodeScaler.setProgress(loyaltyCard.zoomLevel);
            setScalerGuideline(loyaltyCard.zoomLevel);

            binding.barcodeWidthscaler.setProgress(loyaltyCard.zoomLevelWidth);
            setScalerWidthGuideline(loyaltyCard.zoomLevelWidth);

            // Hide actionbar
            if (actionBar != null) {
                actionBar.hide();
            }

            // Hide other UI elements
            binding.bottomAppBar.setVisibility(View.GONE);
            binding.fabEdit.setVisibility(View.GONE);

            // Set Android to fullscreen mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Window window = getWindow();
                if (window != null) {
                    window.setDecorFitsSystemWindows(false);
                    WindowInsetsController wic = window.getInsetsController();
                    if (wic != null) {
                        wic.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                        wic.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                }
            } else {
                setFullscreenModeSdkLessThan30();
            }
        } else {
            Log.d(TAG, "Move out of fullscreen");

            barcodeRenderTarget = binding.mainImage;

            // Show only regular view
            binding.container.setVisibility(View.VISIBLE);
            binding.fullscreenLayout.setVisibility(View.GONE);

            drawMainImage(mainImageIndex, true, isFullscreen);

            // Show actionbar
            if (actionBar != null) {
                actionBar.show();
            }

            // Show other UI elements
            binding.bottomAppBar.setVisibility(View.VISIBLE);
            binding.fabEdit.setVisibility(View.VISIBLE);

            // Unset fullscreen mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Window window = getWindow();
                if (window != null) {
                    window.setDecorFitsSystemWindows(true);
                    WindowInsetsController wic = window.getInsetsController();
                    if (wic != null) {
                        wic.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                        wic.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
                    }
                }
            } else {
                unsetFullscreenModeSdkLessThan30();
            }
        }

        Log.d("setFullScreen", "Is full screen enabled? " + enabled + " Zoom Level = " + binding.barcodeScaler.getProgress());
    }

    @SuppressWarnings("deprecation")
    private void unsetFullscreenModeSdkLessThan30() {
        Window window = getWindow();
        if (window != null) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            & ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            & ~View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    @SuppressWarnings("deprecation")
    private void setFullscreenModeSdkLessThan30() {
        Window window = getWindow();
        if (window != null) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
}
