package protect.card_locker.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import protect.card_locker.DBHelper;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.LoyaltyCardCursorAdapter;
import protect.card_locker.R;

/**
 * The configuration screen for the CardAppWidgetProvider widget.
 */
public class CardAppWidgetConfigure extends AppCompatActivity
{
    static final String TAG = "LoyaltyCardLocker";

    private static final String PREFS_NAME
            = "protect.card_locker.appwidget.CardAppWidgetProvider";
    private static final String PREF_PREFIX_KEY = "prefix_";

    int appWidgetId_ = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setVisibility(View.GONE);

        setTitle(R.string.selectCardTitle);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            appWidgetId_ = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId_ == AppWidgetManager.INVALID_APPWIDGET_ID)
        {
            finish();
        }

        final DBHelper db = new DBHelper(this);

        // If there are no cards, bail
        if(db.getLoyaltyCardCount() == 0)
        {
            finish();
        }

        final ListView cardList = (ListView) findViewById(R.id.list);
        cardList.setVisibility(View.VISIBLE);

        Cursor cardCursor = db.getLoyaltyCardCursor();

        final LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(this, cardCursor);
        cardList.setAdapter(adapter);

        cardList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Context context = CardAppWidgetConfigure.this;
                Cursor selected = (Cursor) parent.getItemAtPosition(position);
                LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(selected);

                Log.d(TAG, "Saving card " + loyaltyCard.store + "," + loyaltyCard.id + " at " + appWidgetId_);

                // Save the association of the card to the widget
                saveIdPref(context, appWidgetId_, loyaltyCard.id);

                // Push widget update to surface with newly set association
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                CardAppWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId_);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId_);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveIdPref(Context context, int appWidgetId, int id)
    {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId, id);
        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static Integer loadIdPref(Context context, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int id = prefs.getInt(PREF_PREFIX_KEY + appWidgetId, -1);
        if(id >= 0)
        {
            return id;
        }
        else
        {
            return null;
        }
    }
}
