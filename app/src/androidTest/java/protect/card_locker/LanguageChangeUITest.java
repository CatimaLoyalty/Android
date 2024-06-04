package protect.card_locker;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class LanguageChangeUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void languageChangeUITest() {
        ViewInteraction overflowMenuButton = onView(
                allOf(withContentDescription(R.string.action_more_options),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        String expectedText = getInstrumentation().getTargetContext().getString(R.string.settings);
        ViewInteraction materialTextView = onView(
                allOf(withId(androidx.recyclerview.R.id.title), withText(expectedText),
                        childAtPosition(
                                childAtPosition(
                                        withId(androidx.appcompat.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        materialTextView.perform(click());

        ViewInteraction recyclerView = onView(
                allOf(withId(com.jaredrummler.android.colorpicker.R.id.recycler_view),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView.perform(actionOnItemAtPosition(4, click()));

        String selectedLanguage = "de";
        int position = getPositionOfItem(selectedLanguage);

        DataInteraction appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(androidx.appcompat.R.id.select_dialog_listview),
                        childAtPosition(
                                withId(androidx.appcompat.R.id.contentPanel),
                                0)))
                .atPosition(position);
        appCompatCheckedTextView.perform(click());

        ViewInteraction textView = onView(
                allOf(withText("Einstellungen"),
                        withParent(allOf(withId(R.id.toolbar),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class)))),
                        isDisplayed()));
        textView.check(matches(withText("Einstellungen")));

        ViewInteraction recyclerView2 = onView(
                allOf(withId(com.jaredrummler.android.colorpicker.R.id.recycler_view),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView2.perform(actionOnItemAtPosition(4, click()));

        selectedLanguage = "el-rGR";
        position = getPositionOfItem(selectedLanguage);

        DataInteraction appCompatCheckedTextView2 = onData(anything())
                .inAdapterView(allOf(withId(androidx.appcompat.R.id.select_dialog_listview),
                        childAtPosition(
                                withId(androidx.appcompat.R.id.contentPanel),
                                0)))
                .atPosition(position);
        appCompatCheckedTextView2.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withText("Ρυθμίσεις"),
                        withParent(allOf(withId(R.id.toolbar),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class)))),
                        isDisplayed()));
        textView2.check(matches(withText("Ρυθμίσεις")));

        ViewInteraction recyclerView3 = onView(
                allOf(withId(com.jaredrummler.android.colorpicker.R.id.recycler_view),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView3.perform(actionOnItemAtPosition(4, click()));

        DataInteraction appCompatCheckedTextView3 = onData(anything())
                .inAdapterView(allOf(withId(androidx.appcompat.R.id.select_dialog_listview),
                        childAtPosition(
                                withId(androidx.appcompat.R.id.contentPanel),
                                0)))
                .atPosition(0);
        appCompatCheckedTextView3.perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withClassName(is("com.google.android.material.appbar.AppBarLayout")),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.welcome_text), withText("Welcome to Catima"),
                        withParent(allOf(withId(R.id.helpSection),
                                withParent(withId(R.id.include)))),
                        isDisplayed()));
        textView3.check(matches(withText("Welcome to Catima")));
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    private static int getPositionOfItem(String selectedLanguage) {
        String[] availableLocales = getInstrumentation().getTargetContext().getResources().getStringArray(R.array.locale_values);
        int position = 0;
        for (String locale : availableLocales) {
            if (locale.equals(selectedLanguage)) {
                break;
            }
            position++;
        }
        return position;
    }
}
