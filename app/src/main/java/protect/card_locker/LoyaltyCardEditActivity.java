package protect.card_locker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.LocaleList;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import com.google.zxing.BarcodeFormat;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

public class LoyaltyCardEditActivity extends AppCompatActivity
{
    private static final String TAG = "Catima";

    private final String STATE_TAB_INDEX = "savedTab";

    private static final int ID_IMAGE_FRONT = 0;
    private static final int ID_IMAGE_BACK = 1;

    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_FRONT = 100;
    private static final int PERMISSION_REQUEST_CAMERA_IMAGE_BACK = 101;

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

    Uri importLoyaltyCardUri = null;
    Integer headingColorValue = null;

    DBHelper db;
    ImportURIHelper importUriHelper;

    boolean hasChanged = false;
    String tempStoredOldBarcodeValue = null;
    boolean initDone = false;
    AlertDialog confirmExitDialog = null;

    boolean validBalance = true;
    Runnable warnOnInvalidBarcodeType;

    HashMap<String, Currency> currencies = new HashMap<>();

    String tempCameraPicturePath;

    private void extractIntentFields(Intent intent)
    {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt("id") : 0;
        updateLoyaltyCard = b != null && b.getBoolean("update", false);

        cardId = b != null ? b.getString("cardId") : null;
        barcodeId = b != null ? b.getString("barcodeId") : null;
        barcodeType = b != null ? b.getString("barcodeType") : null;

        importLoyaltyCardUri = intent.getData();

        Log.d(TAG, "View activity: id=" + loyaltyCardId
                + ", updateLoyaltyCard=" + updateLoyaltyCard);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        tabs = findViewById(R.id.tabs);
        savedInstanceState.putInt(STATE_TAB_INDEX, tabs.getSelectedTabPosition());
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tabs = findViewById(R.id.tabs);
        tabs.selectTab(tabs.getTabAt(savedInstanceState.getInt(STATE_TAB_INDEX)));
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
        cardImageFrontHolder.setId(ID_IMAGE_FRONT);
        cardImageBackHolder.setId(ID_IMAGE_BACK);
        cardImageFront = findViewById(R.id.frontImage);
        cardImageBack = findViewById(R.id.backImage);

        enterButton = findViewById(R.id.enterButton);

        warnOnInvalidBarcodeType = () -> {
            if (!(boolean) barcodeImage.getTag()) {
                Toast.makeText(LoyaltyCardEditActivity.this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show();
            }
        };

        storeFieldEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                generateIcon(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        expiryField.addTextChangedListener(new TextWatcher() {
            CharSequence lastValue;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastValue = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                if (s.toString().equals(getString(R.string.never))) {
                    expiryField.setTag(null);
                } else if (s.toString().equals(getString(R.string.chooseExpiryDate))) {
                    if (!lastValue.toString().equals(getString(R.string.chooseExpiryDate))) {
                        expiryField.setText(lastValue);
                    };
                    DialogFragment datePickerFragment = new DatePickerFragment(expiryField);
                    datePickerFragment.show(getSupportFragmentManager(), "datePicker");
                }
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
                balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol((BigDecimal) balanceField.getTag(), (Currency) balanceCurrencyField.getTag()));
            }
        });

        balanceField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                try {
                    BigDecimal balance = Utils.parseCurrency(s.toString(), Utils.currencyHasDecimals((Currency) balanceCurrencyField.getTag()));
                    validBalance = true;

                    balanceField.setTag(balance);
                } catch (NumberFormatException e) {
                    validBalance = false;
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        balanceCurrencyField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                Currency currency;

                if (s.toString().equals(getString(R.string.points))) {
                    currency = null;
                } else {
                    currency = currencies.get(s.toString());
                }

                balanceCurrencyField.setTag(currency);

                BigDecimal balance = (BigDecimal) balanceField.getTag();

                if (balance != null) {
                    balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(balance, currency));
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

        cardIdFieldView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (initDone) {
                    if (tempStoredOldBarcodeValue == null) {
                        // We changed the card ID, save the current barcode ID in a temp
                        // variable and make sure to ask the user later if they also want to
                        // update the barcode ID
                        if (barcodeIdField.getTag() == null) {
                            // If it is set to "same as Card ID", temp-save the value before the
                            // Card ID change
                            tempStoredOldBarcodeValue = s.toString();
                        } else {
                            // Otherwise, set the temp value to the current field value
                            tempStoredOldBarcodeValue = barcodeIdField.getText().toString();
                        }

                        barcodeIdField.setText(tempStoredOldBarcodeValue);
                        barcodeIdField.setTag(tempStoredOldBarcodeValue);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        barcodeIdField.addTextChangedListener(new TextWatcher() {
            CharSequence lastValue;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastValue = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                if (s.toString().equals(getString(R.string.sameAsCardId))) {
                    // If the user manually changes the barcode again make sure we disable the
                    // request to update it to match the card id (if changed)
                    tempStoredOldBarcodeValue = null;

                    barcodeIdField.setTag(null);
                } else if (s.toString().equals(getString(R.string.setBarcodeId))) {
                    if (!lastValue.toString().equals(getString(R.string.setBarcodeId))) {
                        barcodeIdField.setText(lastValue);
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(LoyaltyCardEditActivity.this);
                    builder.setTitle(R.string.setBarcodeId);
                    final EditText input = new EditText(LoyaltyCardEditActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    if (barcodeIdField.getTag() != null) {
                        input.setText((String) barcodeIdField.getTag());
                    }
                    builder.setView(input);

                    builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        // If the user manually changes the barcode again make sure we disable the
                        // request to update it to match the card id (if changed)
                        tempStoredOldBarcodeValue = null;

                        barcodeIdField.setTag(input.getText().toString());
                        barcodeIdField.setText(input.getText());
                    });
                    builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    input.requestFocus();
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

        barcodeTypeField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                if (!s.toString().isEmpty()) {
                    if (s.toString().equals(getString(R.string.noBarcode))) {
                        barcodeTypeField.setTag(null);
                    } else {
                        try {
                            BarcodeFormat barcodeFormat = BarcodeFormat.valueOf(s.toString());

                            barcodeTypeField.setTag(barcodeFormat);

                            if (!BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES.contains(barcodeFormat.name())) {
                                Toast.makeText(LoyaltyCardEditActivity.this, getString(R.string.unsupportedBarcodeType), Toast.LENGTH_LONG).show();
                            }
                        } catch (IllegalArgumentException e) {}
                    }

                    generateOrHideBarcode();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> barcodeList = new ArrayList<>(BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES);
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
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);

        // Reset these fields, so they are re-populated in onResume().
        storeFieldEdit.setText("");
        noteFieldEdit.setText("");
        expiryField.setTag(null);
        expiryField.setText("");
        balanceCurrencyField.setTag(null);
        balanceCurrencyField.setText("");
        balanceField.setTag(null);
        balanceField.setText("");
        cardIdFieldView.setText("");
        barcodeIdField.setTag(null);
        barcodeIdField.setText("");
        barcodeTypeField.setText("");
        cardImageFront.setTag(null);
        cardImageBack.setTag(null);
    }

    @SuppressLint("DefaultLocale")
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

            if(expiryField.getText().length() == 0)
            {
                expiryField.setTag(loyaltyCard.expiry);
                formatExpiryField(loyaltyCard.expiry);
            }

            if(balanceCurrencyField.getText().length() == 0)
            {
                balanceCurrencyField.setTag(loyaltyCard.balanceType);
                formatBalanceCurrencyField(loyaltyCard.balanceType);
            }

            if(balanceField.getText().length() == 0)
            {
                balanceField.setTag(loyaltyCard.balance);
                balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(loyaltyCard.balance, loyaltyCard.balanceType));
            }

            if(cardIdFieldView.getText().length() == 0)
            {
                cardIdFieldView.setText(loyaltyCard.cardId);
            }

            if(barcodeIdField.getText().length() == 0)
            {
                barcodeIdField.setTag(loyaltyCard.barcodeId);
                barcodeIdField.setText(loyaltyCard.barcodeId != null ? loyaltyCard.barcodeId : getString(R.string.sameAsCardId));
            }

            if(barcodeTypeField.getText().length() == 0)
            {
                barcodeTypeField.setText(loyaltyCard.barcodeType != null ? loyaltyCard.barcodeType.toString() : getString(R.string.noBarcode));
            }

            if(headingColorValue == null)
            {
                headingColorValue = loyaltyCard.headerColor;
                if(headingColorValue == null)
                {
                    headingColorValue = LetterBitmap.getDefaultColor(this, loyaltyCard.store);
                }
            }

            if(cardImageFront.getTag() == null)
            {
                setCardImage(cardImageFront, Utils.retrieveCardImage(this, loyaltyCard.id, true));
            }

            if(cardImageBack.getTag() == null)
            {
                setCardImage(cardImageBack, Utils.retrieveCardImage(this, loyaltyCard.id, false));
            }

            setTitle(R.string.editCardTitle);
        }
        else if(importLoyaltyCardUri != null)
        {
            // Try to parse
            LoyaltyCard importCard;
            try {
                importCard = importUriHelper.parse(importLoyaltyCardUri);
            } catch (InvalidObjectException ex) {
                Toast.makeText(this, R.string.failedParsingImportUriError, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            storeFieldEdit.setText(importCard.store);
            noteFieldEdit.setText(importCard.note);
            expiryField.setTag(importCard.expiry);
            formatExpiryField(importCard.expiry);
            balanceField.setTag(importCard.balance);
            balanceCurrencyField.setTag(importCard.balanceType);
            formatBalanceCurrencyField(importCard.balanceType);
            cardIdFieldView.setText(importCard.cardId);
            barcodeIdField.setTag(importCard.barcodeId);
            barcodeIdField.setText(importCard.barcodeId != null ? importCard.barcodeId : getString(R.string.sameAsCardId));
            barcodeTypeField.setText(importCard.barcodeType != null ? importCard.barcodeType.toString() : getString(R.string.noBarcode));
            headingColorValue = importCard.headerColor;
        }
        else
        {
            setTitle(R.string.addCardTitle);
            expiryField.setTag(null);
            expiryField.setText(getString(R.string.never));
            barcodeIdField.setTag(null);
            barcodeIdField.setText(getString(R.string.sameAsCardId));
            balanceField.setTag(new BigDecimal("0"));
            balanceCurrencyField.setTag(null);
            formatBalanceCurrencyField(null);
            hideBarcode();
            setCardImage(cardImageFront, null);
            setCardImage(cardImageBack, null);
        }

        if(groupsChips.getChildCount() == 0)
        {
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

                chip.setChecked(false);
                for (Group loyaltyCardGroup : loyaltyCardGroups) {
                    if (loyaltyCardGroup._id.equals(group._id)) {
                        chip.setChecked(true);
                        break;
                    }
                }
                chip.setOnTouchListener((v, event) -> {
                    hasChanged = true;

                    return false;
                });

                groupsChips.addView(chip);
            }
        }

        if(headingColorValue == null)
        {
            // Select a random color to start out with.
            TypedArray colors = getResources().obtainTypedArray(R.array.letter_tile_colors);
            final int color = (int)(Math.random() * colors.length());
            headingColorValue = colors.getColor(color, Color.BLACK);
            colors.recycle();
        }

        thumbnail.setOnClickListener(new ColorSelectListener(headingColorValue));

        // Update from intent
        if (barcodeType != null) {
            try {
                barcodeTypeField.setText(BarcodeFormat.valueOf(barcodeType).name());
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

        if (!initDone) {
            hasChanged = false;
            initDone = true;
        }

        generateOrHideBarcode();

        enterButton.setOnClickListener(new EditCardIdAndBarcode());
        barcodeImage.setOnClickListener(new EditCardIdAndBarcode());

        cardImageFrontHolder.setOnClickListener(new ChooseCardImage());
        cardImageBackHolder.setOnClickListener(new ChooseCardImage());

        FloatingActionButton saveButton = findViewById(R.id.fabSave);
        saveButton.setOnClickListener(v -> doSave());

        generateIcon(storeFieldEdit.getText().toString());
    }

    private void setCardImage(ImageView imageView, Bitmap bitmap) {
        imageView.setTag(bitmap);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_camera_white);
        }
    }

    private void formatExpiryField(Date expiry) {
        if (expiry == null) {
            expiryField.setText(getString(R.string.never));
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
                    tempStoredOldBarcodeValue = null;

                    if (callback != null) {
                        callback.run();
                    }
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    barcodeIdField.setText(tempStoredOldBarcodeValue);
                    tempStoredOldBarcodeValue = null;

                    if (callback != null) {
                        callback.run();
                    }
                })
                .setOnDismissListener(dialogInterface -> {
                    barcodeIdField.setText(tempStoredOldBarcodeValue);
                    tempStoredOldBarcodeValue = null;

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

    class EditCardIdAndBarcode implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            Intent i = new Intent(getApplicationContext(), ScanActivity.class);
            final Bundle b = new Bundle();
            b.putString("cardId", cardIdFieldView.getText().toString());
            i.putExtras(b);
            startActivityForResult(i, Utils.BARCODE_SCAN);
        }
    }

    class ChooseCardImage implements View.OnClickListener
    {
        @Override
        public void onClick(View v) throws NoSuchElementException
        {
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

            cardOptions.put(getString(R.string.chooseImageFromGallery), () -> {
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

    class ColorSelectListener implements View.OnClickListener
    {
        final int defaultColor;

        ColorSelectListener(int defaultColor)
        {
            this.defaultColor = defaultColor;
        }

        @Override
        public void onClick(View v)
        {
            ColorPickerDialog dialog = ColorPickerDialog.newBuilder().setColor(defaultColor).create();
            dialog.setColorPickerDialogListener(new ColorPickerDialogListener()
            {
                @Override
                public void onColorSelected(int dialogId, int color)
                {
                    hasChanged = true;

                    headingColorValue = color;

                    generateIcon(storeFieldEdit.getText().toString());
                }

                @Override
                public void onDialogDismissed(int dialogId)
                {
                    // Nothing to do, no change made
                }
            });
            dialog.show(getSupportFragmentManager(), "color-picker-dialog");
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        final EditText expiryFieldEdit;

        DatePickerFragment(EditText expiryFieldEdit) {
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

            expiryFieldEdit.setTag(date);
            expiryFieldEdit.setText(DateFormat.getDateInstance(DateFormat.LONG).format(date));
        }
    }

    private void doSave() {
        if (tempStoredOldBarcodeValue != null) {
            askBarcodeChange(this::doSave);
            return;
        }

        String store = storeFieldEdit.getText().toString();
        String note = noteFieldEdit.getText().toString();
        Date expiry = (Date) expiryField.getTag();
        BigDecimal balance = (BigDecimal) balanceField.getTag();
        Currency balanceType = balanceCurrencyField.getTag() != null ? ((Currency) balanceCurrencyField.getTag()) : null;
        String cardId = cardIdFieldView.getText().toString();
        String barcodeId = (String) barcodeIdField.getTag();
        BarcodeFormat barcodeType = (BarcodeFormat) barcodeTypeField.getTag();

        if(store.isEmpty())
        {
            Snackbar.make(storeFieldEdit, R.string.noStoreError, Snackbar.LENGTH_LONG).show();
            return;
        }

        if(cardId.isEmpty())
        {
            Snackbar.make(cardIdFieldView, R.string.noCardIdError, Snackbar.LENGTH_LONG).show();
            return;
        }

        if(!validBalance)
        {
            Snackbar.make(balanceField, getString(R.string.parsingBalanceFailed, balanceField.getText().toString()), Snackbar.LENGTH_LONG).show();
            return;
        }

        List<Group> selectedGroups = new ArrayList<>();

        for (Integer chipId : groupsChips.getCheckedChipIds()) {
            Chip chip = groupsChips.findViewById(chipId);
            selectedGroups.add((Group) chip.getTag());
        }

        if(updateLoyaltyCard)
        {   //update of "starStatus" not necessary, since it cannot be changed in this activity (only in ViewActivity)
            db.updateLoyaltyCard(loyaltyCardId, store, note, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headingColorValue);
            try {
                Utils.saveCardImage(this, (Bitmap) cardImageFront.getTag(), loyaltyCardId, true);
                Utils.saveCardImage(this, (Bitmap) cardImageBack.getTag(), loyaltyCardId, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Updated " + loyaltyCardId + " to " + cardId);
        }
        else
        {
            loyaltyCardId = (int)db.insertLoyaltyCard(store, note, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headingColorValue, 0);
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
                askBeforeQuitIfChanged();
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == Utils.CARD_IMAGE_FROM_CAMERA_FRONT || requestCode == Utils.CARD_IMAGE_FROM_CAMERA_BACK) {
            if (resultCode == RESULT_OK) {
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
                }
            }
        } else if (requestCode == Utils.CARD_IMAGE_FROM_FILE_FRONT || requestCode == Utils.CARD_IMAGE_FROM_FILE_BACK) {
            if (resultCode == RESULT_OK) {
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), intent.getData());
                } catch (IOException e) {
                    Log.e(TAG, "Error getting data from image file");
                    e.printStackTrace();
                    Toast.makeText(this, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
                }

                if (bitmap != null) {
                    bitmap = Utils.resizeBitmap(bitmap);
                    if (requestCode == Utils.CARD_IMAGE_FROM_FILE_FRONT) {
                        setCardImage(cardImageFront, bitmap);
                    } else {
                        setCardImage(cardImageBack, bitmap);
                    }

                    hasChanged = true;
                }
            }
        } else {
            BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this);

            if (resultCode == RESULT_OK) {
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
        String cardIdString = barcodeIdField.getTag() != null ? barcodeIdField.getTag().toString() : cardIdFieldView.getText().toString();
        BarcodeFormat barcodeFormat = (BarcodeFormat) barcodeTypeField.getTag();

        if (barcodeFormat == null || cardIdString.isEmpty() || !BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES.contains(barcodeFormat.name())) {
            hideBarcode();
        } else {
            generateBarcode(cardIdString, barcodeFormat);
        }
    }

    private void generateBarcode(String cardIdString, BarcodeFormat barcodeFormat) {
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
                            new BarcodeImageWriterTask(barcodeImage, cardIdString, barcodeFormat, null, false, warnOnInvalidBarcodeType).execute();
                        }
                    });
        } else {
            Log.d(TAG, "ImageView size known known, creating barcode");
            new BarcodeImageWriterTask(barcodeImage, cardIdString, barcodeFormat, null, false, warnOnInvalidBarcodeType).execute();
        }

        showBarcode();
    }

    private void generateIcon(String store) {
        if (headingColorValue == null) {
            return;
        }

        thumbnail.setBackgroundColor(headingColorValue);

        LetterBitmap letterBitmap = Utils.generateIcon(this, store, headingColorValue);

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
