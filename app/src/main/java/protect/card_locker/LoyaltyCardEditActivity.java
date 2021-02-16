package protect.card_locker;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import android.os.LocaleList;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LoyaltyCardEditActivity extends AppCompatActivity
{
    private static final String TAG = "Catima";

    TabLayout tabs;

    ImageView thumbnail;
    EditText storeFieldEdit;
    EditText noteFieldEdit;
    ChipGroup groupsChips;
    AutoCompleteTextView expiryField;
    EditText balanceField;
    AutoCompleteTextView balanceCurrencyField;
    View cardAndBarcodeLayout;
    TextView cardIdFieldView;
    AutoCompleteTextView barcodeTypeField;
    ImageView barcodeImage;
    View barcodeImageLayout;
    View barcodeCaptureLayout;

    Button enterButton;

    int loyaltyCardId;
    boolean updateLoyaltyCard;
    String barcodeType;
    String cardId;

    Uri importLoyaltyCardUri = null;
    Integer headingColorValue = null;

    DBHelper db;
    ImportURIHelper importUriHelper;

    boolean hasChanged = false;
    boolean initDone = false;
    AlertDialog confirmExitDialog = null;

    boolean validBalance = true;

    HashMap<String, Currency> currencies = new HashMap<>();

    private void extractIntentFields(Intent intent)
    {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt("id") : 0;
        updateLoyaltyCard = b != null && b.getBoolean("update", false);

        barcodeType = b != null ? b.getString("barcodeType") : null;
        cardId = b != null ? b.getString("cardId") : null;

        importLoyaltyCardUri = intent.getData();

        Log.d(TAG, "View activity: id=" + loyaltyCardId
                + ", updateLoyaltyCard=" + Boolean.toString(updateLoyaltyCard));
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
        cardAndBarcodeLayout = findViewById(R.id.cardAndBarcodeLayout);
        cardIdFieldView = findViewById(R.id.cardIdView);
        barcodeTypeField = findViewById(R.id.barcodeTypeField);
        barcodeImage = findViewById(R.id.barcode);
        barcodeImageLayout = findViewById(R.id.barcodeLayout);
        barcodeCaptureLayout = findViewById(R.id.barcodeCaptureLayout);

        enterButton = findViewById(R.id.enterButton);

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
                    BigDecimal balance = Utils.parseCurrencyInUserLocale(s.toString());
                    validBalance = true;

                    balanceField.setTag(balance);
                } catch (ParseException | NumberFormatException e) {
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

                balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol((BigDecimal) balanceField.getTag(), currency));
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                String formatString = barcodeTypeField.getText().toString();

                if (!formatString.isEmpty()) {
                    if (formatString.equals(getString(R.string.noBarcode))) {
                        hideBarcode();
                    } else {
                        generateBarcode(s.toString(), BarcodeFormat.valueOf(formatString));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        barcodeTypeField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                if (!s.toString().isEmpty()) {
                    if (s.toString().equals(getString(R.string.noBarcode))) {
                        hideBarcode();
                    } else {
                        generateBarcode(cardIdFieldView.getText().toString(), BarcodeFormat.valueOf(s.toString()));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
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
        balanceField.setTag(null);
        balanceField.setText("");
        balanceCurrencyField.setTag(null);
        balanceCurrencyField.setText("");
        cardIdFieldView.setText("");
        barcodeTypeField.setText("");
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

            if(balanceField.getText().length() == 0)
            {
                balanceField.setTag(loyaltyCard.balance);
                balanceField.setText(Utils.formatBalanceWithoutCurrencySymbol(loyaltyCard.balance, loyaltyCard.balanceType));
            }

            if(balanceCurrencyField.getText().length() == 0)
            {
                balanceCurrencyField.setTag(loyaltyCard.balanceType);
                formatBalanceCurrencyField(loyaltyCard.balanceType);
            }

            if(cardIdFieldView.getText().length() == 0)
            {
                cardIdFieldView.setText(loyaltyCard.cardId);
            }

            if(barcodeTypeField.getText().length() == 0)
            {
                barcodeTypeField.setText(loyaltyCard.barcodeType.isEmpty() ? getString(R.string.noBarcode) : loyaltyCard.barcodeType);
            }

            if(headingColorValue == null)
            {
                headingColorValue = loyaltyCard.headerColor;
                if(headingColorValue == null)
                {
                    headingColorValue = LetterBitmap.getDefaultColor(this, loyaltyCard.store);
                }
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
            barcodeTypeField.setText(importCard.barcodeType);
            headingColorValue = importCard.headerColor;
        }
        else
        {
            setTitle(R.string.addCardTitle);
            expiryField.setTag(null);
            expiryField.setText(getString(R.string.never));
            balanceField.setTag(new BigDecimal("0.0"));
            balanceCurrencyField.setTag(null);
            formatBalanceCurrencyField(null);
            hideBarcode();
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
                chip.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        hasChanged = true;

                        return false;
                    }
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

        if (!initDone) {
            hasChanged = false;
            initDone = true;
        }

        // Update from intent
        if (barcodeType != null) {
            barcodeTypeField.setText(barcodeType.isEmpty() ? getString(R.string.noBarcode) : barcodeType);
            barcodeType = null;
        }
        if (cardId != null) {
            cardIdFieldView.setText(cardId);
            cardId = null;
        }

        if(cardIdFieldView.getText().length() > 0 && barcodeTypeField.getText().length() > 0)
        {
            String formatString = barcodeTypeField.getText().toString();

            if(formatString.isEmpty() || formatString.equals(getString(R.string.noBarcode)))
            {
                hideBarcode();
            }
            else
            {
                final BarcodeFormat format = BarcodeFormat.valueOf(formatString);
                final String cardIdString = cardIdFieldView.getText().toString();

                generateBarcode(cardIdString, format);
            }
        }

        enterButton.setOnClickListener(new EditCardIdAndBarcode());
        barcodeImage.setOnClickListener(new EditCardIdAndBarcode());

        ArrayList<String> barcodeList = new ArrayList<>(BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES);
        barcodeList.add(0, getString(R.string.noBarcode));
        ArrayAdapter<String> barcodeAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, barcodeList);
        barcodeTypeField.setAdapter(barcodeAdapter);

        FloatingActionButton saveButton = findViewById(R.id.fabSave);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSave();
            }
        });

        generateIcon(storeFieldEdit.getText().toString());
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

    private void askBeforeQuitIfChanged() {
        if (!hasChanged) {
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

    private void doSave()
    {
        String store = storeFieldEdit.getText().toString();
        String note = noteFieldEdit.getText().toString();
        Date expiry = (Date) expiryField.getTag();
        BigDecimal balance = (BigDecimal) balanceField.getTag();
        Currency balanceType = balanceCurrencyField.getTag() != null ? ((Currency) balanceCurrencyField.getTag()) : null;
        String cardId = cardIdFieldView.getText().toString();
        String barcodeType = barcodeTypeField.getText().toString();

        // We do not want to save the no barcode string to the database
        // it is simply an empty there for no barcode
        if(barcodeType.equals(getString(R.string.noBarcode)))
        {
            barcodeType = "";
        }

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
            db.updateLoyaltyCard(loyaltyCardId, store, note, expiry, balance, balanceType, cardId, barcodeType, headingColorValue);
            Log.i(TAG, "Updated " + loyaltyCardId + " to " + cardId);
        }
        else
        {
            loyaltyCardId = (int)db.insertLoyaltyCard(store, note, expiry, balance, balanceType, cardId, barcodeType, headingColorValue, 0);
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

        BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent);

        barcodeType = barcodeValues.format();
        cardId = barcodeValues.content();

        onResume();
    }

    private void showBarcode() {
        barcodeImageLayout.setVisibility(View.VISIBLE);
    }

    private void hideBarcode() {
        barcodeImageLayout.setVisibility(View.GONE);
    }

    private void generateBarcode(final String cardId, final BarcodeFormat barcodeFormat) {
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
                            new BarcodeImageWriterTask(barcodeImage, cardId, barcodeFormat).execute();
                        }
                    });
        } else {
            Log.d(TAG, "ImageView size known known, creating barcode");
            new BarcodeImageWriterTask(barcodeImage, cardId, barcodeFormat).execute();
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
        View cardPart = findViewById(R.id.cardPart);
        View barcodePart = findViewById(R.id.barcodePart);

        if (getString(R.string.card).equals(part)) {
            cardPart.setVisibility(View.VISIBLE);
            barcodePart.setVisibility(View.GONE);

            // Explicitly hide barcode (fixes blurriness on redraw)
            hideBarcode();
        } else if (getString(R.string.barcode).equals(part)) {
            cardPart.setVisibility(View.GONE);
            barcodePart.setVisibility(View.VISIBLE);

            // Redraw barcode due to size change (Visibility.GONE sets it to 0)
            String formatString = barcodeTypeField.getText().toString();
            if (formatString.isEmpty() || formatString.equals(getString(R.string.noBarcode))) {
                hideBarcode();
            } else {
                generateBarcode(cardIdFieldView.getText().toString(), BarcodeFormat.valueOf(formatString));
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
