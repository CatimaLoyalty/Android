package protect.card_locker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.LocaleList;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.text.DateFormat;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.databinding.LoyaltyCardEditActivityBinding;

public class LoyaltyCardEditActivity extends CatimaAppCompatActivity {
    private LoyaltyCardEditActivityBinding binding;
    private static final String TAG = "Catima";

    private final String STATE_TAB_INDEX = "savedTab";
    private final String STATE_TEMP_CARD = "tempLoyaltyCard";

    private static final int ID_IMAGE_FRONT = 0;
    private static final int ID_IMAGE_BACK = 1;

    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_FRONT = 100;
    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_BACK = 101;

    public static final String BUNDLE_ID = "id";
    public static final String BUNDLE_UPDATE = "update";
    public static final String BUNDLE_CARDID = "cardId";
    public static final String BUNDLE_BARCODEID = "barcodeId";
    public static final String BUNDLE_BARCODETYPE = "barcodeType";
    public static final String BUNDLE_ADDGROUP = "addGroup";

    TabLayout tabs;

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

    Button enterButton;

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

    String tempCameraPicturePath;

    LoyaltyCard tempLoyaltyCard;

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
                Utils.getUnixTime(),100
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
        tabs = binding.tabs;
        savedInstanceState.putInt(STATE_TAB_INDEX, tabs.getSelectedTabPosition());
        savedInstanceState.putParcelable(STATE_TEMP_CARD, tempLoyaltyCard);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        tempLoyaltyCard = savedInstanceState.getParcelable(STATE_TEMP_CARD);
        super.onRestoreInstanceState(savedInstanceState);
        tabs = binding.tabs;
        tabs.selectTab(tabs.getTabAt(savedInstanceState.getInt(STATE_TAB_INDEX)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LoyaltyCardEditActivityBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
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

        tabs = binding.tabs;
        thumbnail = binding.thumbnail;
        storeFieldEdit = binding.storeNameEdit;
        noteFieldEdit = binding.noteEdit;
        groupsChips = binding.groupChips;
        expiryField = binding.expiryField;
        balanceField = binding.balanceField;
        balanceCurrencyField = binding.balanceCurrencyField;
        cardIdFieldView = binding.cardIdView;
        barcodeIdField = binding.barcodeIdField;
        barcodeTypeField = binding.barcodeTypeField;
        barcodeImage = binding.barcode;
        barcodeImageLayout = binding.barcodeLayout;
        barcodeCaptureLayout = binding.barcodeCaptureLayout;
        cardImageFrontHolder = binding.frontImageHolder;
        cardImageBackHolder = binding.backImageHolder;
        cardImageFrontHolder.setId(ID_IMAGE_FRONT);
        cardImageBackHolder.setId(ID_IMAGE_BACK);
        cardImageFront = binding.frontImage;
        cardImageBack = binding.backImage;

        enterButton = binding.enterButton;
        cardImageFront.setBackgroundColor(getThemeColor());
        cardImageBack.setBackgroundColor(getThemeColor());

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

        expiryField.addTextChangedListener(new SimpleTextWatcher() {
            CharSequence lastValue;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
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
                    ;

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
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

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
                setTitle(R.string.editCardTitle);
                setCardImage(cardImageFront, Utils.retrieveCardImage(this, tempLoyaltyCard.id, true));
                setCardImage(cardImageBack, Utils.retrieveCardImage(this, tempLoyaltyCard.id, false));
            } else if (importLoyaltyCardUri != null) {
                try {
                    tempLoyaltyCard = importUriHelper.parse(importLoyaltyCardUri);
                } catch (InvalidObjectException ex) {
                    Toast.makeText(this, R.string.failedParsingImportUriError, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                setTitle(R.string.addCardTitle);
            } else {
                // New card, use default values
                tempLoyaltyCard = new LoyaltyCard(-1, "", "", null, new BigDecimal("0"), null, "", null, null, null, 0, Utils.getUnixTime(),100);
                setTitle(R.string.addCardTitle);
            }
        }

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
            thumbnail.setOnClickListener(new ColorSelectListener());
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
            hasChanged = false;
            initDone = true;
        }

        generateOrHideBarcode();

        enterButton.setOnClickListener(new EditCardIdAndBarcode());
        barcodeImage.setOnClickListener(new EditCardIdAndBarcode());

        cardImageFrontHolder.setOnClickListener(new ChooseCardImage());
        cardImageBackHolder.setOnClickListener(new ChooseCardImage());

        FloatingActionButton saveButton = binding.fabSave;
        saveButton.setOnClickListener(v -> doSave());
        saveButton.bringToFront();

        generateIcon(storeFieldEdit.getText().toString());

        onResuming = false;
    }

    protected static void setCardImage(ImageView imageView, Bitmap bitmap) {
        imageView.setTag(bitmap);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_camera_white);
        }
    }

