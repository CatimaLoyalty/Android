package protect.card_locker.screenshot;

import android.support.test.rule.ActivityTestRule;

import androidx.test.core.app.ActivityScenario;
import androidx.test.runner.AndroidJUnit4;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import protect.card_locker.MainActivity;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@RunWith(AndroidJUnit4.class)
public class ScreenshotCreation {
    @ClassRule
    public static final TestRule classRule = new LocaleTestRule();

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void takeScreenshot() throws Exception {
        Screengrab.screenshot("mainactivity");
    }
}
