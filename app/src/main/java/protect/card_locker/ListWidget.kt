package protect.card_locker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import protect.card_locker.core.WidgetSettings

class ListWidget : AppWidgetProvider() {
    fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ListWidget::class.java)
        onUpdate(
            context,
            appWidgetManager,
            appWidgetManager.getAppWidgetIds(componentName)
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            GlobalScope.launch(Dispatchers.IO) {
                val settingsManager = LoyaltyCardLockerApplication.settingsManager
                val settings = settingsManager.settingsFlow.first()

                val remoteCollectionItemsBuilder = RemoteViewsCompat.RemoteCollectionItems.Builder()
                    .setHasStableIds(true)
                    .setViewTypeCount(1)

                // Load the cards
                val allCards = loadAndFilterCards(context, settings)

                val hasCards = allCards.isNotEmpty()

                if (hasCards) {
                    allCards.forEach { card ->
                        val itemView = createRemoteViews(context, card)
                        remoteCollectionItemsBuilder.addItem(card.id.toLong(), itemView)
                    }
                }

                val views = RemoteViews(context.packageName, R.layout.list_widget)
                views.setEmptyView(R.id.grid_view, R.id.no_cards_view)

                if (hasCards) {
                    // If we have cards, create the list
                    val templateIntent = Intent(context, LoyaltyCardViewActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        templateIntent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    views.setPendingIntentTemplate(R.id.grid_view, pendingIntent)

                    RemoteViewsCompat.setRemoteAdapter(
                        context,
                        views,
                        appWidgetId,
                        R.id.grid_view,
                        remoteCollectionItemsBuilder.build()
                    )
                }

                // Let Android know the widget is ready for display
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    internal fun loadAndFilterCards(context: Context, settings: WidgetSettings): List<LoyaltyCard> {
        val database = DBHelper(context).readableDatabase
        val cardsToShow = mutableListOf<LoyaltyCard>()

        val groups = DBHelper.getGroups(database)

        Log.d("ListWidget", "settings.group: ${settings.group}")
        val matchedGroup = if (settings.group.isNullOrEmpty()) null
        else groups.firstOrNull { group -> group._id == settings.group }
        Log.d("ListWidget", "matchedGroup: ${matchedGroup?._id}")
        return DBHelper.getLoyaltyCardCursor(
            database,
            "",
            matchedGroup,
            settings.sortOrder,
            settings.sortOrderDirection,
            settings.archiveFilter,
            settings.starFilter,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                cardsToShow.add(LoyaltyCard.fromCursor(context, cursor))
            }
            cardsToShow
        }
    }

    private fun createRemoteViews(context: Context, loyaltyCard: LoyaltyCard): RemoteViews {
        // Create a single cell for the grid view, bind it to open in the LoyaltyCardViewActivity
        // Note: Android 5 will not use bitmaps
        val remoteViews = RemoteViews(context.packageName, R.layout.list_widget_item).apply {
            val headerColor = Utils.getHeaderColor(context, loyaltyCard)
            val foreground = if (Utils.needsDarkForeground(headerColor)) Color.BLACK else Color.WHITE
            setInt(R.id.item_container_foreground, "setBackgroundColor", headerColor)
            val icon = loyaltyCard.getImageThumbnail(context)
            // setImageViewIcon is not supported on Android 5, so force Android 5 down the text path
            // FIXME: The icon flow causes a crash up to Android 12L, so SDK_INT is forced up from 23 to 33
            if (icon != null && Build.VERSION.SDK_INT >= 32) {
                setInt(R.id.item_container_foreground, "setBackgroundColor", foreground)
                setImageViewIcon(R.id.item_image, Icon.createWithBitmap(icon))
                setViewVisibility(R.id.item_text, View.INVISIBLE)
                setViewVisibility(R.id.item_image, View.VISIBLE)
            } else {
                setImageViewBitmap(R.id.item_image, null)
                setTextViewText(R.id.item_text, loyaltyCard.store)
                setViewVisibility(R.id.item_text, View.VISIBLE)
                setViewVisibility(R.id.item_image, View.INVISIBLE)
                setTextColor(
                    R.id.item_text,
                    foreground
                )
            }

            // Add the card ID to the intent template
            val fillInIntent = Intent().apply {
                putExtra(LoyaltyCardViewActivity.BUNDLE_ID, loyaltyCard.id)
            }

            setOnClickFillInIntent(R.id.item_container, fillInIntent)
        }

        return remoteViews
    }
}