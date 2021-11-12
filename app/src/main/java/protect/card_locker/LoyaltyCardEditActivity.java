package protect.card_locker;

import android.Manifest;
import android.annotation.SuppressLint;

import android.app.AlarmManager;

import android.app.Activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;

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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import java.util.concurrent.Callable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;
import androidx.palette.graphics.Palette;
import protect.card_locker.async.TaskHandler;

public class LoyaltyCardEditActivity extends CatimaAppCompatActivity {
    private static final String TAG = "Catima";

    private final String STATE_TAB_INDEX = "savedTab";
    private final String STATE_TEMP_CARD = "tempLoyaltyCard";
    private final String STATE_REQUESTED_IMAGE = "requestedImage";
    private final String STATE_FRONT_IMAGE_UNSAVED = "frontImageUnsaved";
    private final String STATE_BACK_IMAGE_UNSAVED = "backImageUnsaved";
    private final String STATE_ICON_UNSAVED = "iconUnsaved";
    private final String STATE_UPDATE_LOYALTY_CARD = "updateLoyaltyCard";
    private final String STATE_HAS_CHANGED = "hasChange";
    private final String STATE_FRONT_IMAGE_REMOVED = "frontImageRemoved";
    private final String STATE_BACK_IMAGE_REMOVED = "backImageRemoved";
    private final String STATE_ICON_REMOVED = "iconRemoved";

    private final String TEMP_CAMERA_IMAGE_NAME = LoyaltyCardEditActivity.class.getSimpleName() + "_camera_image.jpg";
    private final String TEMP_CROP_IMAGE_NAME = LoyaltyCardEditActivity.class.getSimpleName() + "_crop_image.png";
    private final Bitmap.CompressFormat TEMP_CROP_IMAGE_FORMAT = Bitmap.CompressFormat.PNG;

    private final String TEMP_UNSAVED_FRONT_IMAGE_NAME = LoyaltyCardEditActivity.class.getSimpleName() + "_front_image.png";
    private final String TEMP_UNSAVED_BACK_IMAGE_NAME = LoyaltyCardEditActivity.class.getSimpleName() + "_back_image.png";
    private final String TEMP_UNSAVED_ICON_NAME = LoyaltyCardEditActivity.class.getSimpleName() + "_icon.png";
    private final Bitmap.CompressFormat TEMP_UNSAVED_IMAGE_FORMAT = Bitmap.CompressFormat.PNG;

    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_FRONT = 100;
    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_BACK = 101;
    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_ICON = 102;

    public static final String BUNDLE_ID = "id";
    public static final String BUNDLE_UPDATE = "update";
    public static final String BUNDLE_CARDID = "cardId";
    public static final String BUNDLE_BARCODEID = "barcodeId";
    public static final String BUNDLE_BARCODETYPE = "barcodeType";
    public static final String BUNDLE_ADDGROUP = "addGroup";

    TabLayout tabs;
    View showRemindersLayout;

    ImageView thumbnail;
    EditText storeFieldEdit;
    EditText noteFieldEdit;
    ChipGroup groupsChips;
    AutoCompleteTextView expiryField;
    EditText balanceField;
    AutoCompleteTextView balanceCurrencyField;
    TextView cardIdFieldView;
    AutoCompleteTextView barcodeIdField;
    AutoCompleteTextView barcodeTypeField;
    ImageView barcodeImage;
    View barcodeImageLayout;
    View barcodeCaptureLayout;
    View cardImageFrontHolder;
    View cardImageBackHolder;
    ImageView cardImageFront;
    ImageView cardImageBack;
    SharedPreferences sharedPreferences;
    TextView daysTextView, dateReminderView, timeReminderTextView;

    Button enterButton;
    Button addReminderBtn;
    static int hour, minute, days = 1;
    static Calendar delay;
    static long expiryInMillis, reminderDateInMillis;
    CardView reminderCardView;
    ImageButton cancelReminderBtn;
    static String formattedDate;

    int loyaltyCardId;
    boolean updateLoyaltyCard;
    String cardId;
    String barcodeId;
    String barcodeType;
    String addGroup;

    Uri importLoyaltyCardUri = null;

    DBHelper db;
    ImportURIHelper importUriHelper;

    boolean hasChanged = false;
    String tempStoredOldBarcodeValue = null;
    boolean initDone = false;
    boolean onResuming = false;
    AlertDialog confirmExitDialog = null;

    boolean validBalance = true;
    Runnable warnOnInvalidBarcodeType;

    HashMap<String, Currency> currencies = new HashMap<>();

    LoyaltyCard tempLoyaltyCard;
    private static boolean hasReminderChanged = false;

    ActivityResultLauncher<Uri> mPhotoTakerLauncher;
    ActivityResultLauncher<Intent> mPhotoPickerLauncher;
    ActivityResultLauncher<Intent> mCardIdAndBarCodeEditorLauncher;

    ActivityResultLauncher<Intent> mCropperLauncher;
    int mRequestedImage = 0;
    int mCropperFinishedType = 0;
    UCrop.Options mCropperOptions;


    boolean mFrontImageUnsaved = false;
    boolean mBackImageUnsaved = false;
    boolean mIconUnsaved = false;

    boolean mFrontImageRemoved = false;
    boolean mBackImageRemoved = false;
    boolean mIconRemoved = false;

    final private TaskHandler mTasks = new TaskHandler();

    private static LoyaltyCard updateTempState(LoyaltyCard loyaltyCard, LoyaltyCardField fieldName, Object value) {
        return new LoyaltyCard(
                (int) (fieldName == LoyaltyCardField.id ? value : loyaltyCard.id),
                (String) (fieldName == LoyaltyCardField.store ? value : loyaltyCard.store),
                (String) (fieldName == LoyaltyCardField.note ? value : loyaltyCard.note),
                (Date) (fieldName == LoyaltyCardField.expiry ? value : loyaltyCard.expiry),
                (BigDecimal) (fieldName == LoyaltyCardField.balance ? value : loyaltyCard.balance),
                (Currency) (fieldName == LoyaltyCardField.balanceType ? value : loyaltyCard.balanceType),
                (String) (fieldName == LoyaltyCardField.cardId ? value : loyaltyCard.cardId),
                (String) (fieldName == LoyaltyCardField.barcodeId ? value : loyaltyCard.barcodeId),
                (CatimaBarcode) (fieldName == LoyaltyCardField.barcodeType ? value : loyaltyCard.barcodeType),
                (Integer) (fieldName == LoyaltyCardField.headerColor ? value : loyaltyCard.headerColor),
                (int) (fieldName == LoyaltyCardField.starStatus ? value : loyaltyCard.starStatus),
                Utils.getUnixTime(), 100
        );
    }

