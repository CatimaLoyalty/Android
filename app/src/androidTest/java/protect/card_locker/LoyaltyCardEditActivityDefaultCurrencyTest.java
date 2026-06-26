package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Rule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;


import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4.class)
public class LoyaltyCardEditActivityDefaultCurrencyTest {

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA);

    @Test
    public void defaultCurrencyPreselected() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.settings_key_default_currency);
        prefs.edit().putString(key, "USD").commit();

        try (ActivityScenario<LoyaltyCardEditActivity> scenario = ActivityScenario.launch(LoyaltyCardEditActivity.class)) {
            onView(withText(R.string.options)).perform(click());
            onView(withId(R.id.balanceCurrencyField))
                .check(matches(withText("$")));
        }
    }

    @Test
    public void manualCurrencyPersistsAfterSave() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.settings_key_default_currency);
        prefs.edit().putString(key, "USD").commit();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
        
            onView(withId(R.id.fabAdd)).perform(click());

            // handle if camera option shows up
            try {
                onView(withText("Got It")).perform(click());
            } catch (Exception e) { /* already clicked or never showed */ }

            onView(withText("More options")).perform(click());
            onView(withText("Add a card with no barcode")).perform(click());
            onView(withClassName(endsWith("EditText"))).perform(typeText("123"), closeSoftKeyboard());
            onView(withText("OK")).perform(click());
            onView(withId(R.id.storeNameEdit)).perform(typeText("card name 123"), closeSoftKeyboard());
            onView(withText(R.string.options)).perform(click());
            onView(withId(R.id.balanceCurrencyField))
                .check(matches(withText("$")));
            onView(withId(R.id.balanceCurrencyField)).perform(replaceText("£"), closeSoftKeyboard());
            onView(withId(R.id.fabSave)).perform(click());
            onView(withText("mr no hands")).perform(click());
            onView(withId(R.id.fabEdit)).perform(click());
            onView(withText(R.string.options)).perform(click());
            onView(withId(R.id.balanceCurrencyField))
                .check(matches(withText("£")));
        }
    }

}