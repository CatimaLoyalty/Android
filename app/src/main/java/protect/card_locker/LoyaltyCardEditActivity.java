package protect.card_locker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.model.AspectRatio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Callable;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.databinding.LayoutChipChoiceBinding;
import protect.card_locker.databinding.LoyaltyCardEditActivityBinding;
import protect.card_locker.viewmodels.LoyaltyCardEditActivityViewModel;
import protect.card_locker.coverage.CoverageTool;

public class LoyaltyCardEditActivity extends CatimaAppCompatActivity implements BarcodeImageWriterResultCallback, ColorPickerDialogListener {
    private static final String TAG = "Catima";
    protected LoyaltyCardEditActivityViewModel viewModel;
    private LoyaltyCardEditActivityBinding binding;

    private static final String PICK_DATE_REQUEST_KEY = "pick_date_request";
    private static final String NEWLY_PICKED_DATE_ARGUMENT_KEY = "newly_picked_date";

    private final String TEMP_CAMERA_IMAGE_NAME = LoyaltyCardEditActivity.class.getSimpleName() + "_camera_image.jpg";
    private final String TEMP_CROP_IMAGE_NAME = LoyaltyCardEditActivity.class.getSimpleName() + "_crop_image.png";
    private final Bitmap.CompressFormat TEMP_CROP_IMAGE_FORMAT = Bitmap.CompressFormat.PNG;

    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_FRONT = 100;
    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_BACK = 101;
    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_ICON = 102;
    private static final int PERMISSION_REQUEST_STORAGE_IMAGE_FRONT = 103;
    private static final int PERMISSION_REQUEST_STORAGE_IMAGE_BACK = 104;
    private static final int PERMISSION_REQUEST_STORAGE_IMAGE_ICON = 105;

    public static final String BUNDLE_ID = "id";
    public static final String BUNDLE_DUPLICATE_ID = "duplicateId";
    public static final String BUNDLE_UPDATE = "update";
    public static final String BUNDLE_OPEN_SET_ICON_MENU = "openSetIconMenu";
    public static final String BUNDLE_ADDGROUP = "addGroup";

    ImageView thumbnail;
    ImageView thumbnailEditIcon;
    EditText storeFieldEdit;
    EditText noteFieldEdit;
    ChipGroup groupsChips;
    AutoCompleteTextView validFromField;
    AutoCompleteTextView expiryField;
    AutoCompleteTextView balanceCurrencyField;
    EditText balanceField;
    TextView cardIdFieldView;
    AutoCompleteTextView barcodeIdField;
    AutoCompleteTextView barcodeTypeField;
    AutoCompleteTextView barcodeEncodingField;
    ImageView barcodeImage;
    View barcodeImageLayout;
    View barcodeCaptureLayout;
    View cardImageFrontHolder;
    View cardImageBackHolder;
    ImageView cardImageFront;
    ImageView cardImageBack;

    Button enterButton;

    Toolbar toolbar;

    SQLiteDatabase mDatabase;

    String tempStoredOldBarcodeValue = null;
    boolean initDone = false;
    boolean onResuming = false;
    boolean onRestoring = false;
    AlertDialog confirmExitDialog = null;

    HashMap<String, Currency> currencies = new HashMap<>();
    HashMap<String, String> currencySymbols = new HashMap<>();
    boolean validBalance = true;

    ActivityResultLauncher<Uri> mPhotoTakerLauncher;
    ActivityResultLauncher<Intent> mPhotoPickerLauncher;
    ActivityResultLauncher<Intent> mCardIdAndBarCodeEditorLauncher;

    ActivityResultLauncher<Intent> mCropperLauncher;
    UCrop.Options mCropperOptions;

    // store system locale for Build.VERSION.SDK_INT < Build.VERSION_CODES.N
    private Locale mSystemLocale;

    @Override
    protected void attachBaseContext(Context base) {
        // store system locale
        mSystemLocale = Locale.getDefault();
        super.attachBaseContext(base);
    }

