package protect.card_locker;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.core.widget.TextViewCompat;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Intents;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Currency;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class LoyaltyCardViewActivityTest
{
    private final String BARCODE_DATA = "428311627547";
    private final String BARCODE_TYPE = BarcodeFormat.UPC_A.name();

    private final String EAN_BARCODE_DATA = "4763705295336";
    private final String EAN_BARCODE_TYPE = BarcodeFormat.EAN_13.name();

    enum ViewMode
    {
        ADD_CARD,
        VIEW_CARD,
        UPDATE_CARD,
        ;
    }

    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;
    }

    /**
     * Register a handler in the package manager for a image capture intent
     */
    private void registerMediaStoreIntentHandler()
    {
        // Add something that will 'handle' the media capture intent
        PackageManager packageManager = RuntimeEnvironment.application.getPackageManager();

        ResolveInfo info = new ResolveInfo();
        info.isDefault = true;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = "does.not.matter";
        info.activityInfo = new ActivityInfo();
        info.activityInfo.applicationInfo = applicationInfo;
        info.activityInfo.name = "DoesNotMatter";

        Intent intent = new Intent(Intents.Scan.ACTION);

        shadowOf(packageManager).addResolveInfoForIntent(intent, info);
    }

    /**
     * Save a loyalty card and check that the database contains the
     * expected values
     */
    private void saveLoyaltyCardWithArguments(final Activity activity,
                                              final String store, final String note,
                                              final Date expiry,
                                              final BigDecimal balance,
                                              final Currency balanceType,
                                              final String cardId,
                                              final String barcodeType,
                                              boolean creatingNewCard)
    {
        DBHelper db = new DBHelper(activity);
        if(creatingNewCard)
        {
            assertEquals(0, db.getLoyaltyCardCount());
        }
        else
        {
            assertEquals(1, db.getLoyaltyCardCount());
        }

        final EditText storeField = activity.findViewById(R.id.storeNameEdit);
        final EditText noteField = activity.findViewById(R.id.noteEdit);
        final TextInputLayout expiryView = activity.findViewById(R.id.expiryView);
        final EditText balanceView = activity.findViewById(R.id.balanceField);
        final EditText balanceCurrencyField = activity.findViewById(R.id.balanceCurrencyField);
        final TextView cardIdField = activity.findViewById(R.id.cardIdView);
        final TextView barcodeTypeField = activity.findViewById(R.id.barcodeTypeField);

        storeField.setText(store);
        noteField.setText(note);
        expiryView.setTag(expiry);
        if (balance != null) {
            balanceView.setText(balance.toPlainString());
        }
        if (balanceType != null) {
            balanceCurrencyField.setText(balanceType.getSymbol());
        }
        cardIdField.setText(cardId);
        barcodeTypeField.setText(barcodeType);

        assertEquals(false, activity.isFinishing());
        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(true, activity.isFinishing());

        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);
        assertEquals(store, card.store);
        assertEquals(note, card.note);
        assertEquals(expiry, card.expiry);
        if (balance != null) {
            assertEquals(balance, card.balance);
        } else {
            assertEquals(new BigDecimal("0"), card.balance);
        }
        assertEquals(balanceType, card.balanceType);
        assertEquals(cardId, card.cardId);

        // The special "No barcode" string shouldn't actually be written to the loyalty card
        if(barcodeType.equals(activity.getApplicationContext().getString(R.string.noBarcode)))
        {
            assertEquals("", card.barcodeType);
        }
        else
        {
            assertEquals(barcodeType, card.barcodeType);
        }
        assertNotNull(card.headerColor);
        assertNotNull(card.headerTextColor);

        db.close();
    }

    /**
     * Initiate and complete a barcode capture, either in success
     * or in failure
     */
    private void captureBarcodeWithResult(final Activity activity, final boolean success) throws IOException
    {
        // Start image capture
        final Button startButton = activity.findViewById(R.id.enterButton);
        startButton.performClick();

        ShadowActivity.IntentForResult intentForResult = shadowOf(activity).peekNextStartedActivityForResult();
        assertNotNull(intentForResult);

        Intent intent = intentForResult.intent;
        assertNotNull(intent);

        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);

        Intent resultIntent = new Intent(intent);
        Bundle resultBundle = new Bundle();
        resultBundle.putString(BarcodeSelectorActivity.BARCODE_CONTENTS, BARCODE_DATA);
        resultBundle.putString(BarcodeSelectorActivity.BARCODE_FORMAT, BARCODE_TYPE);
        resultIntent.putExtras(resultBundle);

        // Respond to image capture, success
        shadowOf(activity).receiveResult(
                intent,
                success ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                resultIntent);
    }

    /**
     * Initiate and complete a barcode selection, either in success
     * or in failure
     */
    private void selectBarcodeWithResult(final Activity activity, final String barcodeData, final String barcodeType, final boolean success) throws IOException
    {
        // Start barcode selector
        final Button startButton = activity.findViewById(R.id.enterButton);
        startButton.performClick();

        ShadowActivity.IntentForResult intentForResult = shadowOf(activity).peekNextStartedActivityForResult();
        Intent intent = intentForResult.intent;
        assertNotNull(intent);
        assertEquals(intent.getComponent().getClassName(), ScanActivity.class.getCanonicalName());

        Activity newActivity = Robolectric.buildActivity(ScanActivity.class, intent).create().get();

        final Button manualButton = newActivity.findViewById(R.id.add_manually);
        manualButton.performClick();

        intentForResult = shadowOf(activity).peekNextStartedActivityForResult();
        assertNotNull(intentForResult);

        intent = intentForResult.intent;
        assertNotNull(intent);

        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);

        Intent resultIntent = new Intent(intent);
        Bundle resultBundle = new Bundle();
        resultBundle.putString(BarcodeSelectorActivity.BARCODE_FORMAT, barcodeType);
        resultBundle.putString(BarcodeSelectorActivity.BARCODE_CONTENTS, barcodeData);
        resultIntent.putExtras(resultBundle);

        // Respond to barcode selection, success
        shadowOf(activity).receiveResult(
                intent,
                success ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                resultIntent);
    }

    private void checkFieldProperties(final Activity activity, final int id, final int visibility,
                                      final String contents)
    {
        final View view = activity.findViewById(id);
        assertNotNull(view);
        assertEquals(visibility, view.getVisibility());
        if(contents != null)
        {
            try {
                TextView textView = (TextView) view;
                assertEquals(contents, textView.getText().toString());
            } catch (ClassCastException e) {
                TextInputLayout textView = (TextInputLayout) view;
                assertEquals(contents, textView.getEditText().getText().toString());
            }
        }
    }

    private void checkAllFields(final Activity activity, ViewMode mode,
                                final String store, final String note, final String expiryString,
                                final String balanceString, final String balanceTypeString,
                                final String cardId, final String barcodeType)
    {
        if(mode == ViewMode.VIEW_CARD)
        {
            checkFieldProperties(activity, R.id.cardIdView, View.VISIBLE, cardId);
        }
        else
        {
            int editVisibility = View.VISIBLE;

            checkFieldProperties(activity, R.id.storeNameEdit, editVisibility, store);
            checkFieldProperties(activity, R.id.noteEdit, editVisibility, note);
            checkFieldProperties(activity, R.id.expiryView, editVisibility, expiryString);
            checkFieldProperties(activity, R.id.balanceField, editVisibility, balanceString);
            checkFieldProperties(activity, R.id.balanceCurrencyField, editVisibility, balanceTypeString);
            checkFieldProperties(activity, R.id.cardIdView, View.VISIBLE, cardId);
            checkFieldProperties(activity, R.id.barcodeTypeField, View.VISIBLE, barcodeType);
            checkFieldProperties(activity, R.id.barcode, View.VISIBLE, null);
        }
    }

    @Test
    public void startWithoutParametersCheckFieldsAvailable()
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never) , "0", context.getString(R.string.points), "", "");
    }

    @Test
    public void startWithoutParametersCannotCreateLoyaltyCard()
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        
        DBHelper db = new DBHelper(activity);
        assertEquals(0, db.getLoyaltyCardCount());

        final EditText storeField = activity.findViewById(R.id.storeNameEdit);
        final EditText noteField = activity.findViewById(R.id.noteEdit);

        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(0, db.getLoyaltyCardCount());

        storeField.setText("store");
        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(0, db.getLoyaltyCardCount());

        noteField.setText("note");
        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(0, db.getLoyaltyCardCount());

        db.close();
    }

    @Test
    public void startWithoutParametersBack()
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void startWithoutParametersCaptureBarcodeCreateLoyaltyCard() throws IOException
    {
        registerMediaStoreIntentHandler();

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", "");

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, BARCODE_TYPE);

        shadowOf(getMainLooper()).idle();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", null, null, null, BARCODE_DATA, BARCODE_TYPE, true);
    }

    @Test
    public void startWithoutParametersCaptureBarcodeFailure() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", "");

        // Complete barcode capture in failure
        captureBarcodeWithResult(activity, false);

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", "");
    }

    @Test
    public void startWithoutParametersCaptureBarcodeCancel() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", "");

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, BARCODE_TYPE);

        // Cancel the loyalty card creation
        assertEquals(false, activity.isFinishing());

        // A change was made
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.confirmExitDialog.isShowing());
        assertEquals(true, activity.hasChanged);
        assertEquals(false, activity.isFinishing());

        // Exit after setting hasChanged to false
        activity.hasChanged = false;
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(false, activity.hasChanged);
        assertEquals(true, activity.isFinishing());
    }

    private ActivityController createActivityWithLoyaltyCard(boolean editMode)
    {
        Intent intent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putInt("id", 1);

        Class clazz;

        if(editMode)
        {
            bundle.putBoolean("update", true);
            clazz = LoyaltyCardEditActivity.class;
        }
        else
        {
            bundle.putBoolean("view", true);
            clazz = LoyaltyCardViewActivity.class;
        }

        intent.putExtras(bundle);

        return Robolectric.buildActivity(clazz, intent).create();
    }

    @Test
    public void startWithLoyaltyCardEditModeCheckDisplay() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", null, "0", context.getString(R.string.points), BARCODE_DATA, BARCODE_TYPE);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardViewModeCheckDisplay() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.VIEW_CARD, "store", "note", null, "0", context.getString(R.string.points), BARCODE_DATA, BARCODE_TYPE);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardWithBarcodeUpdateBarcode() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", null, "0", context.getString(R.string.points), EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", null, "0", context.getString(R.string.points), BARCODE_DATA, BARCODE_TYPE);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardWithReceiptUpdateReceiptCancel() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", null, "0", context.getString(R.string.points), EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", null, "0", context.getString(R.string.points), BARCODE_DATA, BARCODE_TYPE);

        // Cancel the loyalty card creation
        assertEquals(false, activity.isFinishing());
        // A change was made
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.confirmExitDialog.isShowing());
        assertEquals(true, activity.hasChanged);
        assertEquals(false, activity.isFinishing());

        // Exit after setting hasChanged to false
        activity.hasChanged = false;
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(false, activity.hasChanged);
        assertEquals(true, activity.isFinishing());

        db.close();
    }

    @Test
    public void startWithLoyaltyCardNoExpirySetExpiry() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Set date to today
        MaterialAutoCompleteTextView expiryField = activity.findViewById(R.id.expiryField);
        expiryField.setText(expiryField.getAdapter().getItem(1).toString(), false);

        shadowOf(getMainLooper()).idle();

        DatePickerDialog datePickerDialog = (DatePickerDialog) (ShadowDialog.getLatestDialog());
        assertNotNull(datePickerDialog);
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "0", context.getString(R.string.points), EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardExpirySetNoExpiry() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", new Date(), new BigDecimal("0"), null, EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "0", context.getString(R.string.points), EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Set date to never
        MaterialAutoCompleteTextView expiryField = activity.findViewById(R.id.expiryField);
        expiryField.setText(expiryField.getAdapter().getItem(0).toString(), false);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardNoBalanceSetBalance() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Set balance to 10 points
        EditText balanceField = activity.findViewById(R.id.balanceField);
        balanceField.setText("10");

        shadowOf(getMainLooper()).idle();

        // Change points to EUR
        MaterialAutoCompleteTextView balanceTypeField = activity.findViewById(R.id.balanceCurrencyField);
        balanceTypeField.setText("€", false);

        shadowOf(getMainLooper()).idle();

        // Ensure the balance is reformatted for EUR when focus is cleared
        shadowOf(getMainLooper()).idle();
        balanceField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                assertEquals("10.00", balanceField.getText().toString());

                shadowOf(getMainLooper()).idle();

                DatePickerDialog datePickerDialog = (DatePickerDialog) (ShadowDialog.getLatestDialog());
                assertNotNull(datePickerDialog);
                datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick();

                shadowOf(getMainLooper()).idle();

                checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "10.00", "€", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

                db.close();
            }
        });
        balanceField.clearFocus();
    }

    @Test
    public void startWithLoyaltyCardBalanceSetNoBalance() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("10.00"), Currency.getInstance("USD"), EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "10.00", "$", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        shadowOf(getMainLooper()).idle();

        // Change EUR to WON
        MaterialAutoCompleteTextView balanceTypeField = activity.findViewById(R.id.balanceCurrencyField);
        balanceTypeField.setText("₩", false);

        shadowOf(getMainLooper()).idle();

        // Ensure the balance is reformatted for WON when focus is cleared
        EditText balanceField = activity.findViewById(R.id.balanceField);
        balanceField.clearFocus();
        assertEquals("10", balanceField.getText().toString());

        shadowOf(getMainLooper()).idle();

        // Set the balance to 0
        balanceField.setText("0");

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", "₩", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        db.close();
    }

    @Test
    public void checkMenu() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The share, settings, add and star button should be present
        assertEquals(menu.size(), 3);

        assertEquals("Block Rotation", menu.findItem(R.id.action_lock_unlock).getTitle().toString());
        assertEquals("Share", menu.findItem(R.id.action_share).getTitle().toString());
        assertEquals("Add to favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        db.close();
    }

    @Test
    public void startWithMissingLoyaltyCard() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        // The activity should find that the card is missing and shut down

        assertTrue(activity.isFinishing());

        // Make sure the activity can close down
        activityController.pause();
        activityController.stop();
        activityController.destroy();
    }

    @Test
    public void startWithoutParametersViewBack()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());

        db.close();
    }

    @Test
    public void startWithoutColors()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, null, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());

        db.close();
    }

    @Test
    public void startLoyaltyCardWithoutColorsSave() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, null, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, false);

        db.close();
    }

    @Test
    public void startLoyaltyCardWithExplicitNoBarcodeSave() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, "", Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, activity.getApplicationContext().getString(R.string.noBarcode), false);

        db.close();
    }

    @Test
    public void removeBarcodeFromLoyaltyCard() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // First check if the card is as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, BARCODE_TYPE);

        // Complete empty barcode selection successfully
        selectBarcodeWithResult(activity, BARCODE_DATA, "", true);

        // Check if the barcode type is NO_BARCODE as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, activity.getApplicationContext().getString(R.string.noBarcode));
        assertEquals(View.GONE, activity.findViewById(R.id.barcodeLayout).getVisibility());

        // Check if the special NO_BARCODE string doesn't get saved
        saveLoyaltyCardWithArguments(activity, "store", "note", null, null, null, BARCODE_DATA, activity.getApplicationContext().getString(R.string.noBarcode), false);

        db.close();
    }

    @Test
    public void startCheckFontSizes()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

        final int STORE_FONT_SIZE = 50;
        final int CARD_FONT_SIZE = 40;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        settings.edit()
            .putInt(activity.getResources().getString(R.string.settings_key_card_title_font_size), STORE_FONT_SIZE)
            .putInt(activity.getResources().getString(R.string.settings_key_card_id_font_size), CARD_FONT_SIZE)
            .apply();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        TextView storeName = activity.findViewById(R.id.storeName);
        TextView cardIdFieldView = activity.findViewById(R.id.cardIdView);

        TextViewCompat.getAutoSizeMaxTextSize(storeName);
        TextViewCompat.getAutoSizeMaxTextSize(storeName);
        assertEquals(STORE_FONT_SIZE, (int)storeName.getTextSize());
        assertEquals(CARD_FONT_SIZE, TextViewCompat.getAutoSizeMaxTextSize(cardIdFieldView));

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());

        db.close();
    }

    @Test
    public void checkScreenOrientationLockSetting()
    {
        for(boolean locked : new boolean[] {false, true})
        {
            ActivityController activityController = createActivityWithLoyaltyCard(false);

            Activity activity = (Activity)activityController.get();
            DBHelper db = new DBHelper(activity);
            db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
            settings.edit()
                    .putBoolean(activity.getResources().getString(R.string.settings_key_lock_barcode_orientation), locked)
                    .apply();

            activityController.start();
            activityController.resume();
            activityController.visible();

            assertEquals(false, activity.isFinishing());

            MenuItem item = shadowOf(activity).getOptionsMenu().findItem(R.id.action_lock_unlock);

            if(locked)
            {
                assertEquals(item.isVisible(), false);
            }
            else
            {
                assertEquals(item.isVisible(), true);
                String title = item.getTitle().toString();
                assertEquals(title, activity.getString(R.string.lockScreen));
            }

            db.close();
        }
    }

    @Test
    public void checkPushStarIcon()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity) activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK,0);
        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        shadowOf(getMainLooper()).idle();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The share, settings and star button should be present
        assertEquals(menu.size(), 3);

        assertEquals("Add to favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        shadowOf(activity).clickMenuItem(R.id.action_star_unstar);
        shadowOf(getMainLooper()).idle();
        assertEquals("Remove from favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        shadowOf(activity).clickMenuItem(R.id.action_star_unstar);
        shadowOf(getMainLooper()).idle();
        assertEquals("Add to favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        db.close();
    }

    @Test
    public void checkBarcodeFullscreenWorkflow()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, BARCODE_TYPE, Color.BLACK, 0);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        ImageView barcodeImage = activity.findViewById(R.id.barcode);
        View collapsingToolbarLayout = activity.findViewById(R.id.collapsingToolbarLayout);
        View bottomSheet = activity.findViewById(R.id.bottom_sheet);
        ImageButton maximizeButton = activity.findViewById(R.id.maximizeButton);
        ImageButton minimizeButton = activity.findViewById(R.id.minimizeButton);
        FloatingActionButton editButton = activity.findViewById(R.id.fabEdit);
        SeekBar barcodeScaler = activity.findViewById(R.id.barcodeScaler);

        // Android should not be in fullscreen mode
        int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);

        // Elements should be visible (except minimize button and scaler)
        assertEquals(View.VISIBLE, collapsingToolbarLayout.getVisibility());
        assertEquals(View.VISIBLE, bottomSheet.getVisibility());
        assertEquals(View.VISIBLE, maximizeButton.getVisibility());
        assertEquals(View.GONE, minimizeButton.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());
        assertEquals(View.GONE, barcodeScaler.getVisibility());

        // Click barcode to toggle fullscreen
        barcodeImage.performClick();
        shadowOf(getMainLooper()).idle();

        // Android should be in fullscreen mode
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);

        // Elements should not be visible (except minimize button and scaler)
        assertEquals(View.GONE, collapsingToolbarLayout.getVisibility());
        assertEquals(View.GONE, bottomSheet.getVisibility());
        assertEquals(View.GONE, maximizeButton.getVisibility());
        assertEquals(View.VISIBLE, minimizeButton.getVisibility());
        assertEquals(View.GONE, editButton.getVisibility());
        assertEquals(View.VISIBLE, barcodeScaler.getVisibility());

        // Clicking barcode again should deactivate fullscreen mode
        barcodeImage.performClick();
        shadowOf(getMainLooper()).idle();
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);
        assertEquals(View.VISIBLE, collapsingToolbarLayout.getVisibility());
        assertEquals(View.VISIBLE, bottomSheet.getVisibility());
        assertEquals(View.VISIBLE, maximizeButton.getVisibility());
        assertEquals(View.GONE, minimizeButton.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());
        assertEquals(View.GONE, barcodeScaler.getVisibility());

        // Another click back to fullscreen
        barcodeImage.performClick();
        shadowOf(getMainLooper()).idle();
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);
        assertEquals(View.GONE, collapsingToolbarLayout.getVisibility());
        assertEquals(View.GONE, bottomSheet.getVisibility());
        assertEquals(View.GONE, maximizeButton.getVisibility());
        assertEquals(View.VISIBLE, minimizeButton.getVisibility());
        assertEquals(View.GONE, editButton.getVisibility());
        assertEquals(View.VISIBLE, barcodeScaler.getVisibility());

        // In full screen mode, back button should disable fullscreen
        activity.onBackPressed();
        shadowOf(getMainLooper()).idle();
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);
        assertEquals(View.VISIBLE, collapsingToolbarLayout.getVisibility());
        assertEquals(View.VISIBLE, bottomSheet.getVisibility());
        assertEquals(View.VISIBLE, maximizeButton.getVisibility());
        assertEquals(View.GONE, minimizeButton.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());
        assertEquals(View.GONE, barcodeScaler.getVisibility());

        // Pressing back when not in full screen should finish activity
        activity.onBackPressed();
        shadowOf(getMainLooper()).idle();
        assertEquals(true, activity.isFinishing());

        db.close();
    }

    @Test
    public void importCard()
    {
        Date date = new Date();

        Uri importUri = Uri.parse("https://thelastproject.github.io/Catima/share?store=Example%20Store&note=&expiry=" + date.getTime() + "&balance=10&balancetype=USD&cardid=123456&barcodetype=AZTEC&headercolor=-416706&headertextcolor=-1");

        Intent intent = new Intent();
        intent.setData(importUri);

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class, intent).create();

        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.ADD_CARD, "Example Store", "", DateFormat.getDateInstance(DateFormat.LONG).format(date), "10.00", "$", "123456", "AZTEC");
        assertEquals(-416706, ((ColorDrawable) activity.findViewById(R.id.thumbnail).getBackground()).getColor());
    }

    @Test
    public void importCardOldURL()
    {
        Uri importUri = Uri.parse("https://brarcher.github.io/loyalty-card-locker/share?store=Example%20Store&note=&cardid=123456&barcodetype=AZTEC&headercolor=-416706&headertextcolor=-1");

        Intent intent = new Intent();
        intent.setData(importUri);

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class, intent).create();

        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = ApplicationProvider.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "Example Store", "", context.getString(R.string.never), "0", context.getString(R.string.points), "123456", "AZTEC");
        assertEquals(-416706, ((ColorDrawable) activity.findViewById(R.id.thumbnail).getBackground()).getColor());
    }
}
