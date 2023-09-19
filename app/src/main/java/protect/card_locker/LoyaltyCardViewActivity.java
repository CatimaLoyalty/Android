package protect.card_locker;

import android.content.ActivityNotFoundException;
import android.content.Context;
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
import android.util.TypedValue;
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

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
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

    final private TaskHandler mTasks = new TaskHandler();
    Runnable barcodeImageGenerationFinishedCallback;

    public void onMainImageTap() {
        // If we're in fullscreen, leave fullscreen
        if (isFullscreen) {
            setFullscreen(false);
            return;
        }

        // If the barcode is shown, switch to fullscreen layout
        if (imageTypes.get(mainImageIndex) == ImageType.BARCODE) {
            setFullscreen(true);
            return;
        }

        // If this is an image, open it in the gallery.
        openCurrentMainImageInGallery();
    }

    private void openCurrentMainImageInGallery() {
        ImageType wantedImageType = imageTypes.get(mainImageIndex);

        File file = null;

        switch (wantedImageType) {
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
        BARCODE,
        IMAGE_FRONT,
        IMAGE_BACK
    }

    private void extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt("id") : 0;
        cardList = b != null ? b.getIntegerArrayList("cardList") : null;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Bundle incomingIntentExtras = getIntent().getExtras();

            if (incomingIntentExtras == null) {
                Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            int transitionRight = incomingIntentExtras.getInt("transition_right", -1);
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
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        settings = new Settings(this);

        String cardOrientation = settings.getCardViewOrientation();
        if (cardOrientation.equals(getString(R.string.settings_key_lock_on_opening_orientation))) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        } else if (cardOrientation.equals(getString(R.string.settings_key_portrait_orientation))) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (cardOrientation.equals(getString(R.string.settings_key_landscape_orientation))) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (savedInstanceState != null) {
            mainImageIndex = savedInstanceState.getInt(STATE_IMAGEINDEX);
            isFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN);
        }

        extractIntentFields(getIntent());

        setContentView(binding.getRoot());

        database = new DBHelper(this).getWritableDatabase();
        importURIHelper = new ImportURIHelper(this);

        binding.barcodeScaler.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    Log.d(TAG, "non user triggered onProgressChanged, ignoring, progress is " + progress);
                    return;
                }
                Log.d(TAG, "Progress is " + progress);
                Log.d(TAG, "Max is " + binding.barcodeScaler.getMax());
                float scale = (float) progress / (float) binding.barcodeScaler.getMax();
                Log.d(TAG, "Scaling to " + scale);

                loyaltyCard.zoomLevel = progress;
                DBHelper.updateLoyaltyCardZoomLevel(database, loyaltyCardId, loyaltyCard.zoomLevel);

                setScalerGuideline(loyaltyCard.zoomLevel);

                drawMainImage(mainImageIndex, true, isFullscreen);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

        binding.iconContainer.setOnClickListener(view -> Toast.makeText(LoyaltyCardViewActivity.this, R.string.icon_header_click_text, Toast.LENGTH_LONG).show());
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
        builder.setTitle(R.string.updateBalanceTitle);
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

        TextView updateTextView = new TextView(this);
        updateTextView.setText(getString(R.string.newBalanceSentence, Utils.formatBalance(this, loyaltyCard.balance, loyaltyCard.balanceType)));
        layout.addView(updateTextView);

        final TextInputEditText input = new TextInputEditText(this);
        Context dialogContext = this;
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789,."));
        input.setHint(R.string.updateBalanceHint);
        input.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                BigDecimal newBalance;
                try {
                    newBalance = calculateNewBalance(loyaltyCard.balance, loyaltyCard.balanceType, s.toString());
                } catch (ParseException e) {
                    input.setTag(null);
                    updateTextView.setText(getString(R.string.newBalanceSentence, Utils.formatBalance(dialogContext, loyaltyCard.balance, loyaltyCard.balanceType)));
                    return;
                }

                // Save new balance into this element
                input.setTag(newBalance);
                updateTextView.setText(getString(R.string.newBalanceSentence, Utils.formatBalance(dialogContext, newBalance, loyaltyCard.balanceType)));
            }
        });
        layout.addView(input);
        layout.setLayoutParams(params);
        container.addView(layout);

        builder.setView(container);
        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            // Grab calculated balance from input field
            BigDecimal newBalance = (BigDecimal) input.getTag();
            if (newBalance == null) {
                return;
            }

            // Actually update balance
            DBHelper.updateLoyaltyCardBalance(database, loyaltyCardId, newBalance);
            // Reload UI
            this.onResume();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, Currency currency, String unparsedSubtraction) throws ParseException {
        BigDecimal subtraction = Utils.parseBalance(unparsedSubtraction, currency);
        return currentBalance.subtract(subtraction).max(new BigDecimal(0));
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
        b.putInt("id", loyaltyCardId);
        b.putInt("transition_right", transitionRight ? 1 : 0);
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
    public void onResume() {
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

        loyaltyCard = DBHelper.getLoyaltyCard(database, loyaltyCardId);
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

        binding.cardIdView.setText(loyaltyCard.cardId);

        // Display full text on click in case it doesn't fit in a single line
        binding.cardIdView.setOnClickListener(v -> {
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
        binding.bottomAppBar.setBackgroundColor(darkenedColor);
        int complementaryColor = Utils.getComplementaryColor(darkenedColor);
        binding.fabEdit.setBackgroundTintList(ColorStateList.valueOf(complementaryColor));
        Drawable editButtonIcon = binding.fabEdit.getDrawable();
        editButtonIcon.mutate();
        editButtonIcon.setTint(Utils.needsDarkForeground(complementaryColor) ? Color.BLACK : Color.WHITE);
        binding.fabEdit.setImageDrawable(editButtonIcon);

        Bitmap icon = Utils.retrieveCardImage(this, loyaltyCard.id, ImageLocationType.icon);
        Utils.setIconOrTextWithBackground(this, loyaltyCard, icon, binding.iconImage, binding.iconText);

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

        frontImageBitmap = Utils.retrieveCardImage(this, loyaltyCard.id, ImageLocationType.front);
        if (frontImageBitmap != null) {
            imageTypes.add(ImageType.IMAGE_FRONT);
        }

        backImageBitmap = Utils.retrieveCardImage(this, loyaltyCard.id, ImageLocationType.back);
        if (backImageBitmap != null) {
            imageTypes.add(ImageType.IMAGE_BACK);
        }

        setStateBasedOnImageTypes();

        setFullscreen(isFullscreen);

        DBHelper.updateLoyaltyCardLastUsed(database, loyaltyCard.id);

        invalidateOptionsMenu();
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
    public void onBackPressed() {
        if (isFullscreen) {
            setFullscreen(false);
            return;
        }

        super.onBackPressed();
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

            // Re-init loyaltyCard with new data from DB
            onResume();
            invalidateOptionsMenu();

            return true;
        } else if (id == R.id.action_archive) {
            DBHelper.updateLoyaltyCardArchiveStatus(database, loyaltyCardId, 1);
            Toast.makeText(LoyaltyCardViewActivity.this, R.string.archived, Toast.LENGTH_LONG).show();

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
                    addPadding);
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
            binding.cardIdView.setTextColor(MaterialColors.getColor(binding.cardIdView, com.google.android.material.R.attr.colorOnSurfaceVariant));
            return;
        }

        ImageType wantedImageType = imageTypes.get(index);

        if (wantedImageType == ImageType.BARCODE) {
            barcodeRenderTarget.setBackgroundColor(Color.WHITE);
            binding.mainCardView.setCardBackgroundColor(Color.WHITE);
            binding.cardIdView.setTextColor(getResources().getColor(R.color.md_theme_light_onSurfaceVariant));

            if (waitForResize) {
                redrawBarcodeAfterResize(!isFullscreen);
            } else {
                drawBarcode(!isFullscreen);
            }

            barcodeRenderTarget.setContentDescription(getString(R.string.barcodeImageDescriptionWithType, format.prettyName()));
        } else if (wantedImageType == ImageType.IMAGE_FRONT) {
            barcodeRenderTarget.setImageBitmap(frontImageBitmap);
            barcodeRenderTarget.setBackgroundColor(Color.TRANSPARENT);
            binding.mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            binding.cardIdView.setTextColor(MaterialColors.getColor(binding.cardIdView, com.google.android.material.R.attr.colorOnSurfaceVariant));
            barcodeRenderTarget.setContentDescription(getString(R.string.frontImageDescription));
        } else if (wantedImageType == ImageType.IMAGE_BACK) {
            barcodeRenderTarget.setImageBitmap(backImageBitmap);
            barcodeRenderTarget.setBackgroundColor(Color.TRANSPARENT);
            binding.mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            binding.cardIdView.setTextColor(MaterialColors.getColor(binding.cardIdView, com.google.android.material.R.attr.colorOnSurfaceVariant));
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

            drawMainImage(mainImageIndex, true, isFullscreen);

            binding.barcodeScaler.setProgress(loyaltyCard.zoomLevel);
            setScalerGuideline(loyaltyCard.zoomLevel);

            // Hide actionbar
            if (actionBar != null) {
                actionBar.hide();
            }

            // Hide other UI elements
            binding.bottomAppBar.setVisibility(View.GONE);
            binding.fabEdit.setVisibility(View.GONE);

            // Set Android to fullscreen mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
                if (getWindow().getInsetsController() != null) {
                    getWindow().getInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
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
                getWindow().setDecorFitsSystemWindows(true);
                if (getWindow().getInsetsController() != null) {
                    getWindow().getInsetsController().show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
                }
            } else {
                unsetFullscreenModeSdkLessThan30();
            }
        }

        Log.d("setFullScreen", "Is full screen enabled? " + enabled + " Zoom Level = " + binding.barcodeScaler.getProgress());
    }

    @SuppressWarnings("deprecation")
    private void unsetFullscreenModeSdkLessThan30() {
        getWindow().getDecorView().setSystemUiVisibility(
                getWindow().getDecorView().getSystemUiVisibility()
                        & ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        & ~View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @SuppressWarnings("deprecation")
    private void setFullscreenModeSdkLessThan30() {
        getWindow().getDecorView().setSystemUiVisibility(
                getWindow().getDecorView().getSystemUiVisibility()
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
}