    protected static void formatExpiryField(Context context, EditText expiryField, Date expiry) {
        expiryField.setTag(expiry);

        if (expiry == null) {
            expiryField.setText(context.getString(R.string.never));
        } else {
            expiryField.setText(DateFormat.getDateInstance(DateFormat.LONG).format(expiry));
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
                } else {
                    takePhotoForCard(Utils.CARD_IMAGE_FROM_CAMERA_BACK);
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
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                })
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
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            confirmExitDialog = builder.create();
        }
        confirmExitDialog.show();
    }

    private void takePhotoForCard(int type) throws IOException {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        String imageFileName = "CATIMA_" + new Date().getTime();
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        );

        tempCameraPicturePath = image.getAbsolutePath();

        Uri photoURI = FileProvider.getUriForFile(LoyaltyCardEditActivity.this, BuildConfig.APPLICATION_ID, image);
        i.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        startActivityForResult(i, type);
    }

    class EditCardIdAndBarcode implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            final Bundle b = new Bundle();
            b.putString(LoyaltyCardEditActivity.BUNDLE_CARDID, cardIdFieldView.getText().toString());
            i.putExtras(b);
            startActivityForResult(i, Utils.BARCODE_SCAN);
        }
    }

    class ChooseCardImage implements View.OnClickListener {
        @Override
        public void onClick(View v) throws NoSuchElementException {
            ImageView targetView = v.getId() == ID_IMAGE_FRONT ? cardImageFront : cardImageBack;

            LinkedHashMap<String, Callable<Void>> cardOptions = new LinkedHashMap<>();
            if (targetView.getTag() != null) {
                cardOptions.put(getString(R.string.removeImage), () -> {
                    setCardImage(targetView, null);
                    return null;
                });
            }

            cardOptions.put(getString(R.string.takePhoto), () -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, v.getId() == ID_IMAGE_FRONT ? PERMISSION_REQUEST_CAMERA_IMAGE_FRONT : PERMISSION_REQUEST_CAMERA_IMAGE_BACK);
                } else {
                    takePhotoForCard(v.getId() == ID_IMAGE_FRONT ? Utils.CARD_IMAGE_FROM_CAMERA_FRONT : Utils.CARD_IMAGE_FROM_CAMERA_BACK);
                }
                return null;
            });

            cardOptions.put(getString(R.string.addFromImage), () -> {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, v.getId() == ID_IMAGE_FRONT ? Utils.CARD_IMAGE_FROM_FILE_FRONT : Utils.CARD_IMAGE_FROM_FILE_BACK);
                return null;
            });

            new AlertDialog.Builder(LoyaltyCardEditActivity.this)
                    .setTitle(v.getId() == ID_IMAGE_FRONT ? getString(R.string.setFrontImage) : getString(R.string.setBackImage))
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

    class ColorSelectListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ColorPickerDialog.Builder dialogBuilder = ColorPickerDialog.newBuilder();

            if (tempLoyaltyCard.headerColor != null) {
                dialogBuilder.setColor(tempLoyaltyCard.headerColor);
            }

            ColorPickerDialog dialog = dialogBuilder.create();
            dialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
                @Override
                public void onColorSelected(int dialogId, int color) {
                    updateTempState(LoyaltyCardField.headerColor, color);

                    generateIcon(storeFieldEdit.getText().toString());
                }

                @Override
                public void onDialogDismissed(int dialogId) {
                    // Nothing to do, no change made
                }
            });
            dialog.show(getSupportFragmentManager(), "color-picker-dialog");
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
        }
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
                Utils.saveCardImage(this, (Bitmap) cardImageFront.getTag(), loyaltyCardId, true);
                Utils.saveCardImage(this, (Bitmap) cardImageBack.getTag(), loyaltyCardId, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Updated " + loyaltyCardId + " to " + cardId);
        } else {
            loyaltyCardId = (int) db.insertLoyaltyCard(tempLoyaltyCard.store, tempLoyaltyCard.note, tempLoyaltyCard.expiry, tempLoyaltyCard.balance, tempLoyaltyCard.balanceType, tempLoyaltyCard.cardId, tempLoyaltyCard.barcodeId, tempLoyaltyCard.barcodeType, tempLoyaltyCard.headerColor, 0, tempLoyaltyCard.lastUsed);
            try {
                Utils.saveCardImage(this, (Bitmap) cardImageFront.getTag(), loyaltyCardId, true);
                Utils.saveCardImage(this, (Bitmap) cardImageBack.getTag(), loyaltyCardId, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        db.setLoyaltyCardGroups(loyaltyCardId, selectedGroups);

        finish();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            if (requestCode == Utils.CARD_IMAGE_FROM_CAMERA_FRONT || requestCode == Utils.CARD_IMAGE_FROM_CAMERA_BACK) {
                Bitmap bitmap = BitmapFactory.decodeFile(tempCameraPicturePath);

                if (bitmap != null) {
                    bitmap = Utils.resizeBitmap(bitmap);
                    try {
                        bitmap = Utils.rotateBitmap(bitmap, new ExifInterface(tempCameraPicturePath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (requestCode == Utils.CARD_IMAGE_FROM_CAMERA_FRONT) {
                        setCardImage(cardImageFront, bitmap);
                    } else {
                        setCardImage(cardImageBack, bitmap);
                    }

                    hasChanged = true;
                } else {
                    Toast.makeText(this, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == Utils.CARD_IMAGE_FROM_FILE_FRONT || requestCode == Utils.CARD_IMAGE_FROM_FILE_BACK) {
                Bitmap bitmap = null;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ImageDecoder.Source image_source = ImageDecoder.createSource(getContentResolver(), intent.getData());
                        bitmap = ImageDecoder.decodeBitmap(image_source, (decoder, info, source) -> decoder.setMutableRequired(true));
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), intent.getData());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error getting data from image file");
                    e.printStackTrace();
                }

                if (bitmap != null) {
                    bitmap = Utils.resizeBitmap(bitmap);
                    if (requestCode == Utils.CARD_IMAGE_FROM_FILE_FRONT) {
                        setCardImage(cardImageFront, bitmap);
                    } else {
                        setCardImage(cardImageBack, bitmap);
                    }

                    hasChanged = true;
                } else {
                    Toast.makeText(this, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
                }
            } else {
                BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this);

                cardId = barcodeValues.content();
                barcodeType = barcodeValues.format();
                barcodeId = "";
            }
        }

        onResume();
    }

    private void showBarcode() {
        barcodeImageLayout.setVisibility(View.VISIBLE);
    }

    private void hideBarcode() {
        barcodeImageLayout.setVisibility(View.GONE);
    }

    private void generateOrHideBarcode() {
        String cardIdString = tempLoyaltyCard.barcodeId != null ? tempLoyaltyCard.barcodeId : tempLoyaltyCard.cardId;
        CatimaBarcode barcodeFormat = tempLoyaltyCard.barcodeType;

        if (barcodeFormat == null || cardIdString.isEmpty() || !barcodeFormat.isSupported()) {
            hideBarcode();
        } else {
            generateBarcode(cardIdString, barcodeFormat);
        }
    }

    private void generateBarcode(String cardIdString, CatimaBarcode barcodeFormat) {
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

    private void generateIcon(String store) {
        if (tempLoyaltyCard.headerColor == null) {
            return;
        }

        thumbnail.setBackgroundColor(tempLoyaltyCard.headerColor);

        LetterBitmap letterBitmap = Utils.generateIcon(this, store, tempLoyaltyCard.headerColor);

        if (letterBitmap != null) {
            thumbnail.setImageBitmap(letterBitmap.getLetterTile());
        } else {
            thumbnail.setImageBitmap(null);
        }

        thumbnail.setMinimumWidth(thumbnail.getHeight());
    }

    private void showPart(String part) {
        if (tempStoredOldBarcodeValue != null) {
            askBarcodeChange(() -> showPart(part));
            return;
        }

        View cardPart = binding.cardPart;
        View barcodePart = binding.barcodePart;
        View picturesPart = binding.picturesPart;

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
