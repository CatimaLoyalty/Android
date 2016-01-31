package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Intents;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.util.ActivityController;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class LoyaltyCardViewActivityTest
{
    private final String BARCODE_DATA = "428311627547";
    private final String BARCODE_TYPE = BarcodeFormat.UPC_A.name();

    private final String EAN_BARCODE_DATA = "4763705295336";
    private final String EAN_BARCODE_TYPE = BarcodeFormat.EAN_13.name();

    /**
     * Register a handler in the package manager for a image capture intent
     */
    private void registerMediaStoreIntentHandler()
    {
        // Add something that will 'handle' the media capture intent
        RobolectricPackageManager packageManager = (RobolectricPackageManager) shadowOf(
                RuntimeEnvironment.application).getPackageManager();

        ResolveInfo info = new ResolveInfo();
        info.isDefault = true;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = "does.not.matter";
        info.activityInfo = new ActivityInfo();
        info.activityInfo.applicationInfo = applicationInfo;
        info.activityInfo.name = "DoesNotMatter";

        Intent intent = new Intent(Intents.Scan.ACTION);

        packageManager.addResolveInfoForIntent(intent, info);
    }

    /**
     * Save a loyalty card and check that the database contains the
     * expected values
     */
    private void saveLoyaltyCardWithArguments(final Activity activity,
                                              final String store, final String cardId,
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

        final EditText storeField = (EditText) activity.findViewById(R.id.storeName);
        final EditText cardIdField = (EditText) activity.findViewById(R.id.cardId);
        final EditText barcodeTypeField = (EditText) activity.findViewById(R.id.barcodeType);

        final Button saveButton = (Button) activity.findViewById(R.id.saveButton);

        storeField.setText(store);
        cardIdField.setText(cardId);
        barcodeTypeField.setText(barcodeType);

        assertEquals(false, activity.isFinishing());
        saveButton.performClick();
        assertEquals(true, activity.isFinishing());

        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);
        assertEquals(store, card.store);
        assertEquals(cardId, card.cardId);
        assertEquals(barcodeType, card.barcodeType);
    }

    /**
     * Initiate and complete a barcode capture, either in success
     * or in failure
     */
    private void captureBarcodeWithResult(final Activity activity, final int buttonId, final boolean success) throws IOException
    {
        // Start image capture
        final Button captureButton = (Button) activity.findViewById(buttonId);
        captureButton.performClick();

        ShadowActivity.IntentForResult intentForResult = shadowOf(activity).peekNextStartedActivityForResult();
        assertNotNull(intentForResult);

        Intent intent = intentForResult.intent;
        assertNotNull(intent);

        String action = intent.getAction();
        assertNotNull(action);
        assertEquals(Intents.Scan.ACTION, action);

        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);

        Intent resultIntent = new Intent(intent);
        Bundle resultBuddle = new Bundle();
        resultBuddle.putString(Intents.Scan.RESULT, BARCODE_DATA);
        resultBuddle.putString(Intents.Scan.RESULT_FORMAT, BARCODE_TYPE);
        resultIntent.putExtras(resultBuddle);

        // Respond to image capture, success
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
            TextView textView = (TextView)view;
            assertEquals(contents, textView.getText().toString());
        }
    }

    private void checkAllFields(final Activity activity, final String store, final String cardId,
    final String barcodeType)
    {
        int cardIdVisibility = cardId.isEmpty() ? View.GONE : View.VISIBLE;

        checkFieldProperties(activity, R.id.storeName, View.VISIBLE, store);
        checkFieldProperties(activity, R.id.cardId, View.VISIBLE, cardId);
        checkFieldProperties(activity, R.id.barcodeType, View.VISIBLE, barcodeType);
        checkFieldProperties(activity, R.id.captureButton, View.VISIBLE, null);
        checkFieldProperties(activity, R.id.saveButton, View.VISIBLE, null);
        checkFieldProperties(activity, R.id.cancelButton, View.VISIBLE, null);
        checkFieldProperties(activity, R.id.barcode, View.VISIBLE, null);

        checkFieldProperties(activity, R.id.barcodeIdLayout, cardIdVisibility, null);
        checkFieldProperties(activity, R.id.barcodeLayout, cardIdVisibility, null);
        checkFieldProperties(activity, R.id.barcodeTypeLayout, View.GONE, null);
    }

    @Test
    public void startWithoutParametersCheckFieldsAvailable()
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardViewActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "");
    }

    @Test
    public void startWithoutParametersCannotCreateGiftCard()
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardViewActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        assertEquals(0, db.getLoyaltyCardCount());

        final EditText storeField = (EditText) activity.findViewById(R.id.storeName);
        final EditText cardIdField = (EditText) activity.findViewById(R.id.cardId);

        final Button saveButton = (Button) activity.findViewById(R.id.saveButton);

        saveButton.performClick();
        assertEquals(0, db.getLoyaltyCardCount());

        storeField.setText("store");
        saveButton.performClick();
        assertEquals(0, db.getLoyaltyCardCount());

        cardIdField.setText("cardId");
        saveButton.performClick();
        assertEquals(0, db.getLoyaltyCardCount());
    }

    @Test
    public void startWithoutParametersCancel()
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardViewActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        final Button cancelButton = (Button) activity.findViewById(R.id.cancelButton);

        assertEquals(false, activity.isFinishing());
        cancelButton.performClick();
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void startWithoutParametersCaptureBarcodeCreateGiftCard() throws IOException
    {
        registerMediaStoreIntentHandler();

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardViewActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "");

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, "", BARCODE_DATA, BARCODE_TYPE);

        // Save and check the gift card
        saveLoyaltyCardWithArguments(activity, "store", BARCODE_DATA, BARCODE_TYPE, true);
    }

    @Test
    public void startWithoutParametersCaptureBarcodeFailure() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardViewActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "");

        // Complete barcode capture in failure
        captureBarcodeWithResult(activity, R.id.captureButton, false);

        checkAllFields(activity, "", "", "");
    }

    @Test
    public void startWithoutParametersCaptureBarcodeCancel() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardViewActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "");

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, "", BARCODE_DATA, BARCODE_TYPE);

        // Cancel the gift card creation
        final Button cancelButton = (Button) activity.findViewById(R.id.cancelButton);
        assertEquals(false, activity.isFinishing());
        cancelButton.performClick();
        assertEquals(true, activity.isFinishing());
    }

    private ActivityController createActivityWithLoyaltyCard()
    {
        Intent intent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putInt("id", 1);
        bundle.putBoolean("update", true);
        intent.putExtras(bundle);

        return Robolectric.buildActivity(LoyaltyCardViewActivity.class).withIntent(intent).create();
    }

    @Test
    public void startWithGiftCardCheckDisplay() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard();
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", BARCODE_DATA, BARCODE_TYPE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, "store", BARCODE_DATA, BARCODE_TYPE);
    }

    @Test
    public void startWithLoyaltyCardWithBarcodeUpdateBarcode() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard();
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, "store", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, "store", BARCODE_DATA, BARCODE_TYPE);
    }

    @Test
    public void startWithGiftCardWithReceiptUpdateReceiptCancel() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard();
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, "store", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, "store", BARCODE_DATA, BARCODE_TYPE);

        // Cancel the gift card creation
        final Button cancelButton = (Button) activity.findViewById(R.id.cancelButton);
        assertEquals(false, activity.isFinishing());
        cancelButton.performClick();
        assertEquals(true, activity.isFinishing());
    }
}
