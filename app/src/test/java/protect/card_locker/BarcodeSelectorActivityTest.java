package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class BarcodeSelectorActivityTest {
    @Test
    public void emptyStateTest() {
        ActivityController activityController = Robolectric.buildActivity(BarcodeSelectorActivity.class).create();
        activityController.start();
        activityController.resume();

        Activity activity = (Activity) activityController.get();

        final TextView cardId = activity.findViewById(R.id.cardId);
        final Button noBarcodeButton = activity.findViewById(R.id.noBarcode);

        // No card ID by default
        assertEquals(cardId.getText().toString(), "");

        // Button should be visible but disabled
        assertEquals(View.VISIBLE, noBarcodeButton.getVisibility());
        assertEquals(false, noBarcodeButton.isEnabled());
    }

    @Test
    public void nonEmptyStateTest() throws InterruptedException {
        ActivityController activityController = Robolectric.buildActivity(BarcodeSelectorActivity.class).create();
        activityController.start();
        activityController.resume();

        Activity activity = (Activity) activityController.get();

        final TextView cardId = activity.findViewById(R.id.cardId);
        final Button noBarcodeButton = activity.findViewById(R.id.noBarcode);

        cardId.setText("abcdefg");

        // Run the delayed Handler
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Button should be visible and enabled
        assertEquals(View.VISIBLE, noBarcodeButton.getVisibility());
        assertTrue(noBarcodeButton.isEnabled());

        // Clicking button should create "empty" barcode
        activity.findViewById(R.id.noBarcode).performClick();
        Intent resultIntent = shadowOf(activity).getResultIntent();

        // The BarcodeSelectorActivity should return an empty string
        assertEquals("", resultIntent.getStringExtra(BarcodeSelectorActivity.BARCODE_FORMAT));
        assertEquals("abcdefg", resultIntent.getStringExtra(BarcodeSelectorActivity.BARCODE_CONTENTS));
    }

    @Test
    public void nonEmptyToEmptyStateTest() throws InterruptedException {
        ActivityController activityController = Robolectric.buildActivity(BarcodeSelectorActivity.class).create();
        activityController.start();
        activityController.resume();

        Activity activity = (Activity) activityController.get();

        final TextView cardId = activity.findViewById(R.id.cardId);
        final Button noBarcodeButton = activity.findViewById(R.id.noBarcode);

        cardId.setText("abcdefg");

        // Run the delayed Handler
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Button should be visible and enabled
        assertEquals(View.VISIBLE, noBarcodeButton.getVisibility());
        assertTrue(noBarcodeButton.isEnabled());

        cardId.setText("");

        // Run the delayed Handler
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Button should be visible but disabled
        assertEquals(View.VISIBLE, noBarcodeButton.getVisibility());
        assertFalse(noBarcodeButton.isEnabled());
    }
}
