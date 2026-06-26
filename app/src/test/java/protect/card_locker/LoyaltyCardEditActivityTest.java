package protect.card_locker;

import android.widget.TextView;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.RobolectricTestRunner;

import java.util.Currency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import protect.card_locker.preferences.Settings;

@RunWith(RobolectricTestRunner.class)
public class LoyaltyCardEditActivityTest {

    private ActivityController<LoyaltyCardEditActivity> controller;
    private LoyaltyCardEditActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(LoyaltyCardEditActivity.class);
        activity = controller.get();
    }

    @After
    public void tearDown() {
        if (activity != null && activity.mDatabase != null && activity.mDatabase.isOpen()) {
            activity.mDatabase.close();
        }
    }

    @Test
    public void onCreate_setsBalanceTypeFromSettings_SEK() {
        Currency currency = Currency.getInstance("SEK");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit()
        .putString(
            activity.getString(R.string.settings_key_default_currency),
            currency.getSymbol()
        )
        .commit();

        controller.create();

        assertEquals(currency, activity.viewModel.getLoyaltyCard().balanceType);

        controller.resume();

        TextView field = activity.findViewById(R.id.balanceCurrencyField);
        assertNotNull(field);
        assertEquals(
            currency.getSymbol(),
            field.getText().toString()
        );
    }

    @Test
    public void onCreate_setsBalanceTypeWhenSettingsNull_points() {
        controller.create();

        assertEquals(
            null,
            activity.viewModel.getLoyaltyCard().balanceType
        );

        controller.resume();

        TextView field = activity.findViewById(R.id.balanceCurrencyField);
        assertNotNull(field);
        assertEquals(
            activity.getString(R.string.points),
            field.getText().toString()
        );
    }
}