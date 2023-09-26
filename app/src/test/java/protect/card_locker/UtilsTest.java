package protect.card_locker;

import static org.junit.Assert.assertFalse;

import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.test.core.app.ActivityScenario;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


@RunWith(RobolectricTestRunner.class)
public class UtilsTest {
    /**
     * Ensure all the default card colours (used when a user has not chosen a card colour) use white foreground text
     */
    @Test
    public void allDefaultCardColoursHaveWhiteForegroundTest() {
        try(ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TypedArray colors = activity.getApplicationContext().getResources().obtainTypedArray(R.array.letter_tile_colors);

                for (int i = 0; i < colors.length(); i++) {
                    // Grab white as fallback so that if the retrieval somehow fails the test is guaranteed to fail because a white background will have black foreground
                    int color = colors.getColor(i, Color.WHITE);
                    assertFalse(Utils.needsDarkForeground(color));
                }
            });
        }
    }
}
