package protect.card_locker;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import androidx.appcompat.widget.Toolbar;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivitySearchViewTest {

    @Test
    public void whenSearchViewIsExpandedAndBackIsPressedThenMenuItemShouldNotBeCollapsed() {
        String query = "random arbitrary text";
        try (ActivityScenario<MainActivity> mainActivityScenario = ActivityScenario.launch(MainActivity.class)) {
            mainActivityScenario.onActivity(this::makeSearchMenuItemVisible);
            onView(withId(R.id.action_search)).perform(click());
            onView(withId(androidx.appcompat.R.id.search_src_text)).perform(typeText(query));

            pressBack();

            onView(withId(androidx.appcompat.R.id.search_src_text)).check(matches(withText(query)));
            mainActivityScenario.onActivity(activity -> assertEquals(query, activity.mFilter));
        }
    }

    @Test
    public void whenSearchViewIsExpandedThenItShouldOnlyBeCollapsedWhenBackIsPressedTwice() {
        try (ActivityScenario<MainActivity> mainActivityScenario = ActivityScenario.launch(MainActivity.class)) {
            mainActivityScenario.onActivity(this::makeSearchMenuItemVisible);
            onView(withId(R.id.action_search)).perform(click());

            pressBack();

            onView(withId(androidx.appcompat.R.id.search_src_text)).check(matches(isDisplayed()));

            pressBack();

            onView(withId(android.R.id.content)).check(matches(is(not(withChild(withId(androidx.appcompat.R.id.search_src_text))))));
        }
    }

    private void makeSearchMenuItemVisible(MainActivity activity) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.getMenu().findItem(R.id.action_search).setVisible(true);
    }

    private void pressBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();
    }

}
