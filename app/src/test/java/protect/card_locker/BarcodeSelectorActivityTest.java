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
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;


@RunWith(RobolectricTestRunner.class)
public class BarcodeSelectorActivityTest {
    @Test
    public void emptyStateTest() {
        ActivityController activityController = Robolectric.buildActivity(BarcodeSelectorActivity.class).create();
        activityController.start();
        activityController.resume();

        Activity activity = (Activity) activityController.get();

        final TextView cardId = activity.findViewById(R.id.cardId);

        // No card ID by default
        assertEquals(cardId.getText().toString(), "");
    }
}
