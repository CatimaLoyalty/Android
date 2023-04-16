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
import android.graphics.Outline;
import android.graphics.Rect;
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
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
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
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class LoyaltyCardViewActivity extends CatimaAppCompatActivity implements GestureDetector.OnGestureListener {
    private LoyaltyCardViewLayoutBinding binding;
    private static final String TAG = "Catima";

    private GestureDetector mGestureDetector;

    CoordinatorLayout coordinatorLayout;
    ConstraintLayout mainLayout;
    TextView cardIdFieldView;
    BottomAppBar bottomAppBar;
    ImageButton bottomAppBarInfoButton;
    ImageButton bottomAppBarPreviousButton;
    ImageButton bottomAppBarNextButton;
    ImageButton bottomAppBarUpdateBalanceButton;
    AppCompatTextView storeName;
    ImageButton maximizeButton;
    ImageView mainImage;
    LinearLayout dotIndicator;
    ImageButton minimizeButton;
    View collapsingToolbarLayout;
    AppBarLayout appBarLayout;
    ImageView iconImage;
    Toolbar portraitToolbar;
    Toolbar landscapeToolbar;

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

    FloatingActionButton editButton;

    Guideline centerGuideline;
    SeekBar barcodeScaler;

    Bitmap frontImageBitmap;
    Bitmap backImageBitmap;

    boolean starred;
    boolean backgroundNeedsDarkIcons;
    boolean isFullscreen = false;
    int mainImageIndex = 0;
    List<ImageType> imageTypes;
    private ImageView[] dots;
    boolean isBarcodeSupported = true;

    static final String STATE_IMAGEINDEX = "imageIndex";
    static final String STATE_FULLSCREEN = "isFullscreen";

    private final int HEADER_FILTER_ALPHA = 127;

    final private TaskHandler mTasks = new TaskHandler();
    Runnable barcodeImageGenerationFinishedCallback;

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (imageTypes.size() > 1) {
            Toast.makeText(this, getString(R.string.swipeToSwitchImages), Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
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
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "On fling");

        if (Math.abs(velocityY) > (0.75 * Math.abs(velocityX))) {
            // Vertical swipe
            // Swipe up
            if (velocityY < -150) {
                if (!isFullscreen) {
                    setFullscreen(true);
                }
                return false;
            }

            // Swipe down
            if (velocityY > 150) {
                if (isFullscreen) {
                    setFullscreen(false);
                }
                return false;
            }
        } else if (Math.abs(velocityX) > (0.75 * Math.abs(velocityY))) {
            // Horizontal swipe
            // Swipe right
            if (velocityX < -150) {
                setMainImage(true, false);
                return false;
            }

            // Swipe left
            if (velocityX > 150) {
                setMainImage(false, false);
                return false;
            }
        }

        if (imageTypes.size() > 1) {
            Toast.makeText(this, getString(R.string.swipeToSwitchImages), Toast.LENGTH_SHORT).show();
        }

        return true;
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

    private Drawable getDotIcon(boolean active, boolean darkMode) {
        Drawable unwrappedIcon = AppCompatResources.getDrawable(this, active ? R.drawable.active_dot : R.drawable.inactive_dot);
        assert unwrappedIcon != null;
        Drawable wrappedIcon = DrawableCompat.wrap(unwrappedIcon);
        if (darkMode) {
            DrawableCompat.setTint(wrappedIcon, Color.WHITE);
        } else {
            DrawableCompat.setTint(wrappedIcon, Color.BLACK);
        }

        return wrappedIcon;
    }

    private Drawable getIcon(int icon, boolean dark) {
        Drawable unwrappedIcon = AppCompatResources.getDrawable(this, icon);
        assert unwrappedIcon != null;
        Drawable wrappedIcon = DrawableCompat.wrap(unwrappedIcon);
        wrappedIcon.mutate();
        if (dark) {
            DrawableCompat.setTint(wrappedIcon, Color.BLACK);
        } else {
            DrawableCompat.setTintList(wrappedIcon, null);
        }

        return wrappedIcon;
    }

    private void setCenterGuideline(int zoomLevel) {
        float scale = zoomLevel / 100f;

        if (format != null && format.isSquare()) {
            centerGuideline.setGuidelinePercent(0.75f * scale);
        } else {
            centerGuideline.setGuidelinePercent(0.5f * scale);
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

        coordinatorLayout = binding.coordinatorLayout;
        mainLayout = binding.mainLayout;
        cardIdFieldView = binding.cardIdView;
        storeName = binding.storeName;
        maximizeButton = binding.maximizeButton;
        mainImage = binding.mainImage;
        mainImage.setClipToOutline(true);
        dotIndicator = binding.dotIndicator;
        minimizeButton = binding.minimizeButton;
        collapsingToolbarLayout = binding.collapsingToolbarLayout;
        appBarLayout = binding.appBarLayout;
        bottomAppBar = binding.bottomAppBar;
        iconImage = binding.iconImage;
        portraitToolbar = binding.toolbar;
        landscapeToolbar = binding.toolbarLandscape;

        bottomAppBarInfoButton = binding.buttonShowInfo;
        bottomAppBarPreviousButton = binding.buttonPrevious;
        bottomAppBarNextButton = binding.buttonNext;
        bottomAppBarUpdateBalanceButton = binding.buttonUpdateBalance;

        barcodeImageGenerationFinishedCallback = () -> {
            if (!(boolean) mainImage.getTag()) {
                mainImage.setVisibility(View.GONE);
                imageTypes.remove(ImageType.BARCODE);

                // Redraw UI
                setDotIndicator(Utils.isDarkModeEnabled(LoyaltyCardViewActivity.this));
                setFullscreen(isFullscreen);

                Toast.makeText(LoyaltyCardViewActivity.this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
            }
        };

        centerGuideline = binding.centerGuideline;
        barcodeScaler = binding.barcodeScaler;
        barcodeScaler.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    Log.d(TAG, "non user triggered onProgressChanged, ignoring, progress is " + progress);
                    return;
                }
                Log.d(TAG, "Progress is " + progress);
                Log.d(TAG, "Max is " + barcodeScaler.getMax());
                float scale = (float) progress / (float) barcodeScaler.getMax();
                Log.d(TAG, "Scaling to " + scale);

                loyaltyCard.zoomLevel = progress;
                DBHelper.updateLoyaltyCardZoomLevel(database, loyaltyCardId, loyaltyCard.zoomLevel);

                setCenterGuideline(loyaltyCard.zoomLevel);

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

        // Allow making barcode fullscreen on tap
        maximizeButton.setOnClickListener(v -> setFullscreen(true));
        minimizeButton.setOnClickListener(v -> setFullscreen(false));

        editButton = binding.fabEdit;
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("id", loyaltyCardId);
            bundle.putBoolean("update", true);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        });
        editButton.bringToFront();

        appBarLayout.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                ViewOutlineProvider.BACKGROUND.getOutline(view, outline);
                outline.setAlpha(0f);
            }
        });

        bottomAppBarInfoButton.setOnClickListener(view -> showInfoDialog());
        bottomAppBarPreviousButton.setOnClickListener(view -> prevNextCard(false));
        bottomAppBarNextButton.setOnClickListener(view -> prevNextCard(true));
        bottomAppBarUpdateBalanceButton.setOnClickListener(view -> showBalanceUpdateDialog());

        mGestureDetector = new GestureDetector(this, this);
        View.OnTouchListener gestureTouchListener = (v, event) -> mGestureDetector.onTouchEvent(event);
        mainImage.setOnTouchListener(gestureTouchListener);

        appBarLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                iconImage.setLayoutParams(new CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.MATCH_PARENT, appBarLayout.getHeight())
                );
                iconImage.setClipBounds(new Rect(left, top, right, bottom));
            }
        });
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
        int dialogTitlePadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_title_padding);
        TextView infoTitleView = new TextView(this);
        infoTitleView.setPadding(
                dialogContentPadding,
                dialogContentPadding,
                dialogContentPadding,
                dialogTitlePadding
        );
        infoTitleView.setTextSize(settings.getFontSizeMax(settings.getMediumFont()));
        infoTitleView.setText(loyaltyCard.store);
        infoDialog.setCustomTitle(infoTitleView);
        infoDialog.setTitle(loyaltyCard.store);

        TextView infoTextview = new TextView(this);
        infoTextview.setPadding(
                dialogContentPadding,
                0,
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
            bottomAppBarInfoButton.setVisibility(View.VISIBLE);
        } else {
            bottomAppBarInfoButton.setVisibility(View.GONE);
        }

        if (cardList == null || cardList.size() == 1) {
            bottomAppBarPreviousButton.setVisibility(View.GONE);
            bottomAppBarNextButton.setVisibility(View.GONE);
        } else {
            bottomAppBarPreviousButton.setVisibility(View.VISIBLE);
            bottomAppBarNextButton.setVisibility(View.VISIBLE);
        }

        bottomAppBarUpdateBalanceButton.setVisibility(hasBalance(loyaltyCard) ? View.VISIBLE : View.GONE);
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

        // The brightness value is on a scale from [0, ..., 1], where
        // '1' is the brightest. We attempt to maximize the brightness
        // to help barcode readers scan the barcode.
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams attributes = window.getAttributes();

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

        loyaltyCardGroups = DBHelper.getLoyaltyCardGroups(database, loyaltyCardId);

        setupOrientation();

        format = loyaltyCard.barcodeType;
        cardIdString = loyaltyCard.cardId;
        barcodeIdString = loyaltyCard.barcodeId;

        cardIdFieldView.setText(loyaltyCard.cardId);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(cardIdFieldView,
                settings.getFontSizeMin(settings.getLargeFont()), settings.getFontSizeMax(settings.getLargeFont()),
                1, TypedValue.COMPLEX_UNIT_SP);

        storeName.setText(loyaltyCard.store);
        storeName.setTextSize(settings.getFontSizeMax(settings.getLargeFont()));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                storeName,
                settings.getFontSizeMin(settings.getLargeFont()),
                settings.getFontSizeMax(settings.getLargeFont()),
                1,
                TypedValue.COMPLEX_UNIT_DIP);

        int backgroundHeaderColor;
        if (loyaltyCard.headerColor != null) {
            backgroundHeaderColor = loyaltyCard.headerColor;
        } else {
            backgroundHeaderColor = LetterBitmap.getDefaultColor(this, loyaltyCard.store);
        }

        int textColor;
        if (Utils.needsDarkForeground(backgroundHeaderColor)) {
            textColor = Color.BLACK;
        } else {
            textColor = Color.WHITE;
        }
        storeName.setTextColor(textColor);
        landscapeToolbar.setTitleTextColor(textColor);

        // Also apply colours to UI elements
        int darkenedColor = ColorUtils.blendARGB(backgroundHeaderColor, Color.BLACK, 0.1f);
        barcodeScaler.setProgressTintList(ColorStateList.valueOf(darkenedColor));
        barcodeScaler.setThumbTintList(ColorStateList.valueOf(darkenedColor));
        maximizeButton.setBackgroundColor(darkenedColor);
        minimizeButton.setBackgroundColor(darkenedColor);
        bottomAppBar.setBackgroundColor(darkenedColor);
        maximizeButton.setColorFilter(textColor);
        minimizeButton.setColorFilter(textColor);
        int complementaryColor = Utils.getComplementaryColor(darkenedColor);
        editButton.setBackgroundTintList(ColorStateList.valueOf(complementaryColor));
        Drawable editButtonIcon = editButton.getDrawable();
        editButtonIcon.mutate();
        editButtonIcon.setTint(Utils.needsDarkForeground(complementaryColor) ? Color.BLACK : Color.WHITE);
        editButton.setImageDrawable(editButtonIcon);

        Bitmap icon = Utils.retrieveCardImage(this, loyaltyCard.id, ImageLocationType.icon);
        if (icon != null) {
            int backgroundAlphaColor = Utils.needsDarkForeground(backgroundHeaderColor) ? Color.WHITE : Color.BLACK;
            Log.d("onResume", "setting icon image");
            iconImage.setImageBitmap(icon);
            int backgroundWithAlpha = Color.argb(HEADER_FILTER_ALPHA, Color.red(backgroundAlphaColor), Color.green(backgroundAlphaColor), Color.blue(backgroundAlphaColor));
            // for images that has alpha
            appBarLayout.setBackgroundColor(backgroundWithAlpha);
        } else {
            Bitmap plain = Bitmap.createBitmap(new int[]{backgroundHeaderColor}, 1, 1, Bitmap.Config.ARGB_8888);
            iconImage.setImageBitmap(plain);
            appBarLayout.setBackgroundColor(Color.TRANSPARENT);
        }

        // If the background is very bright, we should use dark icons
        backgroundNeedsDarkIcons = Utils.needsDarkForeground(backgroundHeaderColor);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(getIcon(R.drawable.home_arrow_back_white, backgroundNeedsDarkIcons));
        }

        fixImageButtonColor(bottomAppBarInfoButton);
        fixImageButtonColor(bottomAppBarPreviousButton);
        fixImageButtonColor(bottomAppBarNextButton);
        fixImageButtonColor(bottomAppBarUpdateBalanceButton);
        setBottomAppBarButtonState();

        // Make notification area light if dark icons are needed
        if (Build.VERSION.SDK_INT >= 23) {
            View decorView = getWindow().getDecorView();
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), decorView);
            wic.setAppearanceLightStatusBars(backgroundNeedsDarkIcons);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            // Darken statusbar if icons won't be visible otherwise
            window.setStatusBarColor(backgroundNeedsDarkIcons ? ColorUtils.blendARGB(backgroundHeaderColor, Color.BLACK, 0.15f) : Color.TRANSPARENT);
        }

        // Set shadow colour of store text so even same color on same color would be readable
        storeName.setShadowLayer(1, 1, 1, backgroundNeedsDarkIcons ? Color.BLACK : Color.WHITE);

        if (format != null && !format.isSupported()) {
            isBarcodeSupported = false;

            Toast.makeText(this, getString(R.string.unsupportedBarcodeType), Toast.LENGTH_LONG).show();
        } else if (format == null) {
            isBarcodeSupported = false;
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

        setDotIndicator(Utils.isDarkModeEnabled(this));

        setFullscreen(isFullscreen);

        DBHelper.updateLoyaltyCardLastUsed(database, loyaltyCard.id);
    }

    @SuppressWarnings("deprecation")
    private void showWhenLockedSdkLessThan27(Window window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    private void fixImageButtonColor(ImageButton imageButton) {
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
        starred = loyaltyCard.starStatus != 0;

        if (loyaltyCard.archiveStatus != 0) {
            menu.findItem(R.id.action_unarchive).setVisible(true);
            menu.findItem(R.id.action_archive).setVisible(false);
        } else {
            menu.findItem(R.id.action_unarchive).setVisible(false);
            menu.findItem(R.id.action_archive).setVisible(true);
        }

        menu.findItem(R.id.action_overflow).setIcon(getIcon(R.drawable.ic_overflow_menu, backgroundNeedsDarkIcons));
        menu.findItem(R.id.action_share).setIcon(getIcon(R.drawable.ic_share_white, backgroundNeedsDarkIcons));

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (starred) {
            menu.findItem(R.id.action_star_unstar).setIcon(getIcon(R.drawable.ic_starred_white, backgroundNeedsDarkIcons));
            menu.findItem(R.id.action_star_unstar).setTitle(R.string.unstar);
        } else {
            menu.findItem(R.id.action_star_unstar).setIcon(getIcon(R.drawable.ic_unstarred_white, backgroundNeedsDarkIcons));
            menu.findItem(R.id.action_star_unstar).setTitle(R.string.star);
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
            bundle.putInt("id", loyaltyCardId);
            bundle.putBoolean("duplicateId", true);
            intent.putExtras(bundle);
            startActivity(intent);

            return true;
        } else if (id == R.id.action_star_unstar) {
            starred = !starred;
            DBHelper.updateLoyaltyCardStarStatus(database, loyaltyCardId, starred ? 1 : 0);

            // Re-init loyaltyCard with new data from DB
            onResume();

            return true;
        } else if (id == R.id.action_archive) {
            DBHelper.updateLoyaltyCardArchiveStatus(database, loyaltyCardId, 1);
            Toast.makeText(LoyaltyCardViewActivity.this, R.string.archived, Toast.LENGTH_LONG).show();

            // Re-init loyaltyCard with new data from DB
            onResume();

            return true;
        } else if (id == R.id.action_unarchive) {
            DBHelper.updateLoyaltyCardArchiveStatus(database, loyaltyCardId, 0);
            Toast.makeText(LoyaltyCardViewActivity.this, R.string.unarchived, Toast.LENGTH_LONG).show();

            // Re-init loyaltyCard with new data from DB
            onResume();

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

    private void setupOrientation() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "Detected landscape mode");
            setTitle(loyaltyCard.store);

            collapsingToolbarLayout.setVisibility(View.GONE);
            portraitToolbar.setVisibility(View.GONE);
            landscapeToolbar.setVisibility(View.VISIBLE);

            setSupportActionBar(landscapeToolbar);
        } else {
            Log.d(TAG, "Detected portrait mode");

            setTitle("");

            collapsingToolbarLayout.setVisibility(View.VISIBLE);
            portraitToolbar.setVisibility(View.VISIBLE);
            landscapeToolbar.setVisibility(View.GONE);

            setSupportActionBar(portraitToolbar);
        }

        enableToolbarBackButton();
    }

    private void drawBarcode(boolean addPadding) {
        mTasks.flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false);
        if (format != null) {
            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(
                    getApplicationContext(),
                    mainImage,
                    barcodeIdString != null ? barcodeIdString : cardIdString,
                    format,
                    null,
                    false,
                    barcodeImageGenerationFinishedCallback,
                    addPadding);
            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
        }
    }

    private void redrawBarcodeAfterResize(boolean addPadding) {
        if (format != null) {
            mainImage.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mainImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            Log.d(TAG, "ImageView size now known");
                            drawBarcode(addPadding);
                        }
                    });
        }
    }

    private void drawMainImage(int index, boolean waitForResize, boolean isFullscreen) {
        if (imageTypes.isEmpty()) {
            mainImage.setVisibility(View.GONE);
            return;
        }

        if (dots != null) {
            boolean darkMode = Utils.isDarkModeEnabled(getApplicationContext());
            for (int i = 0; i < dots.length; i++) {
                dots[i].setImageDrawable(getDotIcon(i == index, darkMode));
            }
        }

        ImageType wantedImageType = imageTypes.get(index);

        if (wantedImageType == ImageType.BARCODE) {
            // Use border in non-fullscreen mode
            if (!isFullscreen) {
                mainImage.setBackground(AppCompatResources.getDrawable(this, R.drawable.round_outline));
            } else {
                mainImage.setBackgroundColor(Color.WHITE);
            }

            if (waitForResize) {
                redrawBarcodeAfterResize(!isFullscreen);
            } else {
                drawBarcode(!isFullscreen);
            }

            mainImage.setContentDescription(getString(R.string.barcodeImageDescriptionWithType, format.prettyName()));
        } else if (wantedImageType == ImageType.IMAGE_FRONT) {
            mainImage.setImageBitmap(frontImageBitmap);
            mainImage.setBackgroundColor(Color.TRANSPARENT);
            mainImage.setContentDescription(getString(R.string.frontImageDescription));
        } else if (wantedImageType == ImageType.IMAGE_BACK) {
            mainImage.setImageBitmap(backImageBitmap);
            mainImage.setBackgroundColor(Color.TRANSPARENT);
            mainImage.setContentDescription(getString(R.string.backImageDescription));
        } else {
            throw new IllegalArgumentException("Unknown image type: " + wantedImageType);
        }

        mainImage.setVisibility(View.VISIBLE);
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
    }

    private void setDotIndicator(boolean darkMode) {
        dotIndicator.removeAllViews();
        if (imageTypes.size() >= 2) {
            dots = new ImageView[imageTypes.size()];

            for (int i = 0; i < imageTypes.size(); i++) {
                dots[i] = new ImageView(this);
                dots[i].setImageDrawable(getDotIcon(false, darkMode));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(8, 0, 8, 0);

                dotIndicator.addView(dots[i], params);
            }

            dotIndicator.setVisibility(View.VISIBLE);
        }
    }

    /**
     * When enabled, hides the status bar and moves the barcode to the top of the screen.
     * <p>
     * The purpose of this function is to make sure the barcode can be scanned from the phone
     * by machines which offer no space to insert the complete device.
     */
    private void setFullscreen(boolean enabled) {
        isFullscreen = enabled;
        ActionBar actionBar = getSupportActionBar();

        if (enabled && !imageTypes.isEmpty()) {
            Log.d(TAG, "Move into fullscreen");

            drawMainImage(mainImageIndex, true, isFullscreen);

            barcodeScaler.setProgress(loyaltyCard.zoomLevel);
            setCenterGuideline(loyaltyCard.zoomLevel);

            // Hide maximize and show minimize button and scaler
            maximizeButton.setVisibility(View.GONE);
            minimizeButton.setVisibility(View.VISIBLE);
            barcodeScaler.setVisibility(View.VISIBLE);

            // Hide actionbar
            if (actionBar != null) {
                actionBar.hide();
            }

            // Hide toolbars
            appBarLayout.setVisibility(View.INVISIBLE);
            iconImage.setVisibility(View.INVISIBLE);
            collapsingToolbarLayout.setVisibility(View.GONE);
            landscapeToolbar.setVisibility(View.GONE);

            // Hide other UI elements
            cardIdFieldView.setVisibility(View.GONE);
            bottomAppBar.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);

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

            // Reset center guideline
            setCenterGuideline(100);

            drawMainImage(mainImageIndex, true, isFullscreen);

            // Show maximize and hide minimize button and scaler
            maximizeButton.setVisibility(imageTypes.isEmpty() ? View.GONE : View.VISIBLE);

            minimizeButton.setVisibility(View.GONE);
            barcodeScaler.setVisibility(View.GONE);

            // Show actionbar
            if (actionBar != null) {
                actionBar.show();
            }

            // Show appropriate toolbar
            appBarLayout.setVisibility(View.VISIBLE);
            setupOrientation();
            iconImage.setVisibility(View.VISIBLE);

            // Show other UI elements
            cardIdFieldView.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
            bottomAppBar.setVisibility(View.VISIBLE);

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

        Log.d("setFullScreen", "Is full screen enabled? " + enabled + " Zoom Level = " + barcodeScaler.getProgress());
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
