package protect.card_locker.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import protect.card_locker.DBHelper;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.LoyaltyCardViewActivity;
import protect.card_locker.R;

public class CardAppWidgetProvider extends AppWidgetProvider
{
    private static final String TAG = "LoyaltyCardLocker";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        Log.d(TAG, "CardAppWidgetProvider onUpdate");
        // For each widget that needs an update, get the text that we should display:
        //   - Create a RemoteViews object for it
        //   - Set the text in the RemoteViews object
        //   - Tell the AppWidgetManager to show that views object for the widget.
        for (int appWidgetId : appWidgetIds)
        {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId);

        LoyaltyCard card = null;
        DBHelper db = new DBHelper(context);

        Integer id = CardAppWidgetConfigure.loadIdPref(context, appWidgetId);
        if(id != null)
        {
            Log.d(TAG, "updateAppWidget Retrieved id " + id);
            card = db.getLoyaltyCard(id);
        }

        if(card != null)
        {
            Log.d(TAG, "updateAppWidget Updating widget  " + appWidgetId + " to load " + card.store);

            // Construct the RemoteViews object.  It takes the package name (in our case, it's our
            // package, but it needs this because on the other side it's the widget host inflating
            // the layout from our package).
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_provider);
            views.setTextViewText(R.id.title, card.store);

            // Launch the view activity when clicked
            Intent intent = new Intent(context, LoyaltyCardViewActivity.class);
            Bundle extras = new Bundle();
            extras.putInt("id", id);
            extras.putBoolean("view", true);
            intent.putExtras(extras);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the widget manager
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        else
        {
            Log.d(TAG, "updateAppWidget, no card ID associated with widget " + appWidgetId
                + ", ignoring update");
        }
    }
}
