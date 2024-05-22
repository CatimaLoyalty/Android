package protect.card_locker;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class LoyaltyCardCreationTest {
    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.CAMERA",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.READ_EXTERNAL_STORAGE");

    @Test
    public void loyaltyCardCreationTest() {
        ViewInteraction textView = onView(
                allOf(withId(R.id.add_card_instruction), withText("Click the + plus button to add a card, or import from the ⋮ menu."),
                        withParent(allOf(withId(R.id.helpSection),
                                withParent(withId(R.id.include)))),
                        isDisplayed()));
        textView.check(matches(withText("Click the + plus button to add a card, or import from the ⋮ menu.")));

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.fabAdd), withContentDescription("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        ViewInteraction extendedFloatingActionButton = onView(
                allOf(withId(R.id.fabOtherOptions), withText("More options"),
                        childAtPosition(
                                allOf(withId(R.id.zxing_barcode_scanner),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                1)),
                                0),
                        isDisplayed()));
        extendedFloatingActionButton.perform(click());

        DataInteraction materialTextView = onData(anything())
                .inAdapterView(allOf(withId(androidx.appcompat.R.id.select_dialog_listview),
                        childAtPosition(
                                withId(androidx.appcompat.R.id.contentPanel),
                                0)))
                .atPosition(0);
        materialTextView.perform(click());

        ViewInteraction editText = onView(
                allOf(childAtPosition(
                                childAtPosition(
                                        withId(androidx.appcompat.R.id.custom),
                                        0),
                                1),
                        isDisplayed()));
        editText.perform(replaceText("123456789"), closeSoftKeyboard());

        ViewInteraction materialButton = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(androidx.appcompat.R.id.buttonPanel),
                                        0),
                                3)));
        materialButton.perform(scrollTo(), click());

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.cardIdView), withText("123456789"),
                        withParent(withParent(withId(R.id.cardIdField))),
                        isDisplayed()));
        editText2.check(matches(withText("123456789")));

        ViewInteraction textInputEditText = onView(
                allOf(withId(R.id.storeNameEdit),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.storeNameField),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("CatimaUITestCard"), closeSoftKeyboard());

        ViewInteraction tabView = onView(
                allOf(withContentDescription("Options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.tabs),
                                        0),
                                1),
                        isDisplayed()));
        tabView.perform(click());

        ViewInteraction textInputEditText2 = onView(
                allOf(withId(R.id.balanceField), withText("0"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.balanceView),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText2.perform(replaceText("10000"));

        ViewInteraction textInputEditText3 = onView(
                allOf(withId(R.id.balanceField), withText("10000"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.balanceView),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText3.perform(closeSoftKeyboard());

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.fabSave), withContentDescription("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton2.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.balance), withText("10000 points"),
                        withParent(withParent(withId(R.id.row))),
                        isDisplayed()));
        textView2.check(matches(withText("10000 points")));
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
}