    private void updateTempState(LoyaltyCardField fieldName, Object value) {
        tempLoyaltyCard = updateTempState(tempLoyaltyCard, fieldName, value);

        hasChanged = true;
    }

    private void extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt(BUNDLE_ID) : 0;
        updateLoyaltyCard = b != null && b.getBoolean(BUNDLE_UPDATE, false);

        cardId = b != null ? b.getString(BUNDLE_CARDID) : null;
        barcodeId = b != null ? b.getString(BUNDLE_BARCODEID) : null;
        barcodeType = b != null ? b.getString(BUNDLE_BARCODETYPE) : null;
        addGroup = b != null ? b.getString(BUNDLE_ADDGROUP) : null;

        importLoyaltyCardUri = intent.getData();

        Log.d(TAG, "View activity: id=" + loyaltyCardId
                + ", updateLoyaltyCard=" + updateLoyaltyCard);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        tabs = findViewById(R.id.tabs);
        savedInstanceState.putInt(STATE_TAB_INDEX, tabs.getSelectedTabPosition());
        savedInstanceState.putParcelable(STATE_TEMP_CARD, tempLoyaltyCard);
        savedInstanceState.putInt(STATE_REQUESTED_IMAGE, mRequestedImage);

        Object cardImageFrontObj = cardImageFront.getTag();
        if (mFrontImageUnsaved && (cardImageFrontObj instanceof Bitmap) && Utils.saveTempImage(this, (Bitmap) cardImageFrontObj, TEMP_UNSAVED_FRONT_IMAGE_NAME, TEMP_UNSAVED_IMAGE_FORMAT) != null) {
            savedInstanceState.putInt(STATE_FRONT_IMAGE_UNSAVED, 1);
        } else {
            savedInstanceState.putInt(STATE_FRONT_IMAGE_UNSAVED, 0);
        }

        Object cardImageBackObj = cardImageBack.getTag();
        if (mBackImageUnsaved && (cardImageBackObj instanceof Bitmap) && Utils.saveTempImage(this, (Bitmap) cardImageBackObj, TEMP_UNSAVED_BACK_IMAGE_NAME, TEMP_UNSAVED_IMAGE_FORMAT) != null) {
            savedInstanceState.putInt(STATE_BACK_IMAGE_UNSAVED, 1);
        } else {
            savedInstanceState.putInt(STATE_BACK_IMAGE_UNSAVED, 0);
        }

        Object thumbnailObj = thumbnail.getTag();
        if (mIconUnsaved && (thumbnailObj instanceof Bitmap) && Utils.saveTempImage(this, (Bitmap) thumbnailObj, TEMP_UNSAVED_ICON_NAME, TEMP_UNSAVED_IMAGE_FORMAT) != null) {
            savedInstanceState.putInt(STATE_ICON_UNSAVED, 1);
        } else {
            savedInstanceState.putInt(STATE_ICON_UNSAVED, 0);
        }

        savedInstanceState.putInt(STATE_UPDATE_LOYALTY_CARD, updateLoyaltyCard ? 1 : 0);
        savedInstanceState.putInt(STATE_HAS_CHANGED, hasChanged ? 1 : 0);
        savedInstanceState.putInt(STATE_FRONT_IMAGE_REMOVED, mFrontImageRemoved ? 1 : 0);
        savedInstanceState.putInt(STATE_BACK_IMAGE_REMOVED, mBackImageRemoved ? 1 : 0);
        savedInstanceState.putInt(STATE_ICON_REMOVED, mIconRemoved ? 1 : 0);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        tempLoyaltyCard = savedInstanceState.getParcelable(STATE_TEMP_CARD);
        super.onRestoreInstanceState(savedInstanceState);
        tabs = findViewById(R.id.tabs);
        tabs.selectTab(tabs.getTabAt(savedInstanceState.getInt(STATE_TAB_INDEX)));
        mRequestedImage = savedInstanceState.getInt(STATE_REQUESTED_IMAGE);
        mFrontImageUnsaved = savedInstanceState.getInt(STATE_FRONT_IMAGE_UNSAVED) == 1;
        mBackImageUnsaved = savedInstanceState.getInt(STATE_BACK_IMAGE_UNSAVED) == 1;
        mIconUnsaved = savedInstanceState.getInt(STATE_ICON_UNSAVED) == 1;
        updateLoyaltyCard = savedInstanceState.getInt(STATE_UPDATE_LOYALTY_CARD) == 1;
        hasChanged = savedInstanceState.getInt(STATE_HAS_CHANGED) == 1;
        mFrontImageRemoved = savedInstanceState.getInt(STATE_FRONT_IMAGE_REMOVED) == 1;
        mBackImageRemoved = savedInstanceState.getInt(STATE_BACK_IMAGE_REMOVED) == 1;
        mIconRemoved = savedInstanceState.getInt(STATE_ICON_REMOVED) == 1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.loyalty_card_edit_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();


