package protect.card_locker;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.bottomappbar.BottomAppBar;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Date;

@RunWith(RobolectricTestRunner.class)
public class LoyaltyCardViewActivityTest {
    private final String BARCODE_DATA = "428311627547";
    private final CatimaBarcode BARCODE_TYPE = CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A);

    private final String EAN_BARCODE_DATA = "4763705295336";
    private final CatimaBarcode EAN_BARCODE_TYPE = CatimaBarcode.fromBarcode(BarcodeFormat.EAN_13);

    enum ViewMode {
        ADD_CARD,
        VIEW_CARD,
        UPDATE_CARD,
        ;
    }

    enum FieldTypeView {
        TextView,
        TextInputLayout,
        ImageView
    }

    @Before
    public void setUp() {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;
    }

    /**
     * Register a handler in the package manager for a image capture intent
     */
    private void registerMediaStoreIntentHandler() {
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
                                              final String validFrom,
                                              final String expiry,
                                              final BigDecimal balance,
                                              final String balanceType,
                                              final String cardId,
                                              final String barcodeId,
                                              final String barcodeType,
                                              boolean creatingNewCard) throws ParseException {
        SQLiteDatabase database = new DBHelper(activity).getWritableDatabase();
        if (creatingNewCard) {
            assertEquals(0, DBHelper.getLoyaltyCardCount(database));
        } else {
            assertEquals(1, DBHelper.getLoyaltyCardCount(database));
        }

        final EditText storeField = activity.findViewById(R.id.storeNameEdit);
        final EditText noteField = activity.findViewById(R.id.noteEdit);
        final TextInputLayout validFromView = activity.findViewById(R.id.validFromView);
        final TextInputLayout expiryView = activity.findViewById(R.id.expiryView);
        final EditText balanceView = activity.findViewById(R.id.balanceField);
        final EditText balanceCurrencyField = activity.findViewById(R.id.balanceCurrencyField);
        final TextView cardIdField = activity.findViewById(R.id.cardIdView);
        final TextView barcodeIdField = activity.findViewById(R.id.barcodeIdField);
        final TextView barcodeTypeField = activity.findViewById(R.id.barcodeTypeField);

        storeField.setText(store);
        noteField.setText(note);
        validFromView.setTag(validFrom);
        expiryView.setTag(expiry);
        balanceView.setText(balance.toPlainString());
        balanceCurrencyField.setText(balanceType);
        cardIdField.setText(cardId);
        barcodeIdField.setText(barcodeId);
        barcodeTypeField.setText(barcodeType);

        assertEquals(false, activity.isFinishing());
        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(true, activity.isFinishing());

        assertEquals(1, DBHelper.getLoyaltyCardCount(database));

        LoyaltyCard card = DBHelper.getLoyaltyCard(activity.getApplicationContext(), database, 1);
        assertEquals(store, card.store);
        assertEquals(note, card.note);
        assertEquals(balance, card.balance);

        // The special "Any date" string shouldn't actually be written to the loyalty card
        if (validFrom.equals(activity.getApplicationContext().getString(R.string.anyDate))) {
            assertEquals(null, card.validFrom);
        } else {
            assertEquals(DateFormat.getDateInstance().parse(validFrom), card.validFrom);
        }

        // The special "Never" string shouldn't actually be written to the loyalty card
        if (expiry.equals(activity.getApplicationContext().getString(R.string.never))) {
            assertEquals(null, card.expiry);
        } else {
            assertEquals(DateFormat.getDateInstance().parse(expiry), card.expiry);
        }

        // The special "Points" string shouldn't actually be written to the loyalty card
        if (balanceType.equals(activity.getApplicationContext().getString(R.string.points))) {
            assertEquals(null, card.balanceType);
        } else {
            assertEquals(Currency.getInstance(balanceType), card.balanceType);
        }
        assertEquals(cardId, card.cardId);

        // The special "Same as barcode ID" string shouldn't actually be written to the loyalty card
        if (barcodeId.equals(activity.getApplicationContext().getString(R.string.sameAsCardId))) {
            assertEquals(null, card.barcodeId);
        } else {
            assertEquals(barcodeId, card.barcodeId);
        }

        // The special "No barcode" string shouldn't actually be written to the loyalty card
        if (barcodeType.equals(activity.getApplicationContext().getString(R.string.noBarcode))) {
            assertEquals(null, card.barcodeType);
        } else {
            assertEquals(CatimaBarcode.fromName(barcodeType).format(), card.barcodeType.format());
        }
        assertNotNull(card.headerColor);

        database.close();
    }

    /**
     * Initiate and complete a barcode capture, either in success
     * or in failure
     */
    private void captureBarcodeWithResult(final Activity activity, final boolean success) {
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

        LoyaltyCard loyaltyCard = new LoyaltyCard();
        loyaltyCard.setBarcodeId(null);
        loyaltyCard.setBarcodeType(BARCODE_TYPE);
        loyaltyCard.setCardId(BARCODE_DATA);
        ParseResult parseResult = new ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard);

        resultIntent.putExtras(parseResult.toLoyaltyCardBundle(activity));

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
    private void selectBarcodeWithResult(final Activity activity, final String barcodeData, final String barcodeType, final boolean success) {
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

        LoyaltyCard loyaltyCard = new LoyaltyCard();
        loyaltyCard.setBarcodeId(null);
        loyaltyCard.setBarcodeType(barcodeType != null ? CatimaBarcode.fromName(barcodeType) : null);
        loyaltyCard.setCardId(barcodeData);
        ParseResult parseResult = new ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard);

        resultIntent.putExtras(parseResult.toLoyaltyCardBundle(activity));

        // Respond to barcode selection, success
        shadowOf(activity).receiveResult(
                intent,
                success ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                resultIntent);
    }

    private void checkFieldProperties(final Activity activity, final int id, final int visibility,
                                      final Object contents, final FieldTypeView fieldType) {
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
            try {
                image = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            } catch (ClassCastException e) {
                // This is probably a VectorDrawable, the placeholder image. Aka: No image.
            }

            if (contents == null && image == null) {
                return;
            }

            assertTrue(image.sameAs((Bitmap) contents));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void checkAllFields(final Activity activity, ViewMode mode,
                                final String store, final String note, final String validFromString,
                                final String expiryString, final String balanceString,
                                final String balanceTypeString, final String cardId,
                                final String barcodeId, final String barcodeType,
                                final Bitmap frontImage, final Bitmap backImage) {
        if (mode == ViewMode.VIEW_CARD) {
            checkFieldProperties(activity, R.id.main_image_description, View.VISIBLE, cardId, FieldTypeView.TextView);
        } else {
            int editVisibility = View.VISIBLE;

            checkFieldProperties(activity, R.id.storeNameEdit, editVisibility, store, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.noteEdit, editVisibility, note, FieldTypeView.TextView);
            checkFieldProperties(activity, R.id.validFromView, editVisibility, validFromString, FieldTypeView.TextInputLayout);
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
    @Config(qualifiers="de")
    public void noCrashOnRegionlessLocale() {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();

        LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();
        final Context context = activity.getApplicationContext();

        activityController.start();
        activityController.visible();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        // Check default settings
        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);
    }

    @Test
    public void noDataLossOnResumeOrRotate() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        registerMediaStoreIntentHandler();

        Integer cardId;

        for (boolean newCard : new boolean[]{false, true}) {
            System.out.println();
            System.out.println("=====");
            System.out.println("New card? " + newCard);
            System.out.println("=====");
            System.out.println();

            if (!newCard) {
                cardId = (int) DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);
            } else {
                cardId = null;
            }

            ActivityController activityController = createActivityWithLoyaltyCard(true, cardId);
            LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();

            activityController.start();
            activityController.visible();
            activityController.resume();

            shadowOf(getMainLooper()).idle();

            // Check default settings
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, newCard ? "" : "store", newCard ? "" : "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), newCard ? "" : EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), newCard ? context.getString(R.string.noBarcode) : EAN_BARCODE_TYPE.prettyName(), null, null);

            // Change everything
            final EditText storeField = activity.findViewById(R.id.storeNameEdit);
            final EditText noteField = activity.findViewById(R.id.noteEdit);
            final EditText validFromField = activity.findViewById(R.id.validFromField);
            final EditText expiryField = activity.findViewById(R.id.expiryField);
            final EditText balanceField = activity.findViewById(R.id.balanceField);
            final EditText balanceTypeField = activity.findViewById(R.id.balanceCurrencyField);
            final EditText cardIdField = activity.findViewById(R.id.cardIdView);
            final EditText barcodeField = activity.findViewById(R.id.barcodeIdField);
            final EditText barcodeTypeField = activity.findViewById(R.id.barcodeTypeField);
            final ImageView frontImageView = activity.findViewById(R.id.frontImage);
            final ImageView backImageView = activity.findViewById(R.id.backImage);

            Currency currency = Currency.getInstance("EUR");
            Date validFromDate = Date.from(Instant.now().minus(20, ChronoUnit.DAYS));
            Date expiryDate = new Date();
            Bitmap frontBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.circle);
            Bitmap backBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_done);

            storeField.setText("correct store");
            noteField.setText("correct note");
            LoyaltyCardEditActivity.formatDateField(context, validFromField, validFromDate);
            activity.setLoyaltyCardValidFrom(validFromDate);
            LoyaltyCardEditActivity.formatDateField(context, expiryField, expiryDate);
            activity.setLoyaltyCardExpiry(expiryDate);
            balanceField.setText("100");
            balanceTypeField.setText(currency.getSymbol());
            cardIdField.setText("12345678");
            barcodeField.setText("87654321");
            barcodeTypeField.setText(CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName());
            activity.setCardImage(ImageLocationType.front, frontImageView, frontBitmap, true);
            activity.setCardImage(ImageLocationType.back, backImageView, backBitmap, true);

            shadowOf(getMainLooper()).idle();

            // Check if changed
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(validFromDate), DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);

            // Resume
            activityController.pause();
            activityController.resume();

            shadowOf(getMainLooper()).idle();

            // Check if no changes lost
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(validFromDate), DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);

            // Rotate to landscape
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            activity.recreate();
            shadowOf(getMainLooper()).idle();

            // Check if no changes lost
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(validFromDate), DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);

            // Rotate to portrait
            shadowOf(getMainLooper()).idle();
            activity.recreate();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // Check if no changes lost
            checkAllFields(activity, newCard ? ViewMode.ADD_CARD : ViewMode.UPDATE_CARD, "correct store", "correct note", DateFormat.getDateInstance(DateFormat.LONG).format(validFromDate), DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate), "100.00", currency.getSymbol(), "12345678", "87654321", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE).prettyName(), frontBitmap, backBitmap);
        }
    }

    @Test
    public void startWithoutParametersCheckFieldsAvailable() {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity) activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);
    }

    @Test
    public void startWithoutParametersCannotCreateLoyaltyCard() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity) activityController.get();

        assertEquals(0, DBHelper.getLoyaltyCardCount(database));

        final EditText storeField = activity.findViewById(R.id.storeNameEdit);
        final EditText noteField = activity.findViewById(R.id.noteEdit);

        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(0, DBHelper.getLoyaltyCardCount(database));

        storeField.setText("store");
        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(0, DBHelper.getLoyaltyCardCount(database));

        noteField.setText("note");
        activity.findViewById(R.id.fabSave).performClick();
        assertEquals(0, DBHelper.getLoyaltyCardCount(database));

        database.close();
    }

    @Test
    public void startWithoutParametersBack() {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity) activityController.get();

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

        Activity activity = (Activity) activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        shadowOf(getMainLooper()).idle();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), new BigDecimal("0"), context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.name(), true);
    }

    @Test
    public void startWithoutParametersCaptureBarcodeFailure() throws IOException {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity) activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);

        // Complete barcode capture in failure
        captureBarcodeWithResult(activity, false);
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);
    }

    @Test
    public void startWithoutParametersCaptureBarcodeCancel() throws IOException {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "", context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        // Cancel the loyalty card creation
        assertEquals(false, activity.isFinishing());

        // A change was made
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.confirmExitDialog.isShowing());
        assertEquals(true, activity.viewModel.getHasChanged());
        assertEquals(false, activity.isFinishing());

        // Exit after setting hasChanged to false
        activity.viewModel.setHasChanged(false);
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(false, activity.viewModel.getHasChanged());
        assertEquals(true, activity.isFinishing());
    }

    private ActivityController createActivityWithLoyaltyCard(boolean editMode, @Nullable Integer loyaltyCardId) {
        Intent intent = new Intent();
        final Bundle bundle = new Bundle();

        Class clazz;

        if (editMode) {
            if (loyaltyCardId != null) {
                bundle.putInt(LoyaltyCardEditActivity.BUNDLE_ID, loyaltyCardId);
                bundle.putBoolean(LoyaltyCardEditActivity.BUNDLE_UPDATE, true);
            }
            clazz = LoyaltyCardEditActivity.class;
        } else {
            bundle.putInt(LoyaltyCardViewActivity.BUNDLE_ID, loyaltyCardId);
            clazz = LoyaltyCardViewActivity.class;
        }

        intent.putExtras(bundle);

        return Robolectric.buildActivity(clazz, intent).create();
    }

    @Test
    public void startWithLoyaltyCardEditModeCheckDisplay() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();


        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardViewModeCheckDisplay() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(false, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.VIEW_CARD, "store", "note", null, null, "0", context.getString(R.string.points), BARCODE_DATA, null, BARCODE_TYPE.toString(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardWithBarcodeUpdateBarcode() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardWithReceiptUpdateReceiptCancel() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        LoyaltyCardEditActivity activity = (LoyaltyCardEditActivity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, true);
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        // Cancel the loyalty card creation
        assertEquals(false, activity.isFinishing());
        // A change was made
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.confirmExitDialog.isShowing());
        assertEquals(true, activity.viewModel.getHasChanged());
        assertEquals(false, activity.isFinishing());

        // Exit after setting hasChanged to false
        activity.viewModel.setHasChanged(false);
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(false, activity.viewModel.getHasChanged());
        assertEquals(true, activity.isFinishing());

        database.close();
    }

    @Test
    public void startWithLoyaltyCardNoExpirySetExpiry() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Set date to today
        MaterialAutoCompleteTextView expiryField = activity.findViewById(R.id.expiryField);
        expiryField.setText(expiryField.getAdapter().getItem(1).toString(), false);

        shadowOf(getMainLooper()).idle();

        Dialog datePickerDialog = ShadowDialog.getLatestDialog();
        assertNotNull(datePickerDialog);
        datePickerDialog.findViewById(com.google.android.material.R.id.confirm_button).performClick();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardExpirySetNoExpiry() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, new Date(), new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Set date to never
        MaterialAutoCompleteTextView expiryField = activity.findViewById(R.id.expiryField);
        expiryField.setText(expiryField.getAdapter().getItem(0).toString(), false);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardNoBalanceSetBalance() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

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

                checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), DateFormat.getDateInstance(DateFormat.LONG).format(new Date()), "10.00", "€", EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE.toString(), null, null);

                database.close();
            }
        });
        balanceField.clearFocus();
    }

    @Test
    public void startWithLoyaltyCardBalanceSetNoBalance() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("10.00"), Currency.getInstance("USD"), EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "10.00", "$", EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

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

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", "₩", EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardSameAsCardIDUpdateBarcodeID() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        // Change barcode ID
        EditText barcodeField = activity.findViewById(R.id.barcodeIdField);
        barcodeField.setText("123456");

        // Switch away from card ID and ensure no dialog appears
        TabLayout tabs = activity.findViewById(R.id.tabs);
        tabs.getTabAt(2).select();
        shadowOf(getMainLooper()).idle();
        AlertDialog updateBarcodeIdDialog = (AlertDialog) (ShadowDialog.getLatestDialog());
        assertNull(updateBarcodeIdDialog);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardSameAsCardIDUpdateCardID() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, null, EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

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

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "123456", context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardDifferentFromCardIDUpdateCardIDUpdate() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

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

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "654321", context.getString(R.string.sameAsCardId), EAN_BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void startWithLoyaltyCardDifferentFromCardIDUpdateCardIDDoNotUpdate() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), EAN_BARCODE_DATA, "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

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

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "654321", "123456", EAN_BARCODE_TYPE.prettyName(), null, null);

        database.close();
    }

    @Test
    public void checkMenu() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(false, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The share, star and overflow options should be present
        assertEquals(menu.size(), 3);

        assertEquals("Share", menu.findItem(R.id.action_share).getTitle().toString());
        assertEquals("Add to favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        database.close();
    }

    @Test
    public void startWithMissingLoyaltyCard() throws IOException {
        ActivityController activityController = createActivityWithLoyaltyCard(true, 1);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();

        // The activity should find that the card is missing and shut down
        assertTrue(activity.isFinishing());

        // Make sure the activity can close down
        activityController.pause();
        activityController.stop();
        activityController.destroy();
    }

    @Test
    public void startWithoutParametersViewBack() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(false, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());

        database.close();
    }

    @Test
    public void startWithoutColors() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, null, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(false, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());

        database.close();
    }

    @Test
    public void startLoyaltyCardWithoutColorsSave() throws IOException, ParseException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, null, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", activity.getApplicationContext().getString(R.string.anyDate), activity.getApplicationContext().getString(R.string.never), new BigDecimal("0"), activity.getApplicationContext().getString(R.string.points), BARCODE_DATA, activity.getApplicationContext().getString(R.string.sameAsCardId), BARCODE_TYPE.name(), false);

        database.close();
    }

    @Test
    public void startLoyaltyCardWithExplicitNoBarcodeSave() throws IOException, ParseException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, null, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", activity.getApplicationContext().getString(R.string.anyDate), activity.getApplicationContext().getString(R.string.never), new BigDecimal("0"), activity.getApplicationContext().getString(R.string.points), BARCODE_DATA, activity.getApplicationContext().getString(R.string.sameAsCardId), activity.getApplicationContext().getString(R.string.noBarcode), false);

        database.close();
    }

    @Test
    public void removeBarcodeFromLoyaltyCard() throws IOException, ParseException {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(true, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        // First check if the card is as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), BARCODE_TYPE.prettyName(), null, null);

        // Complete empty barcode selection successfully
        selectBarcodeWithResult(activity, BARCODE_DATA, null, true);
        activityController.resume();

        // Check if the barcode type is NO_BARCODE as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), null, null);
        assertEquals(View.GONE, activity.findViewById(R.id.barcodeLayout).getVisibility());

        // Check if the special NO_BARCODE string doesn't get saved
        saveLoyaltyCardWithArguments(activity, "store", "note", context.getString(R.string.anyDate), context.getString(R.string.never), new BigDecimal("0"), context.getString(R.string.points), BARCODE_DATA, context.getString(R.string.sameAsCardId), context.getString(R.string.noBarcode), false);

        database.close();
    }

    @Test
    public void checkPushStarIcon() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(false, (int) cardId);
        Activity activity = (Activity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        shadowOf(getMainLooper()).idle();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The share, star and overflow options should be present
        assertEquals(menu.size(), 3);

        assertEquals("Add to favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        shadowOf(activity).clickMenuItem(R.id.action_star_unstar);
        shadowOf(getMainLooper()).idle();
        assertEquals("Remove from favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        shadowOf(activity).clickMenuItem(R.id.action_star_unstar);
        shadowOf(getMainLooper()).idle();
        assertEquals("Add to favorites", menu.findItem(R.id.action_star_unstar).getTitle().toString());

        database.close();
    }

    @Test
    public void checkBarcodeFullscreenWorkflow() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(false, (int) cardId);
        AppCompatActivity activity = (AppCompatActivity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertFalse(activity.isFinishing());

        BottomAppBar bottomAppBar = activity.findViewById(R.id.bottom_app_bar);
        ImageView mainImage = activity.findViewById(R.id.main_image);
        LinearLayout container = activity.findViewById(R.id.container);
        ConstraintLayout fullScreenLayout = activity.findViewById(R.id.fullscreen_layout);
        ImageButton minimizeButton = activity.findViewById(R.id.fullscreen_button_minimize);
        FloatingActionButton editButton = activity.findViewById(R.id.fabEdit);

        // Android should not be in fullscreen mode
        assertTrue(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.statusBars()));
        assertTrue(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.navigationBars()));
        assertEquals(WindowInsetsController.BEHAVIOR_DEFAULT, activity.getWindow().getInsetsController().getSystemBarsBehavior());

        // Elements should be visible (except minimize button and scaler)
        assertEquals(View.VISIBLE, bottomAppBar.getVisibility());
        assertEquals(View.VISIBLE, container.getVisibility());
        assertEquals(View.GONE, fullScreenLayout.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());

        // Click maximize button to activate fullscreen
        mainImage.performClick();
        shadowOf(getMainLooper()).idle();

        // Android should be in fullscreen mode
        assertFalse(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.statusBars()));
        assertFalse(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.navigationBars()));
        assertEquals(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE, activity.getWindow().getInsetsController().getSystemBarsBehavior());

        // Elements should not be visible (except minimize button and scaler)
        assertEquals(View.GONE, bottomAppBar.getVisibility());
        assertEquals(View.GONE, container.getVisibility());
        assertEquals(View.VISIBLE, fullScreenLayout.getVisibility());
        assertEquals(View.GONE, editButton.getVisibility());

        // Clicking minimize button should deactivate fullscreen mode
        minimizeButton.performClick();
        shadowOf(getMainLooper()).idle();

        assertTrue(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.statusBars()));
        assertTrue(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.navigationBars()));
        assertEquals(WindowInsetsController.BEHAVIOR_DEFAULT, activity.getWindow().getInsetsController().getSystemBarsBehavior());

        assertEquals(View.VISIBLE, bottomAppBar.getVisibility());
        assertEquals(View.VISIBLE, container.getVisibility());
        assertEquals(View.GONE, fullScreenLayout.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());

        // Another click back to fullscreen
        mainImage.performClick();
        shadowOf(getMainLooper()).idle();

        assertFalse(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.statusBars()));
        assertFalse(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.navigationBars()));
        assertEquals(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE, activity.getWindow().getInsetsController().getSystemBarsBehavior());

        assertEquals(View.GONE, bottomAppBar.getVisibility());
        assertEquals(View.GONE, container.getVisibility());
        assertEquals(View.VISIBLE, fullScreenLayout.getVisibility());
        assertEquals(View.GONE, editButton.getVisibility());

        // In full screen mode, back button should disable fullscreen
        activity.getOnBackPressedDispatcher().onBackPressed();
        shadowOf(getMainLooper()).idle();

        assertTrue(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.statusBars()));
        assertTrue(activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.navigationBars()));
        assertEquals(WindowInsetsController.BEHAVIOR_DEFAULT, activity.getWindow().getInsetsController().getSystemBarsBehavior());

        assertEquals(View.VISIBLE, bottomAppBar.getVisibility());
        assertEquals(View.VISIBLE, container.getVisibility());
        assertEquals(View.GONE, fullScreenLayout.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());

        // Pressing back when not in full screen should finish activity
        activity.getOnBackPressedDispatcher().onBackPressed();
        shadowOf(getMainLooper()).idle();
        assertTrue(activity.isFinishing());

        database.close();
    }

    @Test
    public void checkNoBarcodeFullscreenWorkflow() {
        final Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TestHelpers.getEmptyDb(context).getWritableDatabase();

        long cardId = DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, null, Color.BLACK, 0, null, 0);

        ActivityController activityController = createActivityWithLoyaltyCard(false, (int) cardId);
        AppCompatActivity activity = (AppCompatActivity) activityController.get();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        BottomAppBar bottomAppBar = activity.findViewById(R.id.bottom_app_bar);
        ImageView mainImage = activity.findViewById(R.id.main_image);
        ConstraintLayout fullScreenLayout = activity.findViewById(R.id.fullscreen_layout);
        FloatingActionButton editButton = activity.findViewById(R.id.fabEdit);

        // Android should not be in fullscreen mode
        int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);

        // Elements should be visible (except minimize/maximize buttons and barcode and scaler)
        assertEquals(View.VISIBLE, bottomAppBar.getVisibility());
        assertEquals(View.GONE, mainImage.getVisibility());
        assertEquals(View.GONE, fullScreenLayout.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());

        // Pressing back when not in full screen should finish activity
        activity.getOnBackPressedDispatcher().onBackPressed();
        shadowOf(getMainLooper()).idle();
        assertEquals(true, activity.isFinishing());

        database.close();
    }

    @Test
    public void importCard() {
        Date date = new Date();

        Uri importUri = Uri.parse("https://catima.app/share#store%3DExample%2BStore%26note%3D%26validfrom%3D" + date.getTime() + "%26expiry%3D" + date.getTime() + "%26balance%3D10.00%26balancetype%3DUSD%26cardid%3D123456%26barcodetype%3DAZTEC%26headercolor%3D-416706");

        Intent intent = new Intent();
        intent.setData(importUri);

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class, intent).create();

        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity) activityController.get();
        final Context context = activity.getApplicationContext();

        shadowOf(getMainLooper()).idle();

        checkAllFields(activity, ViewMode.ADD_CARD, "Example Store", "", DateFormat.getDateInstance(DateFormat.LONG).format(date), DateFormat.getDateInstance(DateFormat.LONG).format(date), "10.00", "$", "123456", context.getString(R.string.sameAsCardId), "Aztec", null, null);
        assertEquals(-416706, ((ColorDrawable) activity.findViewById(R.id.thumbnail).getBackground()).getColor());
    }

    @Test
    public void importCardOldFormat() {
        Uri importUri = Uri.parse("https://brarcher.github.io/loyalty-card-locker/share?store=Example%20Store&note=&cardid=123456&barcodetype=AZTEC&headercolor=-416706&headertextcolor=-1");

        Intent intent = new Intent();
        intent.setData(importUri);

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class, intent).create();

        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity) activityController.get();
        final Context context = activity.getApplicationContext();

        checkAllFields(activity, ViewMode.ADD_CARD, "Example Store", "", context.getString(R.string.anyDate), context.getString(R.string.never), "0", context.getString(R.string.points), "123456", context.getString(R.string.sameAsCardId), "Aztec", null, null);
        assertEquals(-416706, ((ColorDrawable) activity.findViewById(R.id.thumbnail).getBackground()).getColor());
    }
}
