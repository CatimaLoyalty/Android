package protect.card_locker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Intents;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Date;

import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.TextViewCompat;
import androidx.preference.PreferenceManager;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class LoyaltyCardViewActivityTest
{
    private final String BARCODE_DATA = "428311627547";
    private final CatimaBarcode BARCODE_TYPE = CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A);

    private final String EAN_BARCODE_DATA = "4763705295336";
    private final CatimaBarcode EAN_BARCODE_TYPE = CatimaBarcode.fromBarcode(BarcodeFormat.EAN_13);

    enum ViewMode
    {
        ADD_CARD,
        VIEW_CARD,
        UPDATE_CARD,
        ;
    }

    enum FieldTypeView
    {
        TextView,
        TextInputLayout,
        ImageView
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
                                              final String expiry,
                                              final BigDecimal balance,
                                              final String balanceType,
                                              final String cardId,
                                              final String barcodeId,
                                              final String barcodeType,
                                              boolean creatingNewCard) throws ParseException {
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
        final TextView barcodeIdField = activity.findViewById(R.id.barcodeIdField);
        final TextView barcodeTypeField = activity.findViewById(R.id.barcodeTypeField);

        storeField.setText(store);
        noteField.setText(note);
        expiryView.setTag(expiry);
        balanceView.setText(balance.toPlainString());
        balanceCurrencyField.setText(balanceType);
        cardIdField.setText(cardId);
        barcodeIdField.setText(barcodeId);
        barcodeTypeField.setText(barcodeType);

        assertEquals(false, activity.isFinishing());
        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(true, activity.isFinishing());

        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);
        assertEquals(store, card.store);
        assertEquals(note, card.note);
        assertEquals(balance, card.balance);

        // The special "Never" string shouldn't actually be written to the loyalty card
        if(expiry.equals(activity.getApplicationContext().getString(R.string.never)))
        {
            assertEquals(null, card.expiry);
        }
        else
        {
            assertEquals(DateFormat.getDateInstance().parse(expiry), card.expiry);
        }

        // The special "Points" string shouldn't actually be written to the loyalty card
        if(balanceType.equals(activity.getApplicationContext().getString(R.string.points)))
        {
            assertEquals(null, card.balanceType);
        }
        else
        {
            assertEquals(Currency.getInstance(balanceType), card.balanceType);
        }
        assertEquals(cardId, card.cardId);

        // The special "Same as barcode ID" string shouldn't actually be written to the loyalty card
        if(barcodeId.equals(activity.getApplicationContext().getString(R.string.sameAsCardId)))
        {
            assertEquals(null, card.barcodeId);
        }
        else
        {
            assertEquals(barcodeId, card.barcodeId);
        }

        // The special "No barcode" string shouldn't actually be written to the loyalty card
        if(barcodeType.equals(activity.getApplicationContext().getString(R.string.noBarcode)))
        {
            assertEquals(null, card.barcodeType);
        }
        else
        {
            assertEquals(CatimaBarcode.fromName(barcodeType).format(), card.barcodeType.format());
        }
        assertNotNull(card.headerColor);

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
        resultBundle.putString(BarcodeSelectorActivity.BARCODE_FORMAT, BARCODE_TYPE.name());
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
                                      final Object contents, final FieldTypeView fieldType)
    {
        final View view = activity.findViewById(id);
        assertNotNull(view);
        assertEquals(visibility, view.getVisibility());

        if (fieldType == FieldTypeView.TextView) {
            TextView textView = (TextView) view;
            assertEquals(contents, textView.getText().toString());
        } else if (fieldType == FieldTypeView.TextInputLayout) {
            TextInputLayout textView = (TextInputLayout) view;
            assertEquals(contents, textView.getEditText().getText().toString());
        } else if (fieldType == FieldTypeView.ImageView) {
            ImageView imageView = (ImageView) view;
            Bitmap image = null;
            if (imageView.getTag() != null) {
                image = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            }
            assertEquals(contents, image);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void checkAllFields(final Activity activity, ViewMode mode,
                                final String store, final String note, final String expiryString,
                                final String balanceString, final String balanceTypeString,
                                final String cardId, final String barcodeId,
                                final String barcodeType, final Bitmap frontImage,
                                final Bitmap backImage)
    {
        if(mode == ViewMode.VIEW_CARD)
        {
            checkFieldProperties(activity, R.id.cardIdView, View.VISIBLE, cardId, FieldTypeView.TextView);
        }
        else
        {
            int editVisibility = View.VISIBLE;

            checkFieldProperties(activity, R.id.storeNameEdit, editVisibility, store, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.noteEdit, editVisibility, note, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.expiryView, editVisibility, expiryString, FieldTypeView.TextInputLayout);
            checkFieldProperties(activity, R.id.balanceField, editVisibility, balanceString, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.balanceCurrencyField, editVisibility, balanceTypeString, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.cardIdView, View.VISIBLE, cardId, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.barcodeIdField, View.VISIBLE, barcodeId, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.barcodeTypeField, View.VISIBLE, barcodeType, FieldTypeView.TextView);
            //checkFieldProperties(activity, R.id.barcode, View.VISIBLE, null, FieldTypeView.ImageView);
            checkFieldProperties(activity, R.id.frontImage, View.VISIBLE, frontImage, FieldTypeView.ImageView);
            checkFieldProperties(activity, R.id.backImage, View.VISIBLE, backImage, FieldTypeView.ImageView);
        }
    }

    @Test
    public void noDataLossOnResumeOrRotate()
    {
        registerMediaStoreIntentHandler();

        for(boolean newCard : new boolean[] {false, true}) {
            System.out.println();
            System.out.println("=====");
            System.out.println("New card? " + newCard);
            System.out.println("=====");
            System.out.println();

            ActivityController activityController;

            if (!newCard) {
                activityController = createActivityWithLoyaltyCard(true);
            } else {
                activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
            }

            LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();
            final Context context = activity.getApplicationContext();
            DBHelper db = TestHelpers.getEmptyDb(activity);

            if (!newCard) {
                db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);
            }

            activityController.start();
            activityController.visible();
            activityController.resume();

            shadowOf(getMainLooper()).idle();

            // Check default settings
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, newCard ? "" : "store", newCard ? "" : "note", context.getString(R.string.never), "0", context.getString(R.string.points), newCard ? "" : EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), newCard ? context.getString(R.string.noBarcode) : EAN_BARCODE_TYPE.prettyName(), null, null);

            // Change everything
            final EditText storeField = activity.findViewById(R.id.storeNameEdit);
            final EditText noteField = activity.findViewById(R.id.noteEdit);
            final EditText expiryField = activity.findViewById(R.id.expiryField);
            final EditText balanceField = activity.findViewById(R.id.balanceField);
            final EditText balanceTypeField = activity.findViewById(R.id.balanceCurrencyField);
            final EditText cardIdField = activity.findViewById(R.id.cardIdView);
            final EditText barcodeField = activity.findViewById(R.id.barcodeIdField);
            final EditText barcodeTypeField = activity.findViewById(R.id.barcodeTypeField);
            final ImageView frontImageView = activity.findViewById(R.id.frontImage);
            final ImageView backImageView = activity.findViewById(R.id.backImage);

            Currency currency = Currency.getInstance("EUR");
            Date expiryDate = new Date();
            Bitmap frontBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.circle);
            Bitmap backBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.save_24dp);

            storeField.setText("correct store");
            noteField.setText("correct note");
            LoyaltyCardEditActivity.formatExpiryField(context, expiryField, expiryDate);
            balanceField.setText("100");
            balanceTypeField.setText(currency.getSymbol());
            cardIdField.setText("12345678");
            barcodeField.setText("87654321");
            barcodeTypeField.setText(CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName());
            activity.setCardImage(frontImageView, frontBitmap, true);
            activity.setCardImage(backImageView, backBitmap, true);

            shadowOf(getMainLooper()).idle();

            // Check if changed
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);

            // Resume
            activityController.pause();
            activityController.resume();

            shadowOf(getMainLooper()).idle();

            // Check if no changes lost
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);

            // Rotate to landscape
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            activity.recreate();
            shadowOf(getMainLooper()).idle();

            // Check if no changes lost
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);

            // Rotate to portrait
            shadowOf(getMainLooper()).idle();
            activity.recreate();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // Check if no changes lost
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);
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
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never) , "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId),context.getString(R.string.noBarcode), null, null);
    }

    @Test
    public void startWithoutParametersCannotCreateLoyaltyCard()
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        
        DBHelper db = TestHelpers.getEmptyDb(activity);
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
    public void startWithoutParametersCaptureBarcodeCreateLoyaltyCard() throws IOException, ParseException {
        registerMediaStoreIntentHandler();

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId),context.getString(R.string.noBarcode), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        shadowOf(getMainLooper()).idle();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", context.getString(R.string.never), new BigDecimal("0"), context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.name(), true);
    }

    @Test
    public void startWithoutParametersCaptureBarcodeFailure() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);

        // Complete barcode capture in failure
        captureBarcodeWithResult(activity, false);
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId),context.getString(R.string.noBarcode), null, null);
    }

    @Test
    public void startWithoutParametersCaptureBarcodeCancel() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId),context.getString(R.string.noBarcode), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

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
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardViewModeCheckDisplay() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.VIEW_CARD, "store", "note", null, "0", context.getString(R.string.points), BARCODE_DATA, null, BARCODE_TYPE.toString(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardWithBarcodeUpdateBarcode() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardWithReceiptUpdateReceiptCancel() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

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
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Set date to today
        MaterialAutoCompleteTextView expiryField = activity.findViewById(R.id.expiryField);
        expiryField.setText(expiryField.getAdapter().getItem(1).toString(), false);

        shadowOf(getMainLooper()).idle();

        DatePickerDialog datePickerDialog = (DatePickerDialog) (ShadowDialog.getLatestDialog());
        assertNotNull(datePickerDialog);
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardExpirySetNoExpiry() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", new Date(), new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Set date to never
        MaterialAutoCompleteTextView expiryField = activity.findViewById(R.id.expiryField);
        expiryField.setText(expiryField.getAdapter().getItem(0).toString(), false);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardNoBalanceSetBalance() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

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

                checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "10.00", "€", EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE.toString(), null, null);

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
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("10.00"), Currency.getInstance("USD"), EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "10.00", "$", EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

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

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", "₩", EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardSameAsCardIDUpdateBarcodeID()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Change barcode ID
        EditText barcodeField = activity.findViewById(R.id.barcodeIdField);
        barcodeField.setText("123456");

        // Switch away from card ID and ensure no dialog appears
        TabLayout tabs = activity.findViewById(R.id.tabs);
        tabs.getTabAt(2).select();
        shadowOf(getMainLooper()).idle();
        AlertDialog updateBarcodeIdDialog = (AlertDialog) (ShadowDialog.getLatestDialog());
        assertNull(updateBarcodeIdDialog);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardSameAsCardIDUpdateCardID()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Change card ID
        EditText cardIdField = activity.findViewById(R.id.cardIdView);
        cardIdField.setText("123456");

        // Switch away from card ID and ensure no dialog appears
        TabLayout tabs = activity.findViewById(R.id.tabs);
        tabs.getTabAt(2).select();
        shadowOf(getMainLooper()).idle();
        AlertDialog updateBarcodeIdDialog = (AlertDialog) (ShadowDialog.getLatestDialog());
        assertNull(updateBarcodeIdDialog);

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), "123456", context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardDifferentFromCardIDUpdateCardIDUpdate()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

        // Change card ID
        EditText cardIdField = activity.findViewById(R.id.cardIdView);
        cardIdField.setText("654321");

        shadowOf(getMainLooper()).idle();

        // Switch away from card ID and ensure the dialog appears
        TabLayout tabs = activity.findViewById(R.id.tabs);
        tabs.getTabAt(2).select();
        shadowOf(getMainLooper()).idle();
        AlertDialog updateBarcodeIdDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(updateBarcodeIdDialog);
        updateBarcodeIdDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), "654321", context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void startWithLoyaltyCardDifferentFromCardIDUpdateCardIDDoNotUpdate()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

        // Change card ID
        EditText cardIdField = activity.findViewById(R.id.cardIdView);
        cardIdField.setText("654321");

        shadowOf(getMainLooper()).idle();

        // Switch away from card ID and ensure the dialog appears
        TabLayout tabs = activity.findViewById(R.id.tabs);
        tabs.getTabAt(2).select();
        shadowOf(getMainLooper()).idle();
        AlertDialog updateBarcodeIdDialog = (AlertDialog) (ShadowDialog.getLatestDialog());
        assertNotNull(updateBarcodeIdDialog);
        updateBarcodeIdDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).performClick();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), "654321", "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

        db.close();
    }

    @Test
    public void checkMenu() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);
        Activity activity = (Activity)activityController.get();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

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
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

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
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, null, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());

        db.close();
    }

    @Test
    public void startLoyaltyCardWithoutColorsSave() throws IOException, ParseException {
        ActivityController activityController = createActivityWithLoyaltyCard(true);

        Activity activity = (Activity)activityController.get();
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, null, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", activity.getApplicationContext().getString(R.string.never), new BigDecimal("0"), activity.getApplicationContext().getString(R.string.points), BARCODE_DATA, activity.getApplicationContext().getString(R.string.sameAsCardId), BARCODE_TYPE.name(), false);

        db.close();
    }

    @Test
    public void startLoyaltyCardWithExplicitNoBarcodeSave() throws IOException, ParseException {
        ActivityController activityController = createActivityWithLoyaltyCard(true);

        Activity activity = (Activity)activityController.get();
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, null, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", activity.getApplicationContext().getString(R.string.never), new BigDecimal("0"), activity.getApplicationContext().getString(R.string.points), BARCODE_DATA, activity.getApplicationContext().getString(R.string.sameAsCardId), activity.getApplicationContext().getString(R.string.noBarcode), false);

        db.close();
    }

    @Test
    public void removeBarcodeFromLoyaltyCard() throws IOException, ParseException {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();
        DBHelper db = TestHelpers.getEmptyDb(activity);

        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // First check if the card is as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        // Complete empty barcode selection successfully
        selectBarcodeWithResult(activity, BARCODE_DATA, "", true);
        activityController.resume();

        // Check if the barcode type is NO_BARCODE as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);
        assertEquals(View.GONE, activity.findViewById(R.id.barcodeLayout).getVisibility());

        // Check if the special NO_BARCODE string doesn't get saved
        saveLoyaltyCardWithArguments(activity, "store", "note", context.getString(R.string.never), new BigDecimal("0"), context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), false);

        db.close();
    }

    @Test
    public void startCheckFontSizes()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

        final int LARGE_FONT_SIZE = 40;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        settings.edit()
            .putInt(activity.getResources().getString(R.string.settings_key_max_font_size_scale), 100)
            .apply();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        TextView storeName = activity.findViewById(R.id.storeName);
        TextView cardIdFieldView = activity.findViewById(R.id.cardIdView);

        TextViewCompat.getAutoSizeMaxTextSize(storeName);
        TextViewCompat.getAutoSizeMaxTextSize(storeName);
        assertEquals(LARGE_FONT_SIZE, TextViewCompat.getAutoSizeMaxTextSize(cardIdFieldView));

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
            db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

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
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);
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
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        ImageView mainImage = activity.findViewById(R.id.mainImage);
        View collapsingToolbarLayout = activity.findViewById(R.id.collapsingToolbarLayout);
        View bottomSheet = activity.findViewById(R.id.bottom_sheet);
        ImageButton maximizeButton = activity.findViewById(R.id.maximizeButton);
        ImageButton minimizeButton = activity.findViewById(R.id.minimizeButton);
        LinearLayout dotIndicator = activity.findViewById(R.id.dotIndicator);
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
        assertEquals(View.GONE, dotIndicator.getVisibility()); // We have no images, only a barcode

        // Click maximize button to activate fullscreen
        maximizeButton.performClick();
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
        assertEquals(View.GONE, dotIndicator.getVisibility()); // We have no images, only a barcode

        // Clicking minimize button should deactivate fullscreen mode
        minimizeButton.performClick();
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
        assertEquals(View.GONE, dotIndicator.getVisibility()); // We have no images, only a barcode

        // Another click back to fullscreen
        maximizeButton.performClick();
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
        assertEquals(View.GONE, dotIndicator.getVisibility()); // We have no images, only a barcode

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
        assertEquals(View.GONE, dotIndicator.getVisibility()); // We have no images, only a barcode

        // Pressing back when not in full screen should finish activity
        activity.onBackPressed();
        shadowOf(getMainLooper()).idle();
        assertEquals(true, activity.isFinishing());

        db.close();
    }

    @Test
    public void checkNoBarcodeFullscreenWorkflow()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = TestHelpers.getEmptyDb(activity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, BARCODE_DATA, null, null, Color.BLACK, 0, null);

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

        // Elements should be visible (except minimize/maximize buttons and barcode and scaler)
        assertEquals(View.VISIBLE, collapsingToolbarLayout.getVisibility());
        assertEquals(View.VISIBLE, bottomSheet.getVisibility());
        assertEquals(View.GONE, maximizeButton.getVisibility());
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

        Uri importUri = Uri.parse("https://catima.app/share#store%3DExample%2BStore%26note%3D%26expiry%3D" + date.getTime() + "%26balance%3D10.00%26balancetype%3DUSD%26cardid%3D123456%26barcodetype%3DAZTEC%26headercolor%3D-416706");

        Intent intent = new Intent();
        intent.setData(importUri);

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class, intent).create();

        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.ADD_CARD, "Example Store", "", DateFormat.getDateInstance(DateFormat.LONG).format(date), "10.00", "$", "123456", context.getString(R.string.sameAsCardId), "Aztec", null, null);
        assertEquals(-416706, ((ColorDrawable) activity.findViewById(R.id.thumbnail).getBackground()).getColor());
    }

    @Test
    public void importCardOldFormat()
    {
        Uri importUri = Uri.parse("https://brarcher.github.io/loyalty-card-locker/share?store=Example%20Store&note=&cardid=123456&barcodetype=AZTEC&headercolor=-416706&headertextcolor=-1");

        Intent intent = new Intent();
        intent.setData(importUri);

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class, intent).create();

        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "Example Store", "", context.getString(R.string.never), "0", context.getString(R.string.points), "123456", context.getString(R.string.sameAsCardId), "Aztec", null, null);
        assertEquals(-416706, ((ColorDrawable) activity.findViewById(R.id.thumbnail).getBackground()).getColor());
    }
}
