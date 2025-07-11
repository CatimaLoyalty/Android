package protect.card_locker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CatimaWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.catima_widget)
            val intent = Intent(context, CatimaRemoteViewsService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            views.setRemoteAdapter(R.id.grid_view, intent)

            val templateIntent =  Intent(context, LoyaltyCardViewActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                templateIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setPendingIntentTemplate(R.id.grid_view, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}