    protected void setLoyaltyCardStore(@NonNull String store) {
        viewModel.getLoyaltyCard().setStore(store);

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardNote(@NonNull String note) {
        viewModel.getLoyaltyCard().setNote(note);

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardValidFrom(@Nullable Date validFrom) {
        viewModel.getLoyaltyCard().setValidFrom(validFrom);

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardExpiry(@Nullable Date expiry) {
        viewModel.getLoyaltyCard().setExpiry(expiry);

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardBalanceType(@Nullable Currency balanceType) {
        viewModel.getLoyaltyCard().setBalanceType(balanceType);

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardBalance(@NonNull BigDecimal balance) {
        viewModel.getLoyaltyCard().setBalance(balance);

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardCardId(@NonNull String cardId) {
        viewModel.getLoyaltyCard().setCardId(cardId);

        generateBarcode();

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardBarcodeId(@Nullable String barcodeId) {
        viewModel.getLoyaltyCard().setBarcodeId(barcodeId);

        generateBarcode();

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardBarcodeType(@Nullable CatimaBarcode barcodeType) {
        viewModel.getLoyaltyCard().setBarcodeType(barcodeType);

        generateBarcode();

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardBarcodeEncoding(@NonNull Charset barcodeEncoding) {
        viewModel.getLoyaltyCard().setBarcodeEncoding(barcodeEncoding);

        generateBarcode();

        viewModel.setHasChanged(true);
    }

    protected void setLoyaltyCardHeaderColor(@Nullable Integer headerColor) {
        viewModel.getLoyaltyCard().setHeaderColor(headerColor);

        viewModel.setHasChanged(true);
    }

    /* Extract intent fields and return if code should keep running */
    private boolean extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();

        viewModel.setAddGroup(b != null ? b.getString(BUNDLE_ADDGROUP) : null);
        viewModel.setOpenSetIconMenu(b != null && b.getBoolean(BUNDLE_OPEN_SET_ICON_MENU, false));

        viewModel.setLoyaltyCardId(b != null ? b.getInt(BUNDLE_ID) : 0);
        viewModel.setUpdateLoyaltyCard(b != null && b.getBoolean(BUNDLE_UPDATE, false));
        viewModel.setDuplicateFromLoyaltyCardId(b != null && b.getBoolean(BUNDLE_DUPLICATE_ID, false));
        viewModel.setImportLoyaltyCardUri(intent.getData());

        Uri importLoyaltyCardUri = viewModel.getImportLoyaltyCardUri();

        // If we have to import a loyalty card, do so
        if (viewModel.getUpdateLoyaltyCard() || viewModel.getDuplicateFromLoyaltyCardId()) {
            // Retrieve from database
            LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(this, mDatabase, viewModel.getLoyaltyCardId());
            if (loyaltyCard == null) {
                Log.w(TAG, "Could not lookup loyalty card " + viewModel.getLoyaltyCardId());
                Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
            viewModel.setLoyaltyCard(loyaltyCard);
        } else if (importLoyaltyCardUri != null) {
            // Load from URI
            try {
                viewModel.setLoyaltyCard(new ImportURIHelper(this).parse(importLoyaltyCardUri));
            } catch (InvalidObjectException ex) {
                Toast.makeText(this, R.string.failedParsingImportUriError, Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
        }

        // If the intent contains any loyalty card fields, override those fields in our current temp card
        if (b != null) {
            LoyaltyCard loyaltyCard = viewModel.getLoyaltyCard();
            loyaltyCard.updateFromBundle(b, false);
            viewModel.setLoyaltyCard(loyaltyCard);
        }

        Log.d(TAG, "Edit activity: id=" + viewModel.getLoyaltyCardId()
                + ", updateLoyaltyCard=" + viewModel.getUpdateLoyaltyCard());

        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        onRestoring = true;
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LoyaltyCardEditActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Utils.applyWindowInsetsAndFabOffset(binding.getRoot(), binding.fabSave);

        viewModel = new ViewModelProvider(this).get(LoyaltyCardEditActivityViewModel.class);

        toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        enableToolbarBackButton();

        mDatabase = new DBHelper(this).getWritableDatabase();

        if (!viewModel.getInitialized()) {
            if (!extractIntentFields(getIntent())) {
                return;
            }
            viewModel.setInitialized(true);
        }

        for (Currency currency : Currency.getAvailableCurrencies()) {
            currencies.put(currency.getSymbol(), currency);
            currencySymbols.put(currency.getCurrencyCode(), currency.getSymbol());
        }

        thumbnail = binding.thumbnail;
        thumbnailEditIcon = binding.thumbnailEditIcon;
        storeFieldEdit = binding.storeNameEdit;
        noteFieldEdit = binding.noteEdit;
        groupsChips = binding.groupChips;
        validFromField = binding.validFromField;
        expiryField = binding.expiryField;
        balanceCurrencyField = binding.balanceCurrencyField;
        balanceField = binding.balanceField;
        cardIdFieldView = binding.cardIdView;
        barcodeIdField = binding.barcodeIdField;
        barcodeTypeField = binding.barcodeTypeField;
        barcodeEncodingField = binding.barcodeEncodingField;
        barcodeImage = binding.barcode;
        barcodeImage.setClipToOutline(true);
        barcodeImageLayout = binding.barcodeLayout;
        barcodeCaptureLayout = binding.barcodeCaptureLayout;
        cardImageFrontHolder = binding.frontImageHolder;
        cardImageBackHolder = binding.backImageHolder;
        cardImageFront = binding.frontImage;
        cardImageBack = binding.backImage;

        enterButton = binding.enterButton;

        storeFieldEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String storeName = s.toString().trim();
                setLoyaltyCardStore(storeName);
                generateIcon(storeName);

                if (storeName.isEmpty()) {
                    storeFieldEdit.setError(getString(R.string.field_must_not_be_empty));
                } else {
                    storeFieldEdit.setError(null);
                }
            }
        });

        noteFieldEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setLoyaltyCardNote(s.toString());
            }
        });

        addDateFieldTextChangedListener(validFromField, R.string.anyDate, R.string.chooseValidFromDate, LoyaltyCardField.validFrom);

        addDateFieldTextChangedListener(expiryField, R.string.never, R.string.chooseExpiryDate, LoyaltyCardField.expiry);

        setMaterialDatePickerResultListener();

        balanceCurrencyField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Currency currency;

                if (s.toString().equals(getString(R.string.points))) {
                    currency = null;
                } else {
                    currency = currencies.get(s.toString());
                }

                setLoyaltyCardBalanceType(currency);

                if (viewModel.getLoyaltyCard().balance != null && !onResuming && !onRestoring) {
                    balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(viewModel.getLoyaltyCard().balance, currency));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> currencyList = new ArrayList<>(currencies.keySet());
                Collections.sort(currencyList, (o1, o2) -> {
                    boolean o1ascii = o1.matches("^[^a-zA-Z]*$");
                    boolean o2ascii = o2.matches("^[^a-zA-Z]*$");

                    if (!o1ascii && o2ascii) {
                        return 1;
                    } else if (o1ascii && !o2ascii) {
                        return -1;
                    }

                    return o1.compareTo(o2);
                });

                // Sort locale currencies on top
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    LocaleList locales = getApplicationContext().getResources().getConfiguration().getLocales();

                    for (int i = locales.size() - 1; i >= 0; i--) {
                        Locale locale = locales.get(i);
                        currencyPrioritizeLocaleSymbols(currencyList, locale);
                    }
                } else {
                    currencyPrioritizeLocaleSymbols(currencyList, mSystemLocale);
                }

                currencyList.add(0, getString(R.string.points));
                ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(LoyaltyCardEditActivity.this, android.R.layout.select_dialog_item, currencyList);
                balanceCurrencyField.setAdapter(currencyAdapter);
            }
        });

        balanceField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !onResuming && !onRestoring) {
                if (balanceField.getText().toString().isEmpty()) {
                    setLoyaltyCardBalance(BigDecimal.valueOf(0));
                }

                balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(viewModel.getLoyaltyCard().balance, viewModel.getLoyaltyCard().balanceType));
            }
        });

        balanceField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (onResuming || onRestoring) return;
                try {
                    BigDecimal balance = Utils.parseBalance(s.toString(), viewModel.getLoyaltyCard().balanceType);
                    setLoyaltyCardBalance(balance);
                    balanceField.setError(null);
                    validBalance = true;
                } catch (ParseException e) {
                    e.printStackTrace();
                    balanceField.setError(getString(R.string.balanceParsingFailed));
                    validBalance = false;
                }
            }
        });

        cardIdFieldView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (initDone && !onResuming) {
                    if (tempStoredOldBarcodeValue == null) {
                        // We changed the card ID, save the current barcode ID in a temp
                        // variable and make sure to ask the user later if they also want to
                        // update the barcode ID
                        if (viewModel.getLoyaltyCard().barcodeId != null) {
                            // If it is not set to "same as Card ID", save as tempStoredOldBarcodeValue
                            tempStoredOldBarcodeValue = barcodeIdField.getText().toString();
                        }
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setLoyaltyCardCardId(s.toString());

                if (s.length() == 0) {
                    cardIdFieldView.setError(getString(R.string.field_must_not_be_empty));
                } else {
                    cardIdFieldView.setError(null);
                }
            }
        });

        barcodeIdField.addTextChangedListener(new SimpleTextWatcher() {
            CharSequence lastValue;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastValue = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals(getString(R.string.sameAsCardId))) {
                    // If the user manually changes the barcode again make sure we disable the
                    // request to update it to match the card id (if changed)
                    tempStoredOldBarcodeValue = null;

                    setLoyaltyCardBarcodeId(null);
                } else if (s.toString().equals(getString(R.string.setBarcodeId))) {
                    if (!lastValue.toString().equals(getString(R.string.setBarcodeId))) {
                        barcodeIdField.setText(lastValue);
                    }

                    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(LoyaltyCardEditActivity.this);
                    builder.setTitle(R.string.setBarcodeId);
                    final EditText input = new EditText(LoyaltyCardEditActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);

                    FrameLayout container = new FrameLayout(LoyaltyCardEditActivity.this);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    int contentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
                    params.leftMargin = contentPadding;
                    params.topMargin = contentPadding / 2;
                    params.rightMargin = contentPadding;

                    input.setLayoutParams(params);
                    container.addView(input);
                    if (viewModel.getLoyaltyCard().barcodeId != null) {
                        input.setText(viewModel.getLoyaltyCard().barcodeId);
                    }
                    builder.setView(container);

                    builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        // If the user manually changes the barcode again make sure we disable the
                        // request to update it to match the card id (if changed)
                        tempStoredOldBarcodeValue = null;

                        barcodeIdField.setText(input.getText());
                    });
                    builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    input.requestFocus();
                } else {
                    setLoyaltyCardBarcodeId(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> barcodeIdList = new ArrayList<>();
                barcodeIdList.add(0, getString(R.string.sameAsCardId));
                barcodeIdList.add(1, getString(R.string.setBarcodeId));
                ArrayAdapter<String> barcodeIdAdapter = new ArrayAdapter<>(LoyaltyCardEditActivity.this, android.R.layout.select_dialog_item, barcodeIdList);
                barcodeIdField.setAdapter(barcodeIdAdapter);
            }
        });

        barcodeTypeField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    if (s.toString().equals(getString(R.string.noBarcode))) {
                        setLoyaltyCardBarcodeType(null);
                    } else {
                        try {
                            CatimaBarcode barcodeFormat = CatimaBarcode.fromPrettyName(s.toString());

                            setLoyaltyCardBarcodeType(barcodeFormat);

                            if (!barcodeFormat.isSupported()) {
                                Toast.makeText(LoyaltyCardEditActivity.this, getString(R.string.unsupportedBarcodeType), Toast.LENGTH_LONG).show();
                            }
                        } catch (IllegalArgumentException e) {
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> barcodeList = new ArrayList<>(CatimaBarcode.barcodePrettyNames);
                barcodeList.add(0, getString(R.string.noBarcode));
                ArrayAdapter<String> barcodeAdapter = new ArrayAdapter<>(LoyaltyCardEditActivity.this, android.R.layout.select_dialog_item, barcodeList);
                barcodeTypeField.setAdapter(barcodeAdapter);
            }
        });

        barcodeEncodingField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    Log.d(TAG, "Setting barcode encoding to " + s.toString());
                    setLoyaltyCardBarcodeEncoding(Charset.forName(s.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> barcodeEncodingList = new ArrayList<>();
                barcodeEncodingList.add(StandardCharsets.ISO_8859_1.name());
                barcodeEncodingList.add(StandardCharsets.UTF_8.name());
                ArrayAdapter<String> barcodeEncodingAdapter = new ArrayAdapter<>(LoyaltyCardEditActivity.this, android.R.layout.select_dialog_item, barcodeEncodingList);
                barcodeEncodingField.setAdapter(barcodeEncodingAdapter);
            }
        });

        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setTabIndex(tab.getPosition());
                showPart(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewModel.setTabIndex(tab.getPosition());
                showPart(tab.getText().toString());
            }
        });

        selectTab(viewModel.getTabIndex());

        mPhotoTakerLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result) {
                startCropper(getCacheDir() + "/" + TEMP_CAMERA_IMAGE_NAME);
            }
        });

        // android 11: wanted to swap it to ActivityResultContracts.GetContent but then it shows a file browsers that shows image mime types, offering gallery in the file browser
        mPhotoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent intent = result.getData();
                if (intent == null) {
                    Log.d("photo picker", "photo picker returned without an intent");
                    return;
                }
                Uri uri = intent.getData();
                startCropperUri(uri);
            }
        });

        mCardIdAndBarCodeEditorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent resultIntent = result.getData();
                if (resultIntent == null) {
                    Log.d(TAG, "barcode and card id editor picker returned without an intent");
                    return;
                }

                Bundle resultIntentBundle = resultIntent.getExtras();
                if (resultIntentBundle == null) {
                    Log.d(TAG, "barcode and card id editor picker returned without a bundle");
                    return;
                }

                LoyaltyCard loyaltyCard = viewModel.getLoyaltyCard();
                loyaltyCard.updateFromBundle(resultIntentBundle, false);
                viewModel.setLoyaltyCard(loyaltyCard);
                generateBarcode();
                viewModel.setHasChanged(true);
            }
        });

        mCropperLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent intent = result.getData();
            if (intent == null) {
                Log.d("cropper", "ucrop returned a null intent");
                return;
            }
            if (result.getResultCode() == Activity.RESULT_OK) {
                Uri debugUri = UCrop.getOutput(intent);
                if (debugUri == null) {
                    throw new RuntimeException("ucrop returned success but not destination uri!");
                }
                Log.d("cropper", "ucrop produced image at " + debugUri);
                Bitmap bitmap = BitmapFactory.decodeFile(getCacheDir() + "/" + TEMP_CROP_IMAGE_NAME);

                if (bitmap != null) {
                    if (requestedFrontImage()) {
                        setCardImage(ImageLocationType.front, cardImageFront, Utils.resizeBitmap(bitmap, Utils.BITMAP_SIZE_BIG), true);
                    } else if (requestedBackImage()) {
                        setCardImage(ImageLocationType.back, cardImageBack, Utils.resizeBitmap(bitmap, Utils.BITMAP_SIZE_BIG), true);
                    } else if (requestedIcon()) {
                        setThumbnailImage(Utils.resizeBitmap(bitmap, Utils.BITMAP_SIZE_SMALL));
                    } else {
                        Toast.makeText(this, R.string.generic_error_please_retry, Toast.LENGTH_LONG).show();
                        return;
                    }
                    Log.d("cropper", "requestedImageType: " + viewModel.getRequestedImageType());
                    viewModel.setHasChanged(true);
                } else {
                    Toast.makeText(LoyaltyCardEditActivity.this, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
                }
            } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                Throwable e = UCrop.getError(intent);
                if (e == null) {
                    throw new RuntimeException("ucrop returned error state but not an error!");
                }
                Log.e("cropper error", e.toString());
            }
        });

        mCropperOptions = new UCrop.Options();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                askBeforeQuitIfChanged();
            }
        });
    }

    private void selectTab(int index) {
        binding.tabs.selectTab(binding.tabs.getTabAt(index));
        viewModel.setTabIndex(index);
    }

    // ucrop 2.2.6 initial aspect ratio is glitched when 0x0 is used as the initial ratio option
    // https://github.com/Yalantis/uCrop/blob/281c8e6438d81f464d836fc6b500517144af264a/ucrop/src/main/java/com/yalantis/ucrop/UCropActivity.java#L264
    // so source width height has to be provided for now, depending on whether future versions of ucrop will support 0x0 as the default option
    private void setCropperOptions(boolean cardShapeDefault, float sourceWidth, float sourceHeight) {
        mCropperOptions.setCompressionFormat(TEMP_CROP_IMAGE_FORMAT);
        mCropperOptions.setFreeStyleCropEnabled(true);
        mCropperOptions.setHideBottomControls(false);
        // default aspect ratio workaround
        int selectedByDefault = 1;
        if (cardShapeDefault) {
            selectedByDefault = 2;
        }
        mCropperOptions.setAspectRatioOptions(selectedByDefault,
                new AspectRatio(null, 1, 1),
                new AspectRatio(getResources().getString(com.yalantis.ucrop.R.string.ucrop_label_original).toUpperCase(), sourceWidth, sourceHeight),
                new AspectRatio(getResources().getString(R.string.card).toUpperCase(), 85.6f, 53.98f)
        );

        // Fix theming

        int colorPrimary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, ContextCompat.getColor(this, R.color.md_theme_light_primary));
        int colorOnPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, ContextCompat.getColor(this, R.color.md_theme_light_onPrimary));
        int colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, ContextCompat.getColor(this, R.color.md_theme_light_surface));
        int colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
        int colorBackground = MaterialColors.getColor(this, android.R.attr.colorBackground, ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
        mCropperOptions.setToolbarColor(colorSurface);
        mCropperOptions.setToolbarWidgetColor(colorOnSurface);
        mCropperOptions.setRootViewBackgroundColor(colorBackground);
        // set tool tip to be the darker of primary color
        if (Utils.isDarkModeEnabled(this)) {
            mCropperOptions.setActiveControlsWidgetColor(colorOnPrimary);
        } else {
            mCropperOptions.setActiveControlsWidgetColor(colorPrimary);
        }
    }

    private boolean requestedFrontImage() {
        int requestedImageType = viewModel.getRequestedImageType();

        return requestedImageType == Utils.CARD_IMAGE_FROM_CAMERA_FRONT || requestedImageType == Utils.CARD_IMAGE_FROM_FILE_FRONT;
    }

    private boolean requestedBackImage() {
        int requestedImageType = viewModel.getRequestedImageType();

        return requestedImageType == Utils.CARD_IMAGE_FROM_CAMERA_BACK || requestedImageType == Utils.CARD_IMAGE_FROM_FILE_BACK;
    }

    private boolean requestedIcon() {
        int requestedImageType = viewModel.getRequestedImageType();

        return requestedImageType == Utils.CARD_IMAGE_FROM_CAMERA_ICON || requestedImageType == Utils.CARD_IMAGE_FROM_FILE_ICON;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "To view card: " + viewModel.getLoyaltyCardId());

        onResuming = true;

        if (viewModel.getUpdateLoyaltyCard()) {
            setTitle(R.string.editCardTitle);
        } else {
            setTitle(R.string.addCardTitle);
        }

        boolean hadChanges = viewModel.getHasChanged();

        storeFieldEdit.setText(viewModel.getLoyaltyCard().store);
        noteFieldEdit.setText(viewModel.getLoyaltyCard().note);
        formatDateField(this, validFromField, viewModel.getLoyaltyCard().validFrom);
        formatDateField(this, expiryField, viewModel.getLoyaltyCard().expiry);
        cardIdFieldView.setText(viewModel.getLoyaltyCard().cardId);
        String barcodeId = viewModel.getLoyaltyCard().barcodeId;
        barcodeIdField.setText(barcodeId != null && !barcodeId.isEmpty() ? barcodeId : getString(R.string.sameAsCardId));
        CatimaBarcode barcodeType = viewModel.getLoyaltyCard().barcodeType;
        barcodeTypeField.setText(barcodeType != null ? barcodeType.prettyName() : getString(R.string.noBarcode));
        Charset barcodeEncoding = viewModel.getLoyaltyCard().barcodeEncoding;
        barcodeEncodingField.setText(barcodeEncoding.name());

        // We set the balance here (with onResuming/onRestoring == true) to prevent formatBalanceCurrencyField() from setting it (via onTextChanged),
        // which can cause issues when switching locale because it parses the balance and e.g. the decimal separator may have changed.
        formatBalanceCurrencyField(viewModel.getLoyaltyCard().balanceType);
        BigDecimal balance = viewModel.getLoyaltyCard().balance == null ? new BigDecimal("0") : viewModel.getLoyaltyCard().balance;
        setLoyaltyCardBalance(balance);
        balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(viewModel.getLoyaltyCard().balance, viewModel.getLoyaltyCard().balanceType));
        validBalance = true;
        Log.d(TAG, "Setting balance to " + balance);

        if (groupsChips.getChildCount() == 0) {
            List<Group> existingGroups = DBHelper.getGroups(mDatabase);

            List<Group> loyaltyCardGroups = DBHelper.getLoyaltyCardGroups(mDatabase, viewModel.getLoyaltyCardId());

            if (existingGroups.isEmpty()) {
                groupsChips.setVisibility(View.GONE);
            } else {
                groupsChips.setVisibility(View.VISIBLE);
            }

            for (Group group : DBHelper.getGroups(mDatabase)) {
                LayoutChipChoiceBinding chipChoiceBinding = LayoutChipChoiceBinding
                        .inflate(LayoutInflater.from(groupsChips.getContext()), groupsChips, false);
                Chip chip = chipChoiceBinding.getRoot();
                chip.setText(group._id);
                chip.setTag(group);

                if (group._id.equals(viewModel.getAddGroup())) {
                    chip.setChecked(true);
                } else {
                    chip.setChecked(false);
                    for (Group loyaltyCardGroup : loyaltyCardGroups) {
                        if (loyaltyCardGroup._id.equals(group._id)) {
                            chip.setChecked(true);
                            break;
                        }
                    }
                }

                chip.setOnTouchListener((v, event) -> {
                    viewModel.setHasChanged(true);

                    return false;
                });

                groupsChips.addView(chip);
            }
        }

        if (viewModel.getLoyaltyCard().headerColor == null) {
            // If name is set, pick colour relevant for name. Otherwise pick randomly
            setLoyaltyCardHeaderColor(viewModel.getLoyaltyCard().store.isEmpty() ? Utils.getRandomHeaderColor(this) : Utils.getHeaderColor(this, viewModel.getLoyaltyCard()));
        }

        setThumbnailImage(viewModel.getLoyaltyCard().getImageThumbnail(this));
        setCardImage(ImageLocationType.front, cardImageFront, viewModel.getLoyaltyCard().getImageFront(this), true);
        setCardImage(ImageLocationType.back, cardImageBack, viewModel.getLoyaltyCard().getImageBack(this), true);

        // Initialization has finished
        if (!initDone) {
            initDone = true;
            viewModel.setHasChanged(hadChanges);
        }

        generateBarcode();

        enterButton.setOnClickListener(new EditCardIdAndBarcode());
        barcodeImage.setOnClickListener(new EditCardIdAndBarcode());

        cardImageFrontHolder.setOnClickListener(new ChooseCardImage());
        cardImageBackHolder.setOnClickListener(new ChooseCardImage());

        FloatingActionButton saveButton = binding.fabSave;
        saveButton.setOnClickListener(v -> doSave());
        saveButton.bringToFront();

        generateIcon(storeFieldEdit.getText().toString().trim());

        Integer headerColor = viewModel.getLoyaltyCard().headerColor;
        if (headerColor != null) {
            thumbnail.setOnClickListener(new ChooseCardImage());
            thumbnailEditIcon.setBackgroundColor(Utils.needsDarkForeground(headerColor) ? Color.BLACK : Color.WHITE);
            thumbnailEditIcon.setColorFilter(Utils.needsDarkForeground(headerColor) ? Color.WHITE : Color.BLACK);
        }

        onResuming = false;
        onRestoring = false;

        // Fake click on the edit icon to cause the set icon option to pop up if the icon was
        // long-pressed in the view activity
        if (viewModel.getOpenSetIconMenu()) {
            viewModel.setOpenSetIconMenu(false);
            thumbnail.callOnClick();
        }
    }

    protected void setThumbnailImage(@Nullable Bitmap bitmap) {
        setCardImage(ImageLocationType.icon, thumbnail, bitmap, false);

        if (bitmap != null) {
            int headerColor = Utils.getHeaderColorFromImage(bitmap, Utils.getHeaderColor(this, viewModel.getLoyaltyCard()));

            setLoyaltyCardHeaderColor(headerColor);

            thumbnail.setBackgroundColor(Utils.needsDarkForeground(headerColor) ? Color.BLACK : Color.WHITE);

            thumbnailEditIcon.setBackgroundColor(Utils.needsDarkForeground(headerColor) ? Color.BLACK : Color.WHITE);
            thumbnailEditIcon.setColorFilter(Utils.needsDarkForeground(headerColor) ? Color.WHITE : Color.BLACK);
        } else {
            generateIcon(storeFieldEdit.getText().toString().trim());

            Integer headerColor = viewModel.getLoyaltyCard().headerColor;

            if (headerColor != null) {
                thumbnailEditIcon.setBackgroundColor(Utils.needsDarkForeground(headerColor) ? Color.BLACK : Color.WHITE);
                thumbnailEditIcon.setColorFilter(Utils.needsDarkForeground(headerColor) ? Color.WHITE : Color.BLACK);
            }
        }
    }

    protected void setCardImage(ImageLocationType imageLocationType, ImageView imageView, Bitmap bitmap, boolean applyFallback) {
        if (imageLocationType == ImageLocationType.icon) {
            viewModel.getLoyaltyCard().setImageThumbnail(bitmap, null);
        } else if (imageLocationType == ImageLocationType.front) {
            viewModel.getLoyaltyCard().setImageFront(bitmap, null);
        } else if (imageLocationType == ImageLocationType.back) {
            viewModel.getLoyaltyCard().setImageBack(bitmap, null);
        } else {
            throw new IllegalArgumentException("Unknown image type");
        }

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else if (applyFallback) {
            imageView.setImageResource(R.drawable.ic_camera_white);
        }
    }

    protected void addDateFieldTextChangedListener(AutoCompleteTextView dateField, @StringRes int defaultOptionStringId, @StringRes int chooseDateOptionStringId, LoyaltyCardField loyaltyCardField) {
        dateField.addTextChangedListener(new SimpleTextWatcher() {
            CharSequence lastValue;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastValue = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals(getString(defaultOptionStringId))) {
                    dateField.setTag(null);
                    switch (loyaltyCardField) {
                        case validFrom:
                            setLoyaltyCardValidFrom(null);
                            break;
                        case expiry:
                            setLoyaltyCardExpiry(null);
                            break;
                        default:
                            throw new AssertionError("Unexpected field: " + loyaltyCardField);
                    }
                } else if (s.toString().equals(getString(chooseDateOptionStringId))) {
                    if (!lastValue.toString().equals(getString(chooseDateOptionStringId))) {
                        dateField.setText(lastValue);
                    }
                    showDatePicker(
                            loyaltyCardField,
                            (Date) dateField.getTag(),
                            // if the expiry date is being set, set date picker's minDate to the 'valid from' date
                            loyaltyCardField == LoyaltyCardField.expiry ? (Date) validFromField.getTag() : null,
                            // if the 'valid from' date is being set, set date picker's maxDate to the expiry date
                            loyaltyCardField == LoyaltyCardField.validFrom ? (Date) expiryField.getTag() : null
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> dropdownOptions = new ArrayList<>();
                dropdownOptions.add(0, getString(defaultOptionStringId));
                dropdownOptions.add(1, getString(chooseDateOptionStringId));
                ArrayAdapter<String> dropdownOptionsAdapter = new ArrayAdapter<>(LoyaltyCardEditActivity.this, android.R.layout.select_dialog_item, dropdownOptions);
                dateField.setAdapter(dropdownOptionsAdapter);
            }
        });
    }

    protected static void formatDateField(Context context, EditText textField, Date date) {
        textField.setTag(date);

        if (date == null) {
            String text;
            if (textField.getId() == R.id.validFromField) {
                text = context.getString(R.string.anyDate);
            } else if (textField.getId() == R.id.expiryField) {
                text = context.getString(R.string.never);
            } else {
                throw new IllegalArgumentException("Unknown textField Id " + textField.getId());
            }
            textField.setText(text);
        } else {
            textField.setText(DateFormat.getDateInstance(DateFormat.LONG).format(date));
        }
    }

    private void formatBalanceCurrencyField(Currency balanceType) {
        if (balanceType == null) {
            balanceCurrencyField.setText(getString(R.string.points));
        } else {
            balanceCurrencyField.setText(getCurrencySymbol(balanceType));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        onMockedRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onMockedRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        Integer failureReason = null;

        if (requestCode == PERMISSION_REQUEST_CAMERA_IMAGE_FRONT) {
            if (granted) {
                takePhotoForCard(Utils.CARD_IMAGE_FROM_CAMERA_FRONT);
                return;
            }

            failureReason = R.string.cameraPermissionRequired;
        } else if (requestCode == PERMISSION_REQUEST_CAMERA_IMAGE_BACK) {
            if (granted) {
                takePhotoForCard(Utils.CARD_IMAGE_FROM_CAMERA_BACK);
                return;
            }

            failureReason = R.string.cameraPermissionRequired;
        } else if (requestCode == PERMISSION_REQUEST_CAMERA_IMAGE_ICON) {
            if (granted) {
                takePhotoForCard(Utils.CARD_IMAGE_FROM_CAMERA_ICON);
                return;
            }

            failureReason = R.string.cameraPermissionRequired;
        } else if (requestCode == PERMISSION_REQUEST_STORAGE_IMAGE_FRONT) {
            if (granted) {
                selectImageFromGallery(Utils.CARD_IMAGE_FROM_FILE_FRONT);
                return;
            }

            failureReason = R.string.storageReadPermissionRequired;
        } else if (requestCode == PERMISSION_REQUEST_STORAGE_IMAGE_BACK) {
            if (granted) {
                selectImageFromGallery(Utils.CARD_IMAGE_FROM_FILE_BACK);
                return;
            }

            failureReason = R.string.storageReadPermissionRequired;
        } else if (requestCode == PERMISSION_REQUEST_STORAGE_IMAGE_ICON) {
            if (granted) {
                selectImageFromGallery(Utils.CARD_IMAGE_FROM_FILE_ICON);
                return;
            }

            failureReason = R.string.storageReadPermissionRequired;
        }

        if (failureReason != null) {
            Toast.makeText(this, failureReason, Toast.LENGTH_LONG).show();
        }
    }

    private void askBarcodeChange(Runnable callback) {
        if (tempStoredOldBarcodeValue.equals(cardIdFieldView.getText().toString())) {
            // They are the same, don't ask
            barcodeIdField.setText(R.string.sameAsCardId);
            tempStoredOldBarcodeValue = null;

            if (callback != null) {
                callback.run();
            }

            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.updateBarcodeQuestionTitle)
                .setMessage(R.string.updateBarcodeQuestionText)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    barcodeIdField.setText(R.string.sameAsCardId);

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialogInterface -> {
                    if (tempStoredOldBarcodeValue != null) {
                        barcodeIdField.setText(tempStoredOldBarcodeValue);
                        tempStoredOldBarcodeValue = null;
                    }

                    if (callback != null) {
                        callback.run();
                    }
                })
                .show();
    }

    private void askBeforeQuitIfChanged() {
        if (!viewModel.getHasChanged()) {
            if (tempStoredOldBarcodeValue != null) {
                askBarcodeChange(this::askBeforeQuitIfChanged);
                return;
            }

            finish();
            return;
        }

        if (confirmExitDialog == null) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.leaveWithoutSaveTitle);
            builder.setMessage(R.string.leaveWithoutSaveConfirmation);
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                finish();
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            confirmExitDialog = builder.create();
        }
        confirmExitDialog.show();
    }


    private void takePhotoForCard(int type) {
        Uri photoURI = FileProvider.getUriForFile(LoyaltyCardEditActivity.this, BuildConfig.APPLICATION_ID, Utils.createTempFile(this, TEMP_CAMERA_IMAGE_NAME));
        viewModel.setRequestedImageType(type);

        try {
            mPhotoTakerLauncher.launch(photoURI);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), R.string.cameraPermissionDeniedTitle, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    private void selectImageFromGallery(int type) {
        viewModel.setRequestedImageType(type);

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(photoPickerIntent, getString(R.string.addFromImage));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { contentIntent });

        try {
            mPhotoPickerLauncher.launch(chooserIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), R.string.failedLaunchingPhotoPicker, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }

    @Override
    public void onBarcodeImageWriterResult(boolean success) {
        if (!success) {
            barcodeImageLayout.setVisibility(View.GONE);
            Toast.makeText(LoyaltyCardEditActivity.this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
        }
    }

    class EditCardIdAndBarcode implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            final Bundle b = new Bundle();
            b.putString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID, cardIdFieldView.getText().toString());
            i.putExtras(b);
            mCardIdAndBarCodeEditorLauncher.launch(i);
        }
    }

    class ChooseCardImage implements View.OnClickListener {
        @Override
        public void onClick(View v) throws NoSuchElementException {
            Bitmap currentImage;
            ImageLocationType imageLocationType;
            ImageView targetView;

            if (v.getId() == R.id.frontImageHolder) {
                CoverageTool.setFunc2Flag(0);
                currentImage = viewModel.getLoyaltyCard().getImageFront(LoyaltyCardEditActivity.this);
                imageLocationType = ImageLocationType.front;
                targetView = cardImageFront;
            } else if (v.getId() == R.id.backImageHolder) {
                CoverageTool.setFunc2Flag(1);
                currentImage = viewModel.getLoyaltyCard().getImageBack(LoyaltyCardEditActivity.this);
                imageLocationType = ImageLocationType.back;
                targetView = cardImageBack;
            } else if (v.getId() == R.id.thumbnail) {
                CoverageTool.setFunc2Flag(2);
                currentImage = viewModel.getLoyaltyCard().getImageThumbnail(LoyaltyCardEditActivity.this);
                imageLocationType = ImageLocationType.icon;
                targetView = thumbnail;
            } else {
                CoverageTool.setFunc2Flag(3);
                throw new IllegalArgumentException("Invalid IMAGE ID " + v.getId());
            }

            LinkedHashMap<String, Callable<Void>> cardOptions = new LinkedHashMap<>();
            if (currentImage != null && v.getId() != R.id.thumbnail) {
                CoverageTool.setFunc2Flag(4);
                cardOptions.put(getString(R.string.removeImage), () -> {
                    setCardImage(imageLocationType, targetView, null, true);
                    return null;
                });
            }
            else
                CoverageTool.setFunc2Flag(5);
            

            if (v.getId() == R.id.thumbnail) {
                CoverageTool.setFunc2Flag(6);
                cardOptions.put(getString(R.string.selectColor), () -> {
                    ColorPickerDialog.Builder dialogBuilder = ColorPickerDialog.newBuilder();

                    if (viewModel.getLoyaltyCard().headerColor != null) {
                        CoverageTool.setFunc2Flag(7);
                        dialogBuilder.setColor(viewModel.getLoyaltyCard().headerColor);
                    }
                    else
                        CoverageTool.setFunc2Flag(8);

                    ColorPickerDialog dialog = dialogBuilder.create();
                    dialog.show(getSupportFragmentManager(), "color-picker-dialog");
                    return null;
                });
            }
            else
                CoverageTool.setFunc2Flag(9);

            cardOptions.put(getString(R.string.takePhoto), () -> {
                int permissionRequestType;

                if (v.getId() == R.id.frontImageHolder) {
                    CoverageTool.setFunc2Flag(10);
                    permissionRequestType = PERMISSION_REQUEST_CAMERA_IMAGE_FRONT;
                } else if (v.getId() == R.id.backImageHolder) {
                    CoverageTool.setFunc2Flag(11);
                    permissionRequestType = PERMISSION_REQUEST_CAMERA_IMAGE_BACK;
                } else if (v.getId() == R.id.thumbnail) {
                    CoverageTool.setFunc2Flag(12);
                    permissionRequestType = PERMISSION_REQUEST_CAMERA_IMAGE_ICON;
                } else {
                    CoverageTool.setFunc2Flag(13);
                    throw new IllegalArgumentException("Unknown ID type " + v.getId());
                }

                PermissionUtils.requestCameraPermission(LoyaltyCardEditActivity.this, permissionRequestType);

                return null;
            });

            cardOptions.put(getString(R.string.addFromImage), () -> {
                int permissionRequestType;

                if (v.getId() == R.id.frontImageHolder) {
                    CoverageTool.setFunc2Flag(14);
                    permissionRequestType = PERMISSION_REQUEST_STORAGE_IMAGE_FRONT;
                } else if (v.getId() == R.id.backImageHolder) {
                    CoverageTool.setFunc2Flag(15);
                    permissionRequestType = PERMISSION_REQUEST_STORAGE_IMAGE_BACK;
                } else if (v.getId() == R.id.thumbnail) {
                    CoverageTool.setFunc2Flag(16);
                    permissionRequestType = PERMISSION_REQUEST_STORAGE_IMAGE_ICON;
                } else {
                    CoverageTool.setFunc2Flag(17);
                    throw new IllegalArgumentException("Unknown ID type " + v.getId());
                }

                PermissionUtils.requestStorageReadPermission(LoyaltyCardEditActivity.this, permissionRequestType);

                return null;
            });

            if (v.getId() == R.id.thumbnail) {
                CoverageTool.setFunc2Flag(18);
                Bitmap imageFront = viewModel.getLoyaltyCard().getImageFront(LoyaltyCardEditActivity.this);
                if (imageFront != null) {
                    CoverageTool.setFunc2Flag(19);
                    cardOptions.put(getString(R.string.useFrontImage), () -> {
                        setThumbnailImage(Utils.resizeBitmap(imageFront, Utils.BITMAP_SIZE_SMALL));

                        return null;
                    });
                }
                else
                    CoverageTool.setFunc2Flag(20);

                Bitmap imageBack = viewModel.getLoyaltyCard().getImageBack(LoyaltyCardEditActivity.this);
                if (imageBack != null) {
                    CoverageTool.setFunc2Flag(21);
                    cardOptions.put(getString(R.string.useBackImage), () -> {
                        setThumbnailImage(Utils.resizeBitmap(imageBack, Utils.BITMAP_SIZE_SMALL));

                        return null;
                    });
                }
                else
                    CoverageTool.setFunc2Flag(22);
            }
            else
                CoverageTool.setFunc2Flag(23);

            int titleResource;

            if (v.getId() == R.id.frontImageHolder) {
                CoverageTool.setFunc2Flag(24);
                titleResource = R.string.setFrontImage;
            } else if (v.getId() == R.id.backImageHolder) {
                CoverageTool.setFunc2Flag(25);
                titleResource = R.string.setBackImage;
            } else if (v.getId() == R.id.thumbnail) {
                CoverageTool.setFunc2Flag(26);
                titleResource = R.string.setIcon;
            } else {
                CoverageTool.setFunc2Flag(27);
                throw new IllegalArgumentException("Unknown ID type " + v.getId());
            }

            new MaterialAlertDialogBuilder(LoyaltyCardEditActivity.this)
                    .setTitle(getString(titleResource))
                    .setItems(cardOptions.keySet().toArray(new CharSequence[cardOptions.size()]), (dialog, which) -> {
                        Iterator<Callable<Void>> callables = cardOptions.values().iterator();
                        Callable<Void> callable = callables.next();

                        for (int i = 0; i < which; i++) {
                            callable = callables.next();
                        }
                        CoverageTool.setFunc2Flag(28);

                        try {
                            CoverageTool.setFunc2Flag(29);
                            callable.call();
                        } catch (Exception e) {
                            CoverageTool.setFunc2Flag(30);
                            e.printStackTrace();

                            // Rethrow as NoSuchElementException
                            // This isn't really true, but a View.OnClickListener doesn't allow throwing other types
                            throw new NoSuchElementException(e.getMessage());
                        }
                    })
                    .show();
            CoverageTool.setFunc2Flag(31);//the last one        
        }
    }

    // ColorPickerDialogListener callback used by the ColorPickerDialog created in ChooseCardImage to set the thumbnail color
    // We don't need to set or check the dialogId since it's only used for that single dialog
    @Override
    public void onColorSelected(int dialogId, int color) {
        // Save new colour
        setLoyaltyCardHeaderColor(color);

        // Unset image if set
        setThumbnailImage(null);
    }

    // ColorPickerDialogListener callback
    @Override
    public void onDialogDismissed(int dialogId) {
        // Nothing to do, no change made
    }

    private void showDatePicker(
            LoyaltyCardField loyaltyCardField,
            @Nullable Date selectedDate,
            @Nullable Date minDate,
            @Nullable Date maxDate
    ) {
        // Create a new instance of MaterialDatePicker and return it
        long startDate = minDate != null ? minDate.getTime() : getDefaultMinDateOfDatePicker();
        long endDate = maxDate != null ? maxDate.getTime() : getDefaultMaxDateOfDatePicker();

        CalendarConstraints.DateValidator dateValidator;
        switch (loyaltyCardField) {
            case validFrom:
                dateValidator = DateValidatorPointBackward.before(endDate);
                break;
            case expiry:
                dateValidator = DateValidatorPointForward.from(startDate);
                break;
            default:
                throw new AssertionError("Unexpected field: " + loyaltyCardField);
        }

        CalendarConstraints calendarConstraints = new CalendarConstraints.Builder()
                .setValidator(dateValidator)
                .setStart(startDate)
                .setEnd(endDate)
                .build();

        // Use the selected date as the default date in the picker
        final Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        }

        MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(calendar.getTimeInMillis())
                .setCalendarConstraints(calendarConstraints)
                .build();

        // Required to handle configuration changes
        // See https://github.com/material-components/material-components-android/issues/1688
        viewModel.setTempLoyaltyCardField(loyaltyCardField);
        getSupportFragmentManager().addFragmentOnAttachListener((fragmentManager, fragment) -> {
            if (fragment instanceof MaterialDatePicker && Objects.equals(fragment.getTag(), PICK_DATE_REQUEST_KEY)) {
                ((MaterialDatePicker<Long>) fragment).addOnPositiveButtonClickListener(selection -> {
                    Bundle args = new Bundle();
                    args.putLong(NEWLY_PICKED_DATE_ARGUMENT_KEY, selection);
                    getSupportFragmentManager().setFragmentResult(PICK_DATE_REQUEST_KEY, args);
                });
            }
        });

        materialDatePicker.show(getSupportFragmentManager(), PICK_DATE_REQUEST_KEY);
    }

    // Required to handle configuration changes
    // See https://github.com/material-components/material-components-android/issues/1688
    private void setMaterialDatePickerResultListener() {
        MaterialDatePicker<Long> fragment = (MaterialDatePicker<Long>) getSupportFragmentManager().findFragmentByTag(PICK_DATE_REQUEST_KEY);
        if (fragment != null) {
            fragment.addOnPositiveButtonClickListener(selection -> {
                Bundle args = new Bundle();
                args.putLong(NEWLY_PICKED_DATE_ARGUMENT_KEY, selection);
                getSupportFragmentManager().setFragmentResult(PICK_DATE_REQUEST_KEY, args);
            });
        }

        getSupportFragmentManager().setFragmentResultListener(
                PICK_DATE_REQUEST_KEY,
                this,
                (requestKey, result) -> {
                    long selection = result.getLong(NEWLY_PICKED_DATE_ARGUMENT_KEY);

                    Date newDate = new Date(selection);

                    LoyaltyCardField tempLoyaltyCardField = viewModel.getTempLoyaltyCardField();
                    if (tempLoyaltyCardField == null) {
                        throw new AssertionError("tempLoyaltyCardField is null unexpectedly!");
                    }

                    switch (tempLoyaltyCardField) {
                        case validFrom:
                            formatDateField(LoyaltyCardEditActivity.this, validFromField, newDate);
                            setLoyaltyCardValidFrom(newDate);
                            break;
                        case expiry:
                            formatDateField(LoyaltyCardEditActivity.this, expiryField, newDate);
                            setLoyaltyCardExpiry(newDate);
                            break;
                        default:
                            throw new AssertionError("Unexpected field: " + tempLoyaltyCardField);
                    }
                }
        );
    }

    private long getDefaultMinDateOfDatePicker() {
        Calendar minDateCalendar = Calendar.getInstance();
        minDateCalendar.set(1970, 0, 1);
        return minDateCalendar.getTimeInMillis();
    }

    private long getDefaultMaxDateOfDatePicker() {
        Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.set(2100, 11, 31);
        return maxDateCalendar.getTimeInMillis();
    }

    private void doSave() {
        if (isFinishing()) {
            // If we are done saving, ignore any queued up save button presses
            return;
        }

        if (tempStoredOldBarcodeValue != null) {
            askBarcodeChange(this::doSave);
            return;
        }

        boolean hasError = false;

        if (viewModel.getLoyaltyCard().store.isEmpty()) {
            storeFieldEdit.setError(getString(R.string.field_must_not_be_empty));

            // Focus element
            selectTab(0);
            storeFieldEdit.requestFocus();

            hasError = true;
        }

        if (viewModel.getLoyaltyCard().cardId.isEmpty()) {
            cardIdFieldView.setError(getString(R.string.field_must_not_be_empty));

            // Focus element if first error element
            if (!hasError) {
                selectTab(0);
                cardIdFieldView.requestFocus();
                hasError = true;
            }
        }

        if (!validBalance) {
            balanceField.setError(getString(R.string.balanceParsingFailed));

            // Focus element if first error element
            if (!hasError) {
                selectTab(1);
                balanceField.requestFocus();
                hasError = true;
            }
        }

        if (hasError) {
            return;
        }

        List<Group> selectedGroups = new ArrayList<>();

        for (Integer chipId : groupsChips.getCheckedChipIds()) {
            Chip chip = groupsChips.findViewById(chipId);
            selectedGroups.add((Group) chip.getTag());
        }

        // Both update and new card save with lastUsed set to null
        // This makes the DBHelper set it to the current date
        // So that new and edited card are always on top when sorting by recently used
        if (viewModel.getUpdateLoyaltyCard()) {
            DBHelper.updateLoyaltyCard(mDatabase, viewModel.getLoyaltyCardId(), viewModel.getLoyaltyCard().store, viewModel.getLoyaltyCard().note, viewModel.getLoyaltyCard().validFrom, viewModel.getLoyaltyCard().expiry, viewModel.getLoyaltyCard().balance, viewModel.getLoyaltyCard().balanceType, viewModel.getLoyaltyCard().cardId, viewModel.getLoyaltyCard().barcodeId, viewModel.getLoyaltyCard().barcodeType, viewModel.getLoyaltyCard().barcodeEncoding, viewModel.getLoyaltyCard().headerColor, viewModel.getLoyaltyCard().starStatus, null, viewModel.getLoyaltyCard().archiveStatus);
        } else {
            viewModel.setLoyaltyCardId((int) DBHelper.insertLoyaltyCard(mDatabase, viewModel.getLoyaltyCard().store, viewModel.getLoyaltyCard().note, viewModel.getLoyaltyCard().validFrom, viewModel.getLoyaltyCard().expiry, viewModel.getLoyaltyCard().balance, viewModel.getLoyaltyCard().balanceType, viewModel.getLoyaltyCard().cardId, viewModel.getLoyaltyCard().barcodeId, viewModel.getLoyaltyCard().barcodeType, viewModel.getLoyaltyCard().barcodeEncoding, viewModel.getLoyaltyCard().headerColor, 0, null, 0));
        }

        try {
            Utils.saveCardImage(this, viewModel.getLoyaltyCard().getImageFront(this), viewModel.getLoyaltyCardId(), ImageLocationType.front);
            Utils.saveCardImage(this, viewModel.getLoyaltyCard().getImageBack(this), viewModel.getLoyaltyCardId(), ImageLocationType.back);
            Utils.saveCardImage(this, viewModel.getLoyaltyCard().getImageThumbnail(this), viewModel.getLoyaltyCardId(), ImageLocationType.icon);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        DBHelper.setLoyaltyCardGroups(mDatabase, viewModel.getLoyaltyCardId(), selectedGroups);

        ShortcutHelper.updateShortcuts(this);

        if (viewModel.getDuplicateFromLoyaltyCardId()) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_add_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            askBeforeQuitIfChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startCropper(String sourceImagePath) {
        startCropperUri(Uri.parse("file://" + sourceImagePath));
    }

    public void startCropperUri(Uri sourceUri) {
        Log.d("cropper", "launching cropper with image " + sourceUri.getPath());
        File cropOutput = Utils.createTempFile(this, TEMP_CROP_IMAGE_NAME);
        Uri destUri = Uri.parse("file://" + cropOutput.getAbsolutePath());
        Log.d("cropper", "asking cropper to output to " + destUri.toString());

        if (requestedFrontImage()) {
            mCropperOptions.setToolbarTitle(getResources().getString(R.string.setFrontImage));
        } else if (requestedBackImage()) {
            mCropperOptions.setToolbarTitle(getResources().getString(R.string.setBackImage));
        } else if (requestedIcon()) {
            mCropperOptions.setToolbarTitle(getResources().getString(R.string.setIcon));
        } else {
            Toast.makeText(this, R.string.generic_error_please_retry, Toast.LENGTH_LONG).show();
            return;
        }

        if (requestedIcon()) {
            setCropperOptions(true, 0f, 0f);
        } else {
            // sniff the input image for width and height to work around a ucrop bug
            Bitmap image = null;
            try {
                image = BitmapFactory.decodeStream(getContentResolver().openInputStream(sourceUri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d("cropper", "failed opening bitmap for initial width and height for ucrop " + sourceUri.toString());
            }
            if (image == null) {
                Log.d("cropper", "failed loading bitmap for initial width and height for ucrop " + sourceUri.toString());
                setCropperOptions(true, 0f, 0f);
            } else {
                try {
                    Bitmap imageRotated = Utils.rotateBitmap(image, new ExifInterface(getContentResolver().openInputStream(sourceUri)));
                    setCropperOptions(false, imageRotated.getWidth(), imageRotated.getHeight());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.d("cropper", "failed opening image for exif reading before setting initial width and height for ucrop");
                    setCropperOptions(false, image.getWidth(), image.getHeight());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("cropper", "exif reading failed before setting initial width and height for ucrop");
                    setCropperOptions(false, image.getWidth(), image.getHeight());
                }
            }
        }
        Intent ucropIntent = UCrop.of(
                sourceUri,
                destUri
        ).withOptions(mCropperOptions)
                .getIntent(this);
        ucropIntent.setClass(this, UCropWrapper.class);
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            // send toolbar font details to ucrop wrapper
            View child = toolbar.getChildAt(i);
            if (child instanceof AppCompatTextView) {
                AppCompatTextView childTextView = (AppCompatTextView) child;
                ucropIntent.putExtra(UCropWrapper.UCROP_TOOLBAR_TYPEFACE_STYLE, childTextView.getTypeface().getStyle());
                break;
            }
        }
        mCropperLauncher.launch(ucropIntent);
    }

    private void generateBarcode() {
        viewModel.getTaskHandler().flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false);

        String cardIdString = viewModel.getLoyaltyCard().barcodeId != null ? viewModel.getLoyaltyCard().barcodeId : viewModel.getLoyaltyCard().cardId;
        CatimaBarcode barcodeFormat = viewModel.getLoyaltyCard().barcodeType;
        Charset barcodeEncoding = viewModel.getLoyaltyCard().barcodeEncoding;

        if (cardIdString == null || cardIdString.isEmpty() || barcodeFormat == null) {
            barcodeImageLayout.setVisibility(View.GONE);
            return;
        }

        barcodeImageLayout.setVisibility(View.VISIBLE);

        if (barcodeImage.getHeight() == 0) {
            Log.d(TAG, "ImageView size is not known known at start, waiting for load");
            // The size of the ImageView is not yet available as it has not
            // yet been drawn. Wait for it to be drawn so the size is available.
            barcodeImage.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            barcodeImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            Log.d(TAG, "ImageView size now known");
                            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getApplicationContext(), barcodeImage, cardIdString, barcodeFormat, barcodeEncoding, null, false, LoyaltyCardEditActivity.this, true, false);
                            viewModel.getTaskHandler().executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
                        }
                    });
        } else {
            Log.d(TAG, "ImageView size known known, creating barcode");
            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getApplicationContext(), barcodeImage, cardIdString, barcodeFormat, barcodeEncoding, null, false, this, true, false);
            viewModel.getTaskHandler().executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
        }
    }

    private void generateIcon(String store) {
        Integer headerColor = viewModel.getLoyaltyCard().headerColor;

        if (headerColor == null) {
            return;
        }

        if (viewModel.getLoyaltyCard().getImageThumbnail(this) == null) {
            thumbnail.setBackgroundColor(headerColor);

            LetterBitmap letterBitmap = Utils.generateIcon(this, store, headerColor);

            if (letterBitmap != null) {
                thumbnail.setImageBitmap(letterBitmap.getLetterTile());
            } else {
                thumbnail.setImageBitmap(null);
            }
        }

        thumbnail.setMinimumWidth(thumbnail.getHeight());
    }

    private void showPart(String part) {
        if (tempStoredOldBarcodeValue != null) {
            askBarcodeChange(() -> showPart(part));
            return;
        }

        View cardPart = binding.cardPart;
        View optionsPart = binding.optionsPart;
        View picturesPart = binding.picturesPart;

        if (getString(R.string.card).equals(part)) {
            cardPart.setVisibility(View.VISIBLE);
            optionsPart.setVisibility(View.GONE);
            picturesPart.setVisibility(View.GONE);

            // Redraw barcode due to size change (Visibility.GONE sets it to 0)
            generateBarcode();
        } else if (getString(R.string.options).equals(part)) {
            cardPart.setVisibility(View.GONE);
            optionsPart.setVisibility(View.VISIBLE);
            picturesPart.setVisibility(View.GONE);
        } else if (getString(R.string.photos).equals(part)) {
            cardPart.setVisibility(View.GONE);
            optionsPart.setVisibility(View.GONE);
            picturesPart.setVisibility(View.VISIBLE);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void currencyPrioritizeLocaleSymbols(ArrayList<String> currencyList, Locale locale) {
        try {
            String currencySymbol = getCurrencySymbol(Currency.getInstance(locale));
            currencyList.remove(currencySymbol);
            currencyList.add(0, currencySymbol);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Could not get currency data for locale info: " + e);
        }
    }

    private String getCurrencySymbol(final Currency currency) {
        // Workaround for Android bug where the output of Currency.getSymbol() changes.
        return currencySymbols.get(currency.getCurrencyCode());
    }
}
