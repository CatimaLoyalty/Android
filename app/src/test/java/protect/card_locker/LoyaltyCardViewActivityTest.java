package protect.card_locker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;
import static protect.card_locker.LoyaltyCardEditActivity.NO_BARCODE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.widget.TextViewCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Intents;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
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
        final TextView cardIdField = activity.findViewById(R.id.cardIdView);
        final TextView barcodeTypeField = activity.findViewById(R.id.barcodeTypeView);

        storeField.setText(store);
        noteField.setText(note);
        cardIdField.setText(cardId);
        barcodeTypeField.setText(barcodeType);

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(R.id.action_save);
        assertEquals(true, activity.isFinishing());

        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);
        assertEquals(store, card.store);
        assertEquals(note, card.note);
        assertEquals(cardId, card.cardId);

        // The special "No barcode" string shouldn't actually be written to the loyalty card
        if(barcodeType.equals(NO_BARCODE))
        {
            assertEquals("", card.barcodeType);
        }
        else
        {
            assertEquals(barcodeType, card.barcodeType);
        }
        assertNotNull(card.headerColor);
        assertNotNull(card.headerTextColor);
    }

    /**
     * Initiate and complete a barcode capture, either in success
     * or in failure
     */
    private void captureBarcodeWithResult(final Activity activity, final int buttonId, final boolean success) throws IOException
    {
        // Start image capture
        final Button captureButton = activity.findViewById(buttonId);
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
        Bundle resultBundle = new Bundle();
        resultBundle.putString(Intents.Scan.RESULT, BARCODE_DATA);
        resultBundle.putString(Intents.Scan.RESULT_FORMAT, BARCODE_TYPE);
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
    private void selectBarcodeWithResult(final Activity activity, final int buttonId, final String barcodeData, final String barcodeType, final boolean success) throws IOException
    {
        // Start image capture
        final Button captureButton = activity.findViewById(buttonId);
        captureButton.performClick();

        ShadowActivity.IntentForResult intentForResult = shadowOf(activity).peekNextStartedActivityForResult();
        assertNotNull(intentForResult);

        Intent intent = intentForResult.intent;
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
            TextView textView = (TextView)view;
            assertEquals(contents, textView.getText().toString());
        }
    }

    private void checkAllFields(final Activity activity, ViewMode mode,
                                final String store, final String note, final String cardId, final String barcodeType)
    {
        if(mode == ViewMode.VIEW_CARD)
        {
            checkFieldProperties(activity, R.id.cardIdView, View.VISIBLE, cardId);
        }
        else
        {
            int captureVisibility = (mode == ViewMode.UPDATE_CARD || mode == ViewMode.ADD_CARD) ? View.VISIBLE : View.GONE;

            int editVisibility = View.VISIBLE;

            checkFieldProperties(activity, R.id.storeNameEdit, editVisibility, store);
            checkFieldProperties(activity, R.id.noteEdit, editVisibility, note);
            checkFieldProperties(activity, R.id.cardIdView, View.VISIBLE, cardId);
            checkFieldProperties(activity, R.id.cardIdDivider, cardId.isEmpty() ? View.GONE : View.VISIBLE, null);
            checkFieldProperties(activity, R.id.cardIdTableRow, cardId.isEmpty() ? View.GONE : View.VISIBLE, null);
            checkFieldProperties(activity, R.id.barcodeTypeView, View.VISIBLE, barcodeType);
            checkFieldProperties(activity, R.id.captureButton, captureVisibility, null);
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

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", "", "");
        assertEquals(View.GONE, activity.findViewById(R.id.barcodeTypeTableRow).getVisibility());
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
        final TextView cardIdField = activity.findViewById(R.id.cardIdView);

        shadowOf(activity).clickMenuItem(R.id.action_save);
        assertEquals(0, db.getLoyaltyCardCount());

        storeField.setText("store");
        shadowOf(activity).clickMenuItem(R.id.action_save);
        assertEquals(0, db.getLoyaltyCardCount());

        noteField.setText("note");
        shadowOf(activity).clickMenuItem(R.id.action_save);
        assertEquals(0, db.getLoyaltyCardCount());
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

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", "", "");

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", BARCODE_DATA, BARCODE_TYPE);

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", BARCODE_DATA, BARCODE_TYPE, true);
    }

    @Test
    public void startWithoutParametersCaptureBarcodeFailure() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", "", "");

        // Complete barcode capture in failure
        captureBarcodeWithResult(activity, R.id.captureButton, false);

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", "", "");
    }

    @Test
    public void startWithoutParametersCaptureBarcodeCancel() throws IOException
    {
        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class).create();
        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", "", "");

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, ViewMode.ADD_CARD, "", "", BARCODE_DATA, BARCODE_TYPE);

        // Cancel the loyalty card creation
        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
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
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", BARCODE_DATA, BARCODE_TYPE);
    }

    @Test
    public void startWithLoyaltyCardViewModeCheckDisplay() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.VIEW_CARD, "store", "note", BARCODE_DATA, BARCODE_TYPE);
    }

    @Test
    public void startWithLoyaltyCardWithBarcodeUpdateBarcode() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", BARCODE_DATA, BARCODE_TYPE);
    }

    @Test
    public void startWithLoyaltyCardWithReceiptUpdateReceiptCancel() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", EAN_BARCODE_DATA, EAN_BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", EAN_BARCODE_DATA, EAN_BARCODE_TYPE);

        // Complete barcode capture successfully
        captureBarcodeWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", BARCODE_DATA, BARCODE_TYPE);

        // Cancel the loyalty card creation
        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void checkMenu() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The share, settings and add button should be present
        assertEquals(menu.size(), 3);

        assertEquals("Block Rotation", menu.findItem(R.id.action_lock_unlock).getTitle().toString());
        assertEquals("Share", menu.findItem(R.id.action_share).getTitle().toString());
        assertEquals("Edit", menu.findItem(R.id.action_edit).getTitle().toString());
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
        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void startWithoutColors()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, null, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void startLoyaltyCardWithoutColorsSave() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, null, null);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", BARCODE_DATA, BARCODE_TYPE, false);
    }

    @Test
    public void startLoyaltyCardWithExplicitNoBarcodeSave() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", BARCODE_DATA, "", Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // Save and check the loyalty card
        saveLoyaltyCardWithArguments(activity, "store", "note", BARCODE_DATA, NO_BARCODE, false);
    }

    @Test
    public void removeBarcodeFromLoyaltyCard() throws IOException
    {
        ActivityController activityController = createActivityWithLoyaltyCard(true);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        // First check if the card is as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", BARCODE_DATA, BARCODE_TYPE);

        // Complete empty barcode selection successfully
        selectBarcodeWithResult(activity, R.id.enterButton, BARCODE_DATA, "", true);

        // Check if the barcode type is NO_BARCODE as expected
        checkAllFields(activity, ViewMode.UPDATE_CARD, "store", "note", BARCODE_DATA, NO_BARCODE);
        assertEquals(View.GONE, activity.findViewById(R.id.barcodeTypeTableRow).getVisibility());

        // Check if the special NO_BARCODE string doesn't get saved
        saveLoyaltyCardWithArguments(activity, "store", "note", BARCODE_DATA, NO_BARCODE, false);
    }

    @Test
    public void startCheckFontSizes()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

        final int STORE_FONT_SIZE = 50;
        final int CARD_FONT_SIZE = 40;
        final int NOTE_FONT_SIZE = 30;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        settings.edit()
            .putInt(activity.getResources().getString(R.string.settings_key_card_title_font_size), STORE_FONT_SIZE)
            .putInt(activity.getResources().getString(R.string.settings_key_card_id_font_size), CARD_FONT_SIZE)
            .putInt(activity.getResources().getString(R.string.settings_key_card_note_font_size), NOTE_FONT_SIZE)
            .apply();

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        TextView storeName = activity.findViewById(R.id.storeName);
        TextView cardIdFieldView = activity.findViewById(R.id.cardIdView);
        TextView noteView = activity.findViewById(R.id.noteView);

        TextViewCompat.getAutoSizeMaxTextSize(storeName);
        TextViewCompat.getAutoSizeMaxTextSize(storeName);
        assertEquals(STORE_FONT_SIZE, (int)storeName.getTextSize());
        assertEquals(CARD_FONT_SIZE, TextViewCompat.getAutoSizeMaxTextSize(cardIdFieldView));
        assertEquals(NOTE_FONT_SIZE, TextViewCompat.getAutoSizeMaxTextSize(noteView));

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void checkScreenOrientationLockSetting()
    {
        for(boolean locked : new boolean[] {false, true})
        {
            ActivityController activityController = createActivityWithLoyaltyCard(false);

            Activity activity = (Activity)activityController.get();
            DBHelper db = new DBHelper(activity);
            db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

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
        }
    }

    @Test
    public void checkBarcodeFullscreenWorkflow()
    {
        ActivityController activityController = createActivityWithLoyaltyCard(false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        db.insertLoyaltyCard("store", "note", BARCODE_DATA, BARCODE_TYPE, Color.BLACK, Color.WHITE);

        activityController.start();
        activityController.visible();
        activityController.resume();

        assertEquals(false, activity.isFinishing());

        ImageView barcodeImage = activity.findViewById(R.id.barcode);
        View collapsingToolbarLayout = activity.findViewById(R.id.collapsingToolbarLayout);

        // Android should not be in fullscreen mode
        int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);

        // Elements should be visible
        assertEquals(View.VISIBLE, collapsingToolbarLayout.getVisibility());

        // Click barcode to toggle fullscreen
        barcodeImage.performClick();

        // Android should be in fullscreen mode
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);

        // Elements should not be visible
        assertEquals(View.GONE, collapsingToolbarLayout.getVisibility());

        // Clicking barcode again should deactivate fullscreen mode
        barcodeImage.performClick();
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);
        assertEquals(View.VISIBLE, collapsingToolbarLayout.getVisibility());

        // Another click back to fullscreen
        barcodeImage.performClick();
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);
        assertEquals(View.GONE, collapsingToolbarLayout.getVisibility());

        // In full screen mode, back button should disable fullscreen
        activity.onBackPressed();
        uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, uiOptions);
        assertNotEquals(uiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN, uiOptions);
        assertEquals(View.VISIBLE, collapsingToolbarLayout.getVisibility());

        // Pressing back when not in full screen should finish activity
        activity.onBackPressed();
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void importCard()
    {
        Uri importUri = Uri.parse("https://brarcher.github.io/loyalty-card-locker/share?store=Example%20Store&note=&cardid=123456&barcodetype=AZTEC&headercolor=-416706&headertextcolor=-1");

        Intent intent = new Intent();
        intent.setData(importUri);

        ActivityController activityController = Robolectric.buildActivity(LoyaltyCardEditActivity.class, intent).create();

        activityController.start();
        activityController.visible();
        activityController.resume();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, ViewMode.ADD_CARD, "Example Store", "", "123456", "AZTEC");
        assertEquals(-416706, ((ColorDrawable) activity.findViewById(R.id.headingColorSample).getBackground()).getColor());
        assertEquals(-1, ((ColorDrawable) activity.findViewById(R.id.headingStoreTextColorSample).getBackground()).getColor());
    }
}