        if (actionBar != null) {

            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        extractIntentFields(getIntent());

        db = new DBHelper(this);
        importUriHelper = new ImportURIHelper(this);

        for (Currency currency : Currency.getAvailableCurrencies()) {
            currencies.put(currency.getSymbol(), currency);
        }

        tabs = findViewById(R.id.tabs);
        thumbnail = findViewById(R.id.thumbnail);
        storeFieldEdit = findViewById(R.id.storeNameEdit);
        noteFieldEdit = findViewById(R.id.noteEdit);
        groupsChips = findViewById(R.id.groupChips);
        expiryField = findViewById(R.id.expiryField);
        balanceField = findViewById(R.id.balanceField);
        balanceCurrencyField = findViewById(R.id.balanceCurrencyField);
        cardIdFieldView = findViewById(R.id.cardIdView);
        barcodeIdField = findViewById(R.id.barcodeIdField);
        barcodeTypeField = findViewById(R.id.barcodeTypeField);
        barcodeImage = findViewById(R.id.barcode);
        barcodeImageLayout = findViewById(R.id.barcodeLayout);
        barcodeCaptureLayout = findViewById(R.id.barcodeCaptureLayout);
        cardImageFrontHolder = findViewById(R.id.frontImageHolder);
        cardImageBackHolder = findViewById(R.id.backImageHolder);
        cardImageFront = findViewById(R.id.frontImage);
        cardImageBack = findViewById(R.id.backImage);
        addReminderBtn = findViewById(R.id.addReminderBtn);
        showRemindersLayout = findViewById(R.id.showRemindersLayout);
        reminderCardView = findViewById(R.id.reminderCardView);
        daysTextView = findViewById(R.id.daysTextView);
        dateReminderView = findViewById(R.id.dateReminderView);
        timeReminderTextView = findViewById(R.id.timeReminderTextView);
        cancelReminderBtn = findViewById(R.id.cancelReminderBtn);


        enterButton = findViewById(R.id.enterButton);
        cardImageFront.setBackgroundColor(getThemeColor());
        cardImageBack.setBackgroundColor(getThemeColor());

        showRemindersLayout.setVisibility(View.GONE);

        delay = Calendar.getInstance();
        sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferenceDateTime), MODE_PRIVATE);


        warnOnInvalidBarcodeType = () -> {
            if (!(boolean) barcodeImage.getTag()) {
                Toast.makeText(LoyaltyCardEditActivity.this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
            }
        };


        storeFieldEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTempState(LoyaltyCardField.store, s.toString());
                generateIcon(s.toString());
            }
        });

        noteFieldEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTempState(LoyaltyCardField.note, s.toString());
            }
        });

        addReminderBtn.setOnClickListener(view -> {

            Calendar c = Calendar.getInstance();

            Log.i(TAG, "onCreate: addReminderBtn clicked");
            Log.i(TAG, "onCreate: expiryInMillis "+expiryInMillis);

            if (expiryInMillis==0) {

                Snackbar.make(getCurrentFocus(), R.string.set_expiry_date, BaseTransientBottomBar.LENGTH_LONG).show();

            } else if (expiryInMillis - (24 * 3600 * 1000) <= c.getTimeInMillis()) {

                Log.i(TAG, "onCreate: expiryInMillis = " + expiryInMillis);
                Log.i(TAG, "onCreate: Card Is expired");

                Snackbar.make(getCurrentFocus(), R.string.card_expired, BaseTransientBottomBar.LENGTH_LONG).show();
            } else {
                NumberPickerFragment pickerFragment = new NumberPickerFragment(showRemindersLayout, loyaltyCardId, addReminderBtn);
                pickerFragment.show(getSupportFragmentManager(), "NumberPicker");
            }
        });

        cancelReminderBtn.setOnClickListener(view -> {

            showRemindersLayout.setVisibility(View.GONE);
            addReminderBtn.setText(R.string.add_reminder);
        });


        expiryField.addTextChangedListener(new SimpleTextWatcher() {
            CharSequence lastValue;


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                Log.i(TAG, "beforeTextChanged: ExpiryTag " + expiryField.getTag());
                lastValue = s;

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.toString().equals(getString(R.string.never))) {
                    expiryField.setTag(null);
                } else if (s.toString().equals(getString(R.string.chooseExpiryDate))) {
                    if (!lastValue.toString().equals(getString(R.string.chooseExpiryDate))) {
                        expiryField.setText(lastValue);
                    }
                    DialogFragment datePickerFragment = new DatePickerFragment(LoyaltyCardEditActivity.this, expiryField);
                    datePickerFragment.show(getSupportFragmentManager(), "datePicker");

                }

                updateTempState(LoyaltyCardField.expiry, expiryField.getTag());
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> expiryList = new ArrayList<>();
                expiryList.add(0, getString(R.string.never));
                expiryList.add(1, getString(R.string.chooseExpiryDate));
                ArrayAdapter<String> expiryAdapter = new ArrayAdapter<>(LoyaltyCardEditActivity.this, android.R.layout.select_dialog_item, expiryList);
                expiryField.setAdapter(expiryAdapter);
            }
        });


        balanceField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(tempLoyaltyCard.balance, tempLoyaltyCard.balanceType));
            }
        });

        balanceField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    BigDecimal balance = Utils.parseCurrency(s.toString(), Utils.currencyHasDecimals(tempLoyaltyCard.balanceType));
                    updateTempState(LoyaltyCardField.balance, balance);
                    validBalance = true;

                } catch (NumberFormatException e) {
                    validBalance = false;
                    e.printStackTrace();
                }
            }
        });

        balanceCurrencyField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Currency currency;

                if (s.toString().equals(getString(R.string.points))) {
                    currency = null;
                } else {
                    currency = currencies.get(s.toString());
                }

                updateTempState(LoyaltyCardField.balanceType, currency);

                if (tempLoyaltyCard.balance != null) {
                    balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(tempLoyaltyCard.balance, currency));
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

                    for (int i = locales.size() - 1; i > 0; i--) {
                        Locale locale = locales.get(i);
                        String currencySymbol = Currency.getInstance(locale).getSymbol();
                        currencyList.remove(currencySymbol);
                        currencyList.add(0, currencySymbol);
                    }
                } else {
                    String currencySymbol = Currency.getInstance(getApplicationContext().getResources().getConfiguration().locale).getSymbol();
                    currencyList.remove(currencySymbol);
                    currencyList.add(0, currencySymbol);
                }

                currencyList.add(0, getString(R.string.points));
                ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(LoyaltyCardEditActivity.this, android.R.layout.select_dialog_item, currencyList);
                balanceCurrencyField.setAdapter(currencyAdapter);
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
                        if (tempLoyaltyCard.barcodeId != null) {
                            // If it is not set to "same as Card ID", save as tempStoredOldBarcodeValue
                            tempStoredOldBarcodeValue = barcodeIdField.getText().toString();
                        }
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTempState(LoyaltyCardField.cardId, s.toString());
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

                    updateTempState(LoyaltyCardField.barcodeId, null);
                } else if (s.toString().equals(getString(R.string.setBarcodeId))) {
                    if (!lastValue.toString().equals(getString(R.string.setBarcodeId))) {
                        barcodeIdField.setText(lastValue);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(LoyaltyCardEditActivity.this);
                    builder.setTitle(R.string.setBarcodeId);
                    final EditText input = new EditText(LoyaltyCardEditActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    if (tempLoyaltyCard.barcodeId != null) {
                        input.setText(tempLoyaltyCard.barcodeId);
                    }
                    builder.setView(input);

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
                    updateTempState(LoyaltyCardField.barcodeId, s.toString());
                }

                generateOrHideBarcode();
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
                        updateTempState(LoyaltyCardField.barcodeType, null);
                    } else {
                        try {
                            CatimaBarcode barcodeFormat = CatimaBarcode.fromPrettyName(s.toString());

                            updateTempState(LoyaltyCardField.barcodeType, barcodeFormat);

                            if (!barcodeFormat.isSupported()) {
                                Toast.makeText(LoyaltyCardEditActivity.this, getString(R.string.unsupportedBarcodeType), Toast.LENGTH_LONG).show();
                            }
                        } catch (IllegalArgumentException e) {
                        }
                    }

                    generateOrHideBarcode();
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

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
            public void onTabSelected(TabLayout.Tab tab) {
                showPart(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
            public void onTabReselected(TabLayout.Tab tab) {
                showPart(tab.getText().toString());
            }
        });

        tabs.selectTab(tabs.getTabAt(0));


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
                Intent intent = result.getData();
                if (intent == null) {
                    Log.d("barcode card id editor", "barcode and card id editor picker returned without an intent");
                    return;
                }
                BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(Utils.BARCODE_SCAN, result.getResultCode(), intent, getApplicationContext());

                cardId = barcodeValues.content();
                barcodeType = barcodeValues.format();
                barcodeId = "";
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
                        mFrontImageUnsaved = true;
                        setCardImage(cardImageFront, Utils.resizeBitmap(bitmap, Utils.BITMAP_SIZE_BIG), true);
                    } else if (requestedBackImage()) {
                        mBackImageUnsaved = true;
                        setCardImage(cardImageBack, Utils.resizeBitmap(bitmap, Utils.BITMAP_SIZE_BIG), true);
                    } else {
                        mIconUnsaved = true;
                        setCardImage(thumbnail, Utils.resizeBitmap(bitmap, Utils.BITMAP_SIZE_SMALL), false);
                        thumbnail.setBackgroundColor(Color.TRANSPARENT);
                        setColorFromIcon();
                    }
                    Log.d("cropper", "mRequestedImage: " + mRequestedImage);
                    mCropperFinishedType = mRequestedImage;
                    hasChanged = true;
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
        setCropperTheme();
    }

    // ucrop 2.2.6 initial aspect ratio is glitched when 0x0 is used as the initial ratio option
    // https://github.com/Yalantis/uCrop/blob/281c8e6438d81f464d836fc6b500517144af264a/ucrop/src/main/java/com/yalantis/ucrop/UCropActivity.java#L264
    // so source width height has to be provided for now, depending on whether future versions of ucrop will support 0x0 as the default option
    private void setCropperOptions(boolean oneByOneDefault, float sourceWidth, float sourceHeight) {
        mCropperOptions.setCompressionFormat(TEMP_CROP_IMAGE_FORMAT);
        mCropperOptions.setFreeStyleCropEnabled(true);
        mCropperOptions.setHideBottomControls(false);
        // default aspect ratio workaround
        int selectedByDefault = 1;
        if (oneByOneDefault) {
            selectedByDefault = 0;
        }
        mCropperOptions.setAspectRatioOptions(selectedByDefault,
                new AspectRatio(null, 1, 1),
                new AspectRatio(getResources().getString(R.string.ucrop_label_original).toUpperCase(), sourceWidth, sourceHeight),
                new AspectRatio(getResources().getString(R.string.card).toUpperCase(), 85.6f, 53.98f)
        );
    }

    private void setCropperTheme() {
        mCropperOptions.setToolbarColor(getResources().getColor(R.color.colorPrimary));
        mCropperOptions.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        mCropperOptions.setToolbarWidgetColor(Color.WHITE);
        mCropperOptions.setActiveControlsWidgetColor(getResources().getColor(R.color.colorPrimary));
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);
    }

    private boolean requestedFrontImage() {
        return mRequestedImage == Utils.CARD_IMAGE_FROM_CAMERA_FRONT || mRequestedImage == Utils.CARD_IMAGE_FROM_FILE_FRONT;
    }

    private boolean croppedFrontImage() {
        return mCropperFinishedType == Utils.CARD_IMAGE_FROM_CAMERA_FRONT || mCropperFinishedType == Utils.CARD_IMAGE_FROM_FILE_FRONT;
    }

    private boolean requestedBackImage() {
        return mRequestedImage == Utils.CARD_IMAGE_FROM_CAMERA_BACK || mRequestedImage == Utils.CARD_IMAGE_FROM_FILE_BACK;
    }

    private boolean croppedBackImage() {
        return mCropperFinishedType == Utils.CARD_IMAGE_FROM_CAMERA_BACK || mCropperFinishedType == Utils.CARD_IMAGE_FROM_FILE_BACK;
    }

    private boolean requestedIcon() {
        return mRequestedImage == Utils.CARD_IMAGE_FROM_CAMERA_ICON || mRequestedImage == Utils.CARD_IMAGE_FROM_FILE_ICON;
    }

    private boolean croppedIcon() {
        return mCropperFinishedType == Utils.CARD_IMAGE_FROM_CAMERA_ICON || mCropperFinishedType == Utils.CARD_IMAGE_FROM_FILE_ICON;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

        Log.i(TAG, "onResume : expiryInMillis is set to zero and days to 1 " + expiryInMillis + days);
        expiryInMillis = 0;
        days = 1;


        if (sharedPreferences.getInt(loyaltyCardId + "noOfDays", 0) != 0) {

            showRemindersLayout.setVisibility(View.VISIBLE);
            addReminderBtn.setText(R.string.edit_reminder);

            Calendar c = Calendar.getInstance();
            Date defaultDate = new Date(c.getTimeInMillis());

            int noOfDays = sharedPreferences.getInt(loyaltyCardId + "noOfDays", 0);
            formattedDate = sharedPreferences.getString(loyaltyCardId + "formattedDate",DateFormat.getDateInstance(DateFormat.LONG).format(defaultDate));
            int rawHour = sharedPreferences.getInt(loyaltyCardId + "Hour", 12);
            int rawMinute = sharedPreferences.getInt(loyaltyCardId + "Minute", 0);
            String time = formatTime(rawHour, rawMinute);

            Log.i(TAG, "onCreate: getting values from shared pref ");
            daysTextView.setText(String.valueOf(noOfDays));
            dateReminderView.setText(formattedDate);
            timeReminderTextView.setText(time);
            days = noOfDays;

            c.set(Calendar.HOUR_OF_DAY, rawHour);
            c.set(Calendar.MINUTE, rawMinute);
            Date date = new Date(c.getTimeInMillis());
            timeReminderTextView.setTag(date);

        }

        onResuming = true;


        if (tempLoyaltyCard == null) {
            if (updateLoyaltyCard) {
                tempLoyaltyCard = db.getLoyaltyCard(loyaltyCardId);
                if (tempLoyaltyCard == null) {
                    Log.w(TAG, "Could not lookup loyalty card " + loyaltyCardId);
                    Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            } else if (importLoyaltyCardUri != null) {
                try {
                    tempLoyaltyCard = importUriHelper.parse(importLoyaltyCardUri);
                } catch (InvalidObjectException ex) {
                    Toast.makeText(this, R.string.failedParsingImportUriError, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            } else {
                // New card, use default values
                tempLoyaltyCard = new LoyaltyCard(-1, "", "", null, new BigDecimal("0"), null, "", null, null, null, 0, Utils.getUnixTime(), 100);

            }
        }

        if (!initDone) {
            if (updateLoyaltyCard) {
                setTitle(R.string.editCardTitle);

                if (!mFrontImageUnsaved && !croppedFrontImage() && !mFrontImageRemoved) {
                    setCardImage(cardImageFront, Utils.retrieveCardImage(this, tempLoyaltyCard.id, ImageLocationType.front), true);
                }
                if (!mBackImageUnsaved && !croppedBackImage() && !mBackImageRemoved) {
                    setCardImage(cardImageBack, Utils.retrieveCardImage(this, tempLoyaltyCard.id, ImageLocationType.back), true);
                }
                if (!mIconUnsaved && !croppedIcon() && !mIconRemoved) {
                    setCardImage(thumbnail, Utils.retrieveCardImage(this, tempLoyaltyCard.id, ImageLocationType.icon), false);
                }
            } else {
                setTitle(R.string.addCardTitle);
            }

            if (mFrontImageUnsaved && !croppedFrontImage()) {
                setCardImage(cardImageFront, Utils.loadTempImage(this, TEMP_UNSAVED_FRONT_IMAGE_NAME), true);
            }
            if (mBackImageUnsaved && !croppedBackImage()) {
                setCardImage(cardImageBack, Utils.loadTempImage(this, TEMP_UNSAVED_BACK_IMAGE_NAME), true);
            }
            if (mIconUnsaved && !croppedIcon()) {
                setCardImage(thumbnail, Utils.loadTempImage(this, TEMP_UNSAVED_ICON_NAME), false);
            }
        }

        mCropperFinishedType = 0;

        boolean hadChanges = hasChanged;

        storeFieldEdit.setText(tempLoyaltyCard.store);
        noteFieldEdit.setText(tempLoyaltyCard.note);
        formatExpiryField(this, expiryField, tempLoyaltyCard.expiry);
        formatBalanceCurrencyField(tempLoyaltyCard.balanceType);
        cardIdFieldView.setText(tempLoyaltyCard.cardId);
        barcodeIdField.setText(tempLoyaltyCard.barcodeId != null ? tempLoyaltyCard.barcodeId : getString(R.string.sameAsCardId));
        barcodeTypeField.setText(tempLoyaltyCard.barcodeType != null ? tempLoyaltyCard.barcodeType.prettyName() : getString(R.string.noBarcode));

        if (groupsChips.getChildCount() == 0) {
            List<Group> existingGroups = db.getGroups();

            List<Group> loyaltyCardGroups = db.getLoyaltyCardGroups(loyaltyCardId);

            if (existingGroups.isEmpty()) {
                groupsChips.setVisibility(View.GONE);
            } else {
                groupsChips.setVisibility(View.VISIBLE);
            }

            for (Group group : db.getGroups()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_choice, groupsChips, false);
                chip.setText(group._id);
                chip.setTag(group);

                if (group._id.equals(addGroup)) {
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
                    hasChanged = true;

                    return false;
                });

                groupsChips.addView(chip);
            }
        }

        // Generate random header color
        if (tempLoyaltyCard.headerColor == null) {
            // Select a random color to start out with.
            TypedArray colors = getResources().obtainTypedArray(R.array.letter_tile_colors);
            final int color = (int) (Math.random() * colors.length());
            updateTempState(LoyaltyCardField.headerColor, colors.getColor(color, Color.BLACK));
            colors.recycle();
        }

        // It can't be null because we set it in updateTempState but SpotBugs insists it can be
        // NP_NULL_ON_SOME_PATH: Possible null pointer dereference
        if (tempLoyaltyCard.headerColor != null) {
            thumbnail.setOnClickListener(new ChooseCardImage());
        }

        // Update from intent
        if (barcodeType != null) {
            try {
                barcodeTypeField.setText(CatimaBarcode.fromName(barcodeType).prettyName());
            } catch (IllegalArgumentException e) {
                barcodeTypeField.setText(getString(R.string.noBarcode));
            }
        }

        if (cardId != null) {
            cardIdFieldView.setText(cardId);
        }

        if (barcodeId != null) {
            if (!barcodeId.isEmpty()) {
                barcodeIdField.setText(barcodeId);
            } else {
                barcodeIdField.setText(getString(R.string.sameAsCardId));
            }
        }

        // Empty intent values
        barcodeType = null;
        cardId = null;
        barcodeId = null;

        // Initialization has finished
        if (!initDone) {
            initDone = true;
            hasChanged = hadChanges;
        }

        generateOrHideBarcode();

        enterButton.setOnClickListener(new EditCardIdAndBarcode());
        barcodeImage.setOnClickListener(new EditCardIdAndBarcode());

        cardImageFrontHolder.setOnClickListener(new ChooseCardImage());
        cardImageBackHolder.setOnClickListener(new ChooseCardImage());

        FloatingActionButton saveButton = findViewById(R.id.fabSave);
        saveButton.setOnClickListener(v -> doSave());
        saveButton.bringToFront();

        generateIcon(storeFieldEdit.getText().toString());

        onResuming = false;
    }

    protected void setColorFromIcon() {
        Object icon = thumbnail.getTag();
        if (icon != null && (icon instanceof Bitmap)) {
            updateTempState(LoyaltyCardField.headerColor, new Palette.Builder((Bitmap) icon).generate().getDominantColor(tempLoyaltyCard.headerColor != null ? tempLoyaltyCard.headerColor : getResources().getColor(R.color.colorPrimary)));
        } else {
            Log.d("setColorFromIcon", "attempting header color change from icon but icon does not exist");
        }
    }

    protected void setCardImage(ImageView imageView, Bitmap bitmap, boolean applyFallback) {
        imageView.setTag(bitmap);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else if (applyFallback) {
            imageView.setImageResource(R.drawable.ic_camera_white);
        }
    }


    protected static void formatExpiryField(Context context, EditText expiryField, Date expiry) {
        expiryField.setTag(expiry);

        if (expiry == null) {
            expiryField.setText(context.getString(R.string.never));
        } else {

            expiryField.setText(DateFormat.getDateInstance(DateFormat.LONG).format(expiry));
            expiryInMillis = expiry.getTime();

        }
    }

    private void formatBalanceCurrencyField(Currency balanceType) {
        if (balanceType == null) {
            balanceCurrencyField.setText(getString(R.string.points));
        } else {
            balanceCurrencyField.setText(balanceType.getSymbol());
        }
    }

    @Override
    public void onBackPressed() {
        askBeforeQuitIfChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (requestCode == PERMISSION_REQUEST_CAMERA_IMAGE_FRONT) {
                    takePhotoForCard(Utils.CARD_IMAGE_FROM_CAMERA_FRONT);
                } else if (requestCode == PERMISSION_REQUEST_CAMERA_IMAGE_BACK) {
                    takePhotoForCard(Utils.CARD_IMAGE_FROM_CAMERA_BACK);
                } else if (requestCode == PERMISSION_REQUEST_CAMERA_IMAGE_ICON) {
                    takePhotoForCard(Utils.CARD_IMAGE_FROM_CAMERA_ICON);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        new AlertDialog.Builder(this)
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
        if (!hasChanged) {
            if (tempStoredOldBarcodeValue != null) {
                askBarcodeChange(this::askBeforeQuitIfChanged);
                return;
            }

            finish();
            return;
        }

        if (confirmExitDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        mRequestedImage = type;

        mPhotoTakerLauncher.launch(photoURI);
    }

    class EditCardIdAndBarcode implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            final Bundle b = new Bundle();
            b.putString(LoyaltyCardEditActivity.BUNDLE_CARDID, cardIdFieldView.getText().toString());
            i.putExtras(b);
            mCardIdAndBarCodeEditorLauncher.launch(i);
        }
    }

    class ChooseCardImage implements View.OnClickListener {
        @Override
        public void onClick(View v) throws NoSuchElementException {
            ImageView targetView;

            if (v.getId() == R.id.frontImageHolder) {
                targetView = cardImageFront;
            } else if (v.getId() == R.id.backImageHolder) {
                targetView = cardImageBack;
            } else if (v.getId() == R.id.thumbnail) {
                targetView = thumbnail;
            } else {
                throw new IllegalArgumentException("Invalid IMAGE ID " + v.getId());
            }

            LinkedHashMap<String, Callable<Void>> cardOptions = new LinkedHashMap<>();
            if (targetView.getTag() != null && v.getId() != R.id.thumbnail) {
                cardOptions.put(getString(R.string.removeImage), () -> {
                    if (targetView == cardImageFront) {
                        mFrontImageRemoved = true;
                        mFrontImageUnsaved = false;
                    } else {
                        mBackImageRemoved = true;
                        mBackImageUnsaved = false;
                    }

                    setCardImage(targetView, null, true);
                    return null;
                });
            }

            if (v.getId() == R.id.thumbnail) {
                cardOptions.put(getString(R.string.selectColor), () -> {
                    ColorPickerDialog.Builder dialogBuilder = ColorPickerDialog.newBuilder();

                    if (tempLoyaltyCard.headerColor != null) {
                        dialogBuilder.setColor(tempLoyaltyCard.headerColor);
                    }

                    ColorPickerDialog dialog = dialogBuilder.create();
                    dialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
                        @Override
                        public void onColorSelected(int dialogId, int color) {
                            updateTempState(LoyaltyCardField.headerColor, color);

                            // Unset image if set
                            thumbnail.setTag(null);

                            generateIcon(storeFieldEdit.getText().toString());
                        }

                        @Override
                        public void onDialogDismissed(int dialogId) {
                            // Nothing to do, no change made
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "color-picker-dialog");

                    setCardImage(targetView, null, false);
                    mIconRemoved = true;
                    mIconUnsaved = false;

                    return null;
                });
            }

            cardOptions.put(getString(R.string.takePhoto), () -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permissionRequestType;

                    if (v.getId() == R.id.frontImageHolder) {
                        permissionRequestType = PERMISSION_REQUEST_CAMERA_IMAGE_FRONT;
                    } else if (v.getId() == R.id.backImageHolder) {
                        permissionRequestType = PERMISSION_REQUEST_CAMERA_IMAGE_BACK;
                    } else if (v.getId() == R.id.thumbnail) {
                        permissionRequestType = PERMISSION_REQUEST_CAMERA_IMAGE_ICON;
                    } else {
                        throw new IllegalArgumentException("Unknown ID type " + v.getId());
                    }

                    requestPermissions(new String[]{Manifest.permission.CAMERA}, permissionRequestType);
                } else {
                    int cardImageType;

                    if (v.getId() == R.id.frontImageHolder) {
                        cardImageType = Utils.CARD_IMAGE_FROM_CAMERA_FRONT;
                    } else if (v.getId() == R.id.backImageHolder) {
                        cardImageType = Utils.CARD_IMAGE_FROM_CAMERA_BACK;
                    } else if (v.getId() == R.id.thumbnail) {
                        cardImageType = Utils.CARD_IMAGE_FROM_CAMERA_ICON;
                    } else {
                        throw new IllegalArgumentException("Unknown ID type " + v.getId());
                    }

                    takePhotoForCard(cardImageType);
                }
                return null;
            });

            cardOptions.put(getString(R.string.addFromImage), () -> {
                if (v.getId() == R.id.frontImageHolder) {
                    mRequestedImage = Utils.CARD_IMAGE_FROM_FILE_FRONT;
                } else if (v.getId() == R.id.backImageHolder) {
                    mRequestedImage = Utils.CARD_IMAGE_FROM_FILE_BACK;
                } else if (v.getId() == R.id.thumbnail) {
                    mRequestedImage = Utils.CARD_IMAGE_FROM_FILE_ICON;
                } else {
                    throw new IllegalArgumentException("Unknown ID type " + v.getId());
                }

                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                mPhotoPickerLauncher.launch(i);
                return null;
            });

            int titleResource;

            if (v.getId() == R.id.frontImageHolder) {
                titleResource = R.string.setFrontImage;
            } else if (v.getId() == R.id.backImageHolder) {
                titleResource = R.string.setBackImage;
            } else if (v.getId() == R.id.thumbnail) {
                titleResource = R.string.setIcon;
            } else {
                throw new IllegalArgumentException("Unknown ID type " + v.getId());
            }

            new AlertDialog.Builder(LoyaltyCardEditActivity.this)
                    .setTitle(getString(titleResource))
                    .setItems(cardOptions.keySet().toArray(new CharSequence[cardOptions.size()]), (dialog, which) -> {
                        Iterator<Callable<Void>> callables = cardOptions.values().iterator();
                        Callable<Void> callable = callables.next();

                        for (int i = 0; i < which; i++) {
                            callable = callables.next();
                        }

                        try {
                            callable.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .show();
        }
    }



    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        final Context context;
        final EditText expiryFieldEdit;

        DatePickerFragment(Context context, EditText expiryFieldEdit) {
            this.context = context;
            this.expiryFieldEdit = expiryFieldEdit;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();

            Date date = (Date) expiryFieldEdit.getTag();

            if (date != null) {
                c.setTime(date);
            }

            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);
            c.set(Calendar.HOUR, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);


            long unixTime = c.getTimeInMillis();

            Date date = new Date(unixTime);

            formatExpiryField(context, expiryFieldEdit, date);

            expiryInMillis = unixTime;
        }
    }


    public static class NumberPickerFragment extends DialogFragment {
        NumberPicker numberPicker;
        TextView dateDialogTextView;
        View showRemindersLayout;
        int id;
        Button addReminderBtn;


        public NumberPickerFragment(View showRemindersLayout, int loyaltyCardId, Button addReminderBtn) {
            this.showRemindersLayout = showRemindersLayout;
            id = loyaltyCardId;
            this.addReminderBtn = addReminderBtn;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.number_picker_dialog_layout, null);

            dateDialogTextView = view.findViewById(R.id.dateTextView);
            numberPicker = view.findViewById(R.id.numberPicker);

            Calendar calendar = Calendar.getInstance();
            long differenceInMillis = expiryInMillis - calendar.getTimeInMillis();

            int noOfDaysDifference = (int) (differenceInMillis / (1000 * 3600 * 24));

            Log.i(TAG, "onCreateDialog: noOfDaysDiff" + noOfDaysDifference);


            if (noOfDaysDifference > 365) {
                noOfDaysDifference = 365;
            }
            if(days > noOfDaysDifference) {

                days =1;

            }

            Log.i(TAG, "onCreateDialog: noOfDaysDifference " + noOfDaysDifference);
            Log.i(TAG, "onCreateDialog: days = " + days);

            Log.i(TAG, "onCreateDialog: Number picker dialog created");


            numberPicker.setMaxValue(noOfDaysDifference);
            numberPicker.setMinValue(1);
            numberPicker.setValue(days);

            reminderDateInMillis = expiryInMillis - (1000 * 3600 * 24) * (long) days;

            Date date = new Date(reminderDateInMillis);
            formattedDate = DateFormat.getDateInstance(DateFormat.LONG).format(date);
            dateDialogTextView.setText(formattedDate);

            numberPicker.setOnValueChangedListener((numberPicker, i, i1) -> {

                days = i1;
                long cal = (1000 * 3600 * 24) * (long) days;
                reminderDateInMillis = expiryInMillis - cal;

                Date date1 = new Date(reminderDateInMillis);
                formattedDate = DateFormat.getDateInstance(DateFormat.LONG).format(date1);
                dateDialogTextView.setText(formattedDate);
            });

            delay.setTimeInMillis(reminderDateInMillis);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(view)
                    .setTitle(R.string.add_reminder)
                    .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            Log.i(TAG, "onClick: Set clicked");
                            DialogFragment dialogFragment = new TimePickerFragment(getContext(), showRemindersLayout, addReminderBtn);
                            dialogFragment.show(getActivity().getSupportFragmentManager(), "TimePicker");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.i(TAG, "onClick: Cancel clicked");
                        }
                    });
            return builder.create();
        }
    }

    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        Calendar calendar;
        Context context;
        View showRemindersLayout;
        TextView daysTextView, dateReminderView, timeReminderTextView;
        Button addReminderBtn;


        public TimePickerFragment(Context context, View showRemindersLayout, Button addReminderBtn) {

            this.context = context;
            this.showRemindersLayout = showRemindersLayout;
            this.addReminderBtn = addReminderBtn;

            daysTextView = showRemindersLayout.findViewById(R.id.daysTextView);
            dateReminderView = showRemindersLayout.findViewById(R.id.dateReminderView);
            timeReminderTextView = showRemindersLayout.findViewById(R.id.timeReminderTextView);

        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

            calendar = Calendar.getInstance();

            Date date = (Date) timeReminderTextView.getTag();

            if (date != null) {
                calendar.setTime(date);
            }


            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);

            return new TimePickerDialog(getActivity(), this, hour, minute, false);
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int mhour, int mminute) {

            calendar.set(Calendar.HOUR_OF_DAY, mhour);
            calendar.set(Calendar.MINUTE, mminute);

            hour = mhour;
            minute = mminute;

            Date date = new Date(calendar.getTimeInMillis());
            timeReminderTextView.setTag(date);

            delay.set(Calendar.HOUR_OF_DAY, mhour);
            delay.set(Calendar.MINUTE, mminute);

            Log.i(TAG, "onTimeSet: timeInMillis " + delay.getTimeInMillis());

            showReminders(addReminderBtn);
            hasReminderChanged = true;

        }

        private void showReminders(Button addReminderBtn) {


            Log.i(TAG, "showReminders: days =" + days);

            daysTextView.setText(String.valueOf(days));
            dateReminderView.setText(formattedDate);
            timeReminderTextView.setText(formatTime(hour, minute));

            showRemindersLayout.setVisibility(View.VISIBLE);
            addReminderBtn.setText(R.string.edit_reminder);

        }

    }


    private static String formatTime(int mhour, int mminute) {
        String time24;
        String sTime="";

        time24 = mhour+":"+mminute;

        try {

            SimpleDateFormat time24Format = new SimpleDateFormat("HH:mm");
            SimpleDateFormat time12Format = new SimpleDateFormat("hh:mm a");
            Date date24 = time24Format.parse(time24);

            assert date24 != null;
            sTime = time12Format.format(date24);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return sTime;
    }

    private void doSave() {
        if (tempStoredOldBarcodeValue != null) {
            askBarcodeChange(this::doSave);
            return;
        }

        if (tempLoyaltyCard.store.isEmpty()) {
            Snackbar.make(storeFieldEdit, R.string.noStoreError, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (tempLoyaltyCard.cardId.isEmpty()) {
            Snackbar.make(cardIdFieldView, R.string.noCardIdError, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (!validBalance) {
            Snackbar.make(balanceField, getString(R.string.parsingBalanceFailed, balanceField.getText().toString()), Snackbar.LENGTH_LONG).show();
            return;
        }

        List<Group> selectedGroups = new ArrayList<>();

        for (Integer chipId : groupsChips.getCheckedChipIds()) {
            Chip chip = groupsChips.findViewById(chipId);
            selectedGroups.add((Group) chip.getTag());
        }

        if (updateLoyaltyCard) {   //update of "starStatus" not necessary, since it cannot be changed in this activity (only in ViewActivity)
            db.updateLoyaltyCard(loyaltyCardId, tempLoyaltyCard.store, tempLoyaltyCard.note, tempLoyaltyCard.expiry, tempLoyaltyCard.balance, tempLoyaltyCard.balanceType, tempLoyaltyCard.cardId, tempLoyaltyCard.barcodeId, tempLoyaltyCard.barcodeType, tempLoyaltyCard.headerColor);
            try {
                Utils.saveCardImage(this, (Bitmap) cardImageFront.getTag(), loyaltyCardId, ImageLocationType.front);
                Utils.saveCardImage(this, (Bitmap) cardImageBack.getTag(), loyaltyCardId, ImageLocationType.back);
                Utils.saveCardImage(this, (Bitmap) thumbnail.getTag(), loyaltyCardId, ImageLocationType.icon);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Updated " + loyaltyCardId + " to " + cardId);
        } else {
            loyaltyCardId = (int) db.insertLoyaltyCard(tempLoyaltyCard.store, tempLoyaltyCard.note, tempLoyaltyCard.expiry, tempLoyaltyCard.balance, tempLoyaltyCard.balanceType, tempLoyaltyCard.cardId, tempLoyaltyCard.barcodeId, tempLoyaltyCard.barcodeType, tempLoyaltyCard.headerColor, 0, tempLoyaltyCard.lastUsed);
            try {
                Utils.saveCardImage(this, (Bitmap) cardImageFront.getTag(), loyaltyCardId, ImageLocationType.front);
                Utils.saveCardImage(this, (Bitmap) cardImageBack.getTag(), loyaltyCardId, ImageLocationType.back);
                Utils.saveCardImage(this, (Bitmap) thumbnail.getTag(), loyaltyCardId, ImageLocationType.icon);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        db.setLoyaltyCardGroups(loyaltyCardId, selectedGroups);

        SharedPreferences sharedPreferences = getSharedPreferences("protect.card_locker.dateTime", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (showRemindersLayout.getVisibility() == View.VISIBLE && hasReminderChanged) {

            Log.i(TAG, "doSave: noOfDays " + days);
            Log.i(TAG, "doSave: hour " + hour);
            Log.i(TAG, "doSave: minute " + minute);
            Log.i(TAG, "doSave: formattedDate " + formattedDate);

            editor.putInt(loyaltyCardId + "noOfDays", days);
            editor.putString(loyaltyCardId + "formattedDate", formattedDate);
            editor.putInt(loyaltyCardId + "Hour", hour);
            editor.putInt(loyaltyCardId + "Minute", minute);
            scheduleNotification();

        }
        if (showRemindersLayout.getVisibility() != View.VISIBLE) {

            editor.putInt(loyaltyCardId + "noOfDays", 0);
            Log.i(TAG, "doSave: Alarm not saved");

            Utils utils = new Utils();
            utils.deleteAlarm(loyaltyCardId,getApplicationContext());
        }

        editor.apply();
        finish();
        hasReminderChanged = false;
    }

    private void scheduleNotification() {

        Intent intent = new Intent(this, Notification.class);
        intent.putExtra("cardName", storeFieldEdit.getText().toString());
        intent.putExtra("loyaltyCardId", loyaltyCardId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, loyaltyCardId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, delay.getTimeInMillis(), pendingIntent);

        Log.i(TAG, "scheduleNotification: getDelayTimeInMillis " + delay.getTimeInMillis());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (updateLoyaltyCard) {
            getMenuInflater().inflate(R.menu.card_update_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.card_add_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                askBeforeQuitIfChanged();
                break;

            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.deleteTitle);
                builder.setMessage(R.string.deleteConfirmation);
                builder.setPositiveButton(R.string.confirm, (dialog, which) -> {

                    Log.e(TAG, "Deleting card: " + loyaltyCardId);
                    DBHelper db = new DBHelper(LoyaltyCardEditActivity.this);
                    db.deleteLoyaltyCard(loyaltyCardId);

                    Utils utils = new Utils();
                    utils.deleteAlarm(loyaltyCardId,getApplicationContext());
                    ShortcutHelper.removeShortcut(LoyaltyCardEditActivity.this, loyaltyCardId);
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



    public void startCropper (String sourceImagePath){
        startCropperUri(Uri.parse("file://" + sourceImagePath));
    }


    public void startCropperUri (Uri sourceUri){
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
                    e.printStackTrace();Log.d("cropper", "exif reading failed before setting initial width and height for ucrop");
                    setCropperOptions(false, image.getWidth(), image.getHeight());
                }
            }
        }
        mCropperLauncher.launch(
                UCrop.of(
                        sourceUri,
                        destUri
                ).withOptions(mCropperOptions)
                        .getIntent(this)
        );
    }


    private void showBarcode () {
        barcodeImageLayout.setVisibility(View.VISIBLE);
    }
    private void hideBarcode () {
        barcodeImageLayout.setVisibility(View.GONE);
    }

    private void generateOrHideBarcode () {
        String cardIdString = tempLoyaltyCard.barcodeId != null ? tempLoyaltyCard.barcodeId : tempLoyaltyCard.cardId;
        CatimaBarcode barcodeFormat = tempLoyaltyCard.barcodeType;

        if (barcodeFormat == null || cardIdString.isEmpty() || !barcodeFormat.isSupported()) {
            hideBarcode();
        } else {
            generateBarcode(cardIdString, barcodeFormat);
        }
    }

    private void generateBarcode (String cardIdString, CatimaBarcode barcodeFormat){
        mTasks.flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false);

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
                            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getApplicationContext(), barcodeImage, cardIdString, barcodeFormat, null, false, warnOnInvalidBarcodeType);
                            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
                        }
                    });
        } else {
            Log.d(TAG, "ImageView size known known, creating barcode");
            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getApplicationContext(), barcodeImage, cardIdString, barcodeFormat, null, false, warnOnInvalidBarcodeType);
            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
        }

        showBarcode();
    }

    private void generateIcon (String store){
        if (tempLoyaltyCard.headerColor == null) {
            return;
        }

        if (thumbnail.getTag() == null) {
            thumbnail.setBackgroundColor(tempLoyaltyCard.headerColor);

            LetterBitmap letterBitmap = Utils.generateIcon(this, store, tempLoyaltyCard.headerColor);

            if (letterBitmap != null) {
                thumbnail.setImageBitmap(letterBitmap.getLetterTile());
            } else {
                thumbnail.setImageBitmap(null);
            }
        }

        thumbnail.setMinimumWidth(thumbnail.getHeight());
    }

    private void showPart (String part){
        if (tempStoredOldBarcodeValue != null) {
            askBarcodeChange(() -> showPart(part));
            return;
        }

        View cardPart = findViewById(R.id.cardPart);
        View barcodePart = findViewById(R.id.barcodePart);
        View picturesPart = findViewById(R.id.picturesPart);

        if (getString(R.string.card).equals(part)) {
            cardPart.setVisibility(View.VISIBLE);
            barcodePart.setVisibility(View.GONE);
            picturesPart.setVisibility(View.GONE);

            // Explicitly hide barcode (fixes blurriness on redraw)
            hideBarcode();
        } else if (getString(R.string.barcode).equals(part)) {
            cardPart.setVisibility(View.GONE);
            barcodePart.setVisibility(View.VISIBLE);
            picturesPart.setVisibility(View.GONE);

            // Redraw barcode due to size change (Visibility.GONE sets it to 0)
            generateOrHideBarcode();
        } else if (getString(R.string.photos).equals(part)) {
            cardPart.setVisibility(View.GONE);
            barcodePart.setVisibility(View.GONE);
            picturesPart.setVisibility(View.VISIBLE);

            // Explicitly hide barcode (fixes blurriness on redraw)
            hideBarcode();
        } else {
            throw new UnsupportedOperationException();
        }
    }



}


