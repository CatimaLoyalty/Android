package protect.card_locker;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;


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
