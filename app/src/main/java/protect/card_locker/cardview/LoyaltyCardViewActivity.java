package protect.card_locker.cardview;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import protect.card_locker.*;
import protect.card_locker.databinding.LoyaltyCardViewLayoutBinding;
import protect.card_locker.preferences.Settings;
import protect.card_locker.preferences.SettingsActivity;

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
    Charset barcodeEncoding;

    Bitmap frontImageBitmap;
    Bitmap backImageBitmap;

    boolean backgroundNeedsDarkIcons;
    boolean isFullscreen = false;
    ImageView barcodeRenderTarget;
    LoyaltyCardImageNavigator cardNavigator = new LoyaltyCardImageNavigator(new ArrayList<>(), 0);
    private LoyaltyCardMainImageRenderer mainImageRenderer;
    private final LoyaltyCardViewDialogs dialogs = new LoyaltyCardViewDialogs();
    // Used only to seed the first navigator after recreation, before card data has been reloaded.
    private Integer restoredImageIndex = null;

    public static final String STATE_IMAGEINDEX = "imageIndex";
    public static final String STATE_FULLSCREEN = "isFullscreen";

    public static final String BUNDLE_ID = "id";
    public static final String BUNDLE_CARDLIST = "cardList";
    public static final String BUNDLE_TRANSITION_RIGHT = "transition_right";

    private long initTime = System.currentTimeMillis();

    private enum AdjacentCardDirection {
        PREVIOUS,
        NEXT
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (settings.useVolumeKeysForNavigation()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (initTime < (System.currentTimeMillis() - 1000)) {
                    navigateToAdjacentCard(AdjacentCardDirection.PREVIOUS);
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (initTime < (System.currentTimeMillis() - 1000)) {
                    navigateToAdjacentCard(AdjacentCardDirection.NEXT);
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

        LoyaltyCardImageType imageType = cardNavigator.getCurrent();

        // Fullscreen exists mainly to make barcodes easier to scan, not as a separate screen flow.
        if (imageType == LoyaltyCardImageType.BARCODE) {
            setFullscreen(true);

            return;
        }

        // If this is an image, open it in the gallery.
        openImageInGallery(imageType);
    }

    private void openImageInGallery(LoyaltyCardImageType imageType) {
        File file = null;

        switch (imageType) {
            case NONE:
                return;
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
            // If barcode rendering fails, drop that slot so the user falls back to working content.
            cardNavigator.remove(LoyaltyCardImageType.BARCODE);

            setStateBasedOnImageTypes();

            // Call correct drawMainImage
            setFullscreen(isFullscreen);

            Toast.makeText(LoyaltyCardViewActivity.this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
        }
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
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else if (transitionRight == 0) {
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
            restoredImageIndex = savedInstanceState.getInt(STATE_IMAGEINDEX, 0);
            isFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN);
        }

        extractIntentFields(getIntent());

        database = new DBHelper(this).getWritableDatabase();
        importURIHelper = new ImportURIHelper(this);
        mainImageRenderer = new LoyaltyCardMainImageRenderer(this, this);

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

        binding.bottomAppBarInfoButton.setOnClickListener(view ->
                dialogs.showInfoDialog(this, loyaltyCard, loyaltyCardGroups)
        );
        binding.bottomAppBarPreviousButton.setOnClickListener(view ->
                navigateToAdjacentCard(AdjacentCardDirection.PREVIOUS)
        );
        binding.bottomAppBarNextButton.setOnClickListener(view ->
                navigateToAdjacentCard(AdjacentCardDirection.NEXT)
        );
        binding.bottomAppBarUpdateBalanceButton.setOnClickListener(view ->
                dialogs.showBalanceUpdateDialog(this, loyaltyCard, newBalance -> {
                    DBHelper.updateLoyaltyCardBalance(database, loyaltyCardId, newBalance);
                    onResume();
                })
        );

        binding.iconContainer.setOnClickListener(view -> {
            if (loyaltyCard.getImageThumbnail(this) != null) {
                openImageInGallery(LoyaltyCardImageType.ICON);
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
        // This shortcut started as a TalkBack aid, but it is still useful as a quick way to cycle images.
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
                renderCurrentMainImage(true);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    private boolean hasBalance(LoyaltyCard loyaltyCard) {
        return !loyaltyCard.balance.equals(new BigDecimal(0));
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

    private void navigateToAdjacentCard(AdjacentCardDirection direction) {
        if (cardList == null || cardList.size() == 1) {
            return;
        }

        boolean next = direction == AdjacentCardDirection.NEXT;
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
        savedInstanceState.putInt(STATE_IMAGEINDEX, cardNavigator.getCurrentIndex());
        savedInstanceState.putBoolean(STATE_FULLSCREEN, isFullscreen);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        activityOverridesNavBarColor = true;
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

        Window window = getWindow();
        applyWindowPreferences(window);
        enablePausedNfcIfConfigured();

        if (!loadCurrentCardFromDatabase()) {
            finish();
            return;
        }

        populateStateFromCurrentCard();
        showHideElementsForScreenSize();
        bindCardIdDescriptionInteractions();
        applyCardStyling(window);
        refreshDisplayedCardMedia();

        DBHelper.updateLoyaltyCardLastUsed(database, loyaltyCard.id);

        invalidateOptionsMenu();

        ShortcutHelper.updateShortcuts(this);
    }

    @Override
    protected void onDestroy() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        super.onDestroy();
    }

    private void applyWindowPreferences(Window window) {
        if (window == null) {
            return;
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        WindowManager.LayoutParams attributes = window.getAttributes();

        // Brightening the screen improves scan reliability when the barcode is displayed on-device.
        if (settings.useMaxBrightnessDisplayingBarcode()) {
            attributes.screenBrightness = 1F;
        }

        if (settings.getKeepScreenOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Some users scan cards directly from the lock screen, so keep the historical unlock behavior.
        if (settings.getDisableLockscreenWhileViewingCard()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
            } else {
                showWhenLockedSdkLessThan27(window);
            }
        }

        window.setAttributes(attributes);
    }

    private void enablePausedNfcIfConfigured() {
        // Pause NFC to prevent NFC payments from triggering while showing a barcode
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            return;
        }

        if (settings.getDisableNfcWhileViewingCard()) {
            nfcAdapter.enableReaderMode(this, tag -> {
                Snackbar snackbar = Snackbar.make(binding.container, R.string.nfc_blocked_while_viewing_card, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.fabEdit)
                        .setAction(R.string.change_settings, view -> {
                            // Open settings activity
                            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                            startActivity(intent);
                        });
                snackbar.show();
            }, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V
                    | NfcAdapter.FLAG_READER_NFC_BARCODE
                    | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                    | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
        } else {
            nfcAdapter.disableReaderMode(this);
        }
    }

    private boolean loadCurrentCardFromDatabase() {
        loyaltyCard = DBHelper.getLoyaltyCard(this, database, loyaltyCardId);
        if (loyaltyCard != null) {
            return true;
        }

        Log.w(TAG, "Could not lookup loyalty card " + loyaltyCardId);
        Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
        return false;
    }

    private void populateStateFromCurrentCard() {
        setTitle(loyaltyCard.store);
        loyaltyCardGroups = DBHelper.getLoyaltyCardGroups(database, loyaltyCardId);
        format = loyaltyCard.barcodeType;
        cardIdString = loyaltyCard.cardId;
        barcodeIdString = loyaltyCard.barcodeId;
        barcodeEncoding = loyaltyCard.barcodeEncoding;
        binding.mainImageDescription.setText(loyaltyCard.cardId);
    }

    private void bindCardIdDescriptionInteractions() {
        binding.mainImageDescription.setOnClickListener(v -> {
            // Only the barcode/card-id state exposes the full value and copy action.
            if (!isShowingCardIdDescription()) {
                return;
            }

            TextView cardIdView = new TextView(LoyaltyCardViewActivity.this);
            cardIdView.setAutoLinkMask(Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
            cardIdView.setText(loyaltyCard.cardId);
            cardIdView.setTextIsSelectable(true);
            int contentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
            cardIdView.setPadding(contentPadding, contentPadding / 2, contentPadding, 0);

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(LoyaltyCardViewActivity.this);
            builder.setTitle(R.string.cardId);
            builder.setView(cardIdView);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
            builder.setNeutralButton(R.string.copy_value, (dialog, which) -> copyCardIdToClipboard());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
        binding.mainImageDescription.setOnLongClickListener(view -> {
            if (!isShowingCardIdDescription()) {
                return false;
            }

            copyCardIdToClipboard();
            return true;
        });
    }

    private void applyCardStyling(Window window) {
        int backgroundHeaderColor = Utils.getHeaderColor(this, loyaltyCard);
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
    }

    private void refreshDisplayedCardMedia() {
        cardNavigator = createCardNavigator(isBarcodeSupported());
        setStateBasedOnImageTypes();
        // Call correct drawMainImage
        setFullscreen(isFullscreen);
    }

    private boolean isBarcodeSupported() {
        if (format == null) {
            return false;
        }

        if (format.isSupported()) {
            return true;
        }

        Toast.makeText(this, getString(R.string.unsupportedBarcodeType), Toast.LENGTH_LONG).show();
        return false;
    }

    private void setStateBasedOnImageTypes() {
        ViewGroup.LayoutParams cardHolderLayoutParams = binding.cardHolder.getLayoutParams();
        // A card without barcode/front/back media should shrink to its text fallback instead of filling the screen.
        if (cardNavigator.isEmpty()) {
            cardHolderLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            cardHolderLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        binding.cardHolder.setLayoutParams(cardHolderLayoutParams);

        updateMainImageUiState();
    }

    private LoyaltyCardImageNavigator createCardNavigator(boolean isBarcodeSupported) {
        List<LoyaltyCardImageType> availableImageTypes = new ArrayList<>();

        if (isBarcodeSupported) {
            availableImageTypes.add(LoyaltyCardImageType.BARCODE);
        }

        frontImageBitmap = loyaltyCard.getImageFront(this);
        if (frontImageBitmap != null) {
            availableImageTypes.add(LoyaltyCardImageType.IMAGE_FRONT);
        }

        backImageBitmap = loyaltyCard.getImageBack(this);
        if (backImageBitmap != null) {
            availableImageTypes.add(LoyaltyCardImageType.IMAGE_BACK);
        }

        // Card edits may remove barcode/front/back images, so keep the previously selected index in range.
        int initialIndex = cardNavigator.isEmpty()
                ? (restoredImageIndex != null ? restoredImageIndex : 0)
                : cardNavigator.getCurrentIndex();
        LoyaltyCardImageNavigator navigator =
                new LoyaltyCardImageNavigator(availableImageTypes, initialIndex);
        restoredImageIndex = null;
        return navigator;
    }

    private void renderCurrentMainImage(boolean waitForResize) {
        mainImageRenderer.renderCurrent(
                cardNavigator.getCurrent(),
                frontImageBitmap,
                backImageBitmap,
                format,
                barcodeEncoding,
                cardIdString,
                barcodeIdString,
                barcodeRenderTarget,
                binding.mainImageDescription,
                binding.mainCardView,
                isFullscreen,
                waitForResize
        );
    }

    private void syncFullscreenScalers() {
        binding.barcodeScaler.setProgress(loyaltyCard.zoomLevel);
        setScalerGuideline(loyaltyCard.zoomLevel);
        binding.barcodeWidthscaler.setProgress(loyaltyCard.zoomLevelWidth);
        setScalerWidthGuideline(loyaltyCard.zoomLevelWidth);
    }

    @SuppressWarnings("deprecation")
    private void showWhenLockedSdkLessThan27(Window window) {
        // Pre-O_MR1 devices still need the legacy window flags because setShowWhenLocked(true) is unavailable.
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
            if (loyaltyCard.starStatus == 1) {
                menu.findItem(R.id.action_star_unstar).setIcon(R.drawable.ic_starred);
                menu.findItem(R.id.action_star_unstar).setTitle(R.string.unstar);
            } else {
                menu.findItem(R.id.action_star_unstar).setIcon(R.drawable.ic_unstarred);
                menu.findItem(R.id.action_star_unstar).setTitle(R.string.star);
            }

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
        // Treat square-ish devices such as the Unihertz Titan like landscape to avoid a cramped header layout.
        boolean isSmallHeight = getResources().getDisplayMetrics().heightPixels < (getResources().getDisplayMetrics().widthPixels * 1.5);

        if (orientation == Configuration.ORIENTATION_LANDSCAPE || isSmallHeight) {
            Log.d(TAG, "Detected landscape mode or square-ish screen");
            binding.iconContainer.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "Detected portrait mode on non square-ish screen");
            binding.iconContainer.setVisibility(View.VISIBLE);
        }

        enableToolbarBackButton();
    }

    private void setMainImage(boolean next, boolean overflow) {
        boolean moved = next ? cardNavigator.moveNext(overflow) : cardNavigator.movePrevious();
        if (!moved) {
            return;
        }

        renderCurrentMainImage(false);

        updateMainImageUiState();
    }

    private boolean isShowingCardIdDescription() {
        return cardNavigator.isEmpty() || cardNavigator.getCurrent() == LoyaltyCardImageType.BARCODE;
    }

    private void updateMainImageUiState() {
        updateMainImagePreviousNextButtons();
        updateMainImageAccessibility();
    }

    private void updateMainImageAccessibility() {
        // The same image view can represent barcode/front/back states, so accessibility actions must track that role.
        int accessibilityClickAction;
        LoyaltyCardImageType currentImageType = cardNavigator.getCurrent();
        if (currentImageType == LoyaltyCardImageType.IMAGE_FRONT) {
            accessibilityClickAction = R.string.openFrontImageInGalleryApp;
        } else if (currentImageType == LoyaltyCardImageType.IMAGE_BACK) {
            accessibilityClickAction = R.string.openBackImageInGalleryApp;
        } else {
            accessibilityClickAction = R.string.moveBarcodeToTopOfScreen;
        }

        ViewCompat.replaceAccessibilityAction(
                binding.mainImage,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getString(accessibilityClickAction),
                null
        );

        int accessibilityLongPressAction;
        LoyaltyCardImageType nextImageType = cardNavigator.peekNext(true);
        if (nextImageType == LoyaltyCardImageType.IMAGE_FRONT) {
            accessibilityLongPressAction = R.string.switchToFrontImage;
        } else if (nextImageType == LoyaltyCardImageType.IMAGE_BACK) {
            accessibilityLongPressAction = R.string.switchToBackImage;
        } else {
            accessibilityLongPressAction = R.string.switchToBarcode;
        }

        ViewCompat.replaceAccessibilityAction(
                binding.mainImage,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                getString(accessibilityLongPressAction),
                null
        );
    }

    private void updateMainImagePreviousNextButtons() {
        if (cardNavigator.size() < 2) {
            binding.mainLeftButton.setVisibility(View.INVISIBLE);
            binding.mainRightButton.setVisibility(View.INVISIBLE);
            binding.mainLeftButton.setOnClickListener(null);
            binding.mainRightButton.setOnClickListener(null);
            return;
        }

        final ImageButton previousButton;
        final ImageButton nextButton;
        // In RTL, the visual left/right buttons map to opposite logical navigation directions.
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            previousButton = binding.mainRightButton;
            nextButton = binding.mainLeftButton;
        } else {
            previousButton = binding.mainLeftButton;
            nextButton = binding.mainRightButton;
        }

        if (cardNavigator.canGoPrevious()) {
            previousButton.setVisibility(View.VISIBLE);
            previousButton.setOnClickListener(view -> setMainImage(false, false));
        } else {
            previousButton.setVisibility(View.INVISIBLE);
            previousButton.setOnClickListener(null);
        }

        if (cardNavigator.canGoNext()) {
            nextButton.setVisibility(View.VISIBLE);
            nextButton.setOnClickListener(view -> setMainImage(true, false));
        } else {
            nextButton.setVisibility(View.INVISIBLE);
            nextButton.setOnClickListener(null);
        }
    }

    /**
     * Fullscreen hides system chrome and moves the barcode higher on screen so scanners can read
     * it even when the whole device does not fit cleanly in front of the reader.
     */
    private void setFullscreen(boolean enabled) {
        isFullscreen = enabled;
        ActionBar actionBar = getSupportActionBar();

        if (enabled && !cardNavigator.isEmpty()) {
            barcodeRenderTarget = binding.fullscreenImage;
            binding.container.setVisibility(View.GONE);
            binding.fullscreenLayout.setVisibility(View.VISIBLE);
            // Square barcodes resize uniformly, and Data Matrix behaves similarly, so width-only scaling adds no value.
            binding.setWidthLayout.setVisibility(
                    format == null || format.isSquare() || format.format() == com.google.zxing.BarcodeFormat.DATA_MATRIX
                            ? View.GONE
                            : View.VISIBLE
            );
            renderCurrentMainImage(true);
            syncFullscreenScalers();

            if (actionBar != null) {
                actionBar.hide();
            }

            binding.bottomAppBar.setVisibility(View.GONE);
            binding.fabEdit.setVisibility(View.GONE);
            setFullscreenMode();
        } else {
            barcodeRenderTarget = binding.mainImage;
            binding.container.setVisibility(View.VISIBLE);
            binding.fullscreenLayout.setVisibility(View.GONE);
            renderCurrentMainImage(true);

            if (actionBar != null) {
                actionBar.show();
            }

            binding.bottomAppBar.setVisibility(View.VISIBLE);
            binding.fabEdit.setVisibility(View.VISIBLE);
            unsetFullscreenMode();
        }
        Log.d("setFullScreen", "Is full screen enabled? " + enabled + " Zoom Level = " + binding.barcodeScaler.getProgress());
    }

    @SuppressWarnings("deprecation")
    private void setFullscreenMode() {
        Window window = getWindow();
        if (window == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController wic = window.getInsetsController();
            if (wic != null) {
                wic.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                wic.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            return;
        }

        window.getDecorView().setSystemUiVisibility(
                window.getDecorView().getSystemUiVisibility()
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @SuppressWarnings("deprecation")
    private void unsetFullscreenMode() {
        Window window = getWindow();
        if (window == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
            WindowInsetsController wic = window.getInsetsController();
            if (wic != null) {
                wic.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                wic.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
            }
            return;
        }

        window.getDecorView().setSystemUiVisibility(
                window.getDecorView().getSystemUiVisibility()
                        & ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        & ~View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    private void copyCardIdToClipboard() {
        String value = loyaltyCard.cardId;

        if (value == null || value.isEmpty()) {
            Toast.makeText(this, R.string.nothing_to_copy, Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.cardId), value);
        cm.setPrimaryClip(clip);

        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }
}
