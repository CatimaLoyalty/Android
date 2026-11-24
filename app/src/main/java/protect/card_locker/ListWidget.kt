package protect.card_locker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import protect.card_locker.DBHelper.LoyaltyCardArchiveFilter
import protect.card_locker.cardview.LoyaltyCardViewActivity
import protect.card_locker.shared.ForegroundColorHelper
import androidx.core.graphics.scale

private const val TAG = "catima.listwidget"

private const val FALLBACK_MAX_BYTES = 15_000_000L
private const val ITEM_CLICK = "listwidget.ACTION_WIDGET_ITEM_CLICK"

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

    // Taken from the official Android documentation: https://developer.android.com/about/versions/17/behavior-changes-17#widget-memory-mitigation
    fun calculateWidgetMemoryCapBytes(context: Context): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(WindowManager::class.java)
                ?: return FALLBACK_MAX_BYTES
            val bounds = wm.currentWindowMetrics.bounds
            return 6L * bounds.width().toLong() * bounds.height().toLong()
        }
        val dm = context.resources.displayMetrics
        return 6L * dm.widthPixels.toLong() * dm.heightPixels.toLong()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent?.action == "${context?.packageName}.${ITEM_CLICK}") {
            val bundleId = intent.getIntExtra(LoyaltyCardViewActivity.BUNDLE_ID, -1)
            if (bundleId != -1) {
                context!!.startActivity(
                    Intent(context, LoyaltyCardViewActivity::class.java).apply {
                        putExtras(Bundle().apply {
                            putInt(LoyaltyCardViewActivity.BUNDLE_ID, bundleId)
                        })
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            } else {
                context!!.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val database = DBHelper(context).readableDatabase

            // Get cards
            val order = Utils.getLoyaltyCardOrder(context);
            val orderDirection = Utils.getLoyaltyCardOrderDirection(context);

            val loyaltyCardCursor = DBHelper.getLoyaltyCardCursor(
                database,
                "",
                null,
                order,
                orderDirection,
                LoyaltyCardArchiveFilter.Unarchived
            )

            // Loop over cards to get count for scaling the bitmaps
            // We may need to scale the bitmaps to stay within the max memory size limitations added in Android 17
            // Add at most 26 cards and mark if we have more. If we have more, add a fake "Show more" card
            // This amount is chosen because a fullscreen widget on the Fairphone 6 would fit 27 cards (26 cards + 1 fake one) at most
            val loyaltyCards = mutableListOf<LoyaltyCard>()
            var tooManyLoyaltyCards = false
            if (loyaltyCardCursor.moveToFirst()) {
                do {
                    if (loyaltyCards.size >= 26) {
                        tooManyLoyaltyCards = true
                        break
                    } else {
                        loyaltyCards.add(LoyaltyCard.fromCursor(context, loyaltyCardCursor))
                    }
                } while (loyaltyCardCursor.moveToNext())
            }
            loyaltyCardCursor.close()

            // Calculate max bitmap size per card
            val bitmapSizePerCard = calculateWidgetMemoryCapBytes(context) / loyaltyCards.size;

            // Bind every card to cell in the grid
            val remoteCollectionItemsBuilder = RemoteViewsCompat.RemoteCollectionItems.Builder()
            for (loyaltyCard in loyaltyCards) {
                remoteCollectionItemsBuilder.addItem(
                    loyaltyCard.id.toLong(),
                    createRemoteViews(
                        context, loyaltyCard, bitmapSizePerCard, true
                    )
                )
            }

            // Add a fake card if too many cards
            if (tooManyLoyaltyCards) {
                remoteCollectionItemsBuilder.addItem(
                    -1,
                    createRemoteViews(
                        context,
                        LoyaltyCard().apply {
                            store = context.getString(R.string.view_more)
                        },
                        bitmapSizePerCard,
                        false
                    )
                )
            }

            // Create the base empty view
            var views = RemoteViews(context.packageName, R.layout.list_widget_empty)

            if (loyaltyCards.isNotEmpty()) {
                // If we have cards, create the list
                views = RemoteViews(context.packageName, R.layout.list_widget)
                val templateIntent = Intent(context, ListWidget::class.java).apply {
                    action = "${context.packageName}.${ITEM_CLICK}"
                }
                val pendingIntent = PendingIntent.getBroadcast(
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

    private fun tryMakeBitmapFit(bitmap: Bitmap?, maxBitmapSize: Long): Bitmap? {
        if (bitmap == null) {
            return null
        }

        if (bitmap.allocationByteCount <= maxBitmapSize) {
            return bitmap;
        }

        // To calculate the size needed, we figure out how many bytes we currently use compared to how much we are allowed to use
        // Then, we add a 10% margin to be safe
        // The .toDouble() cast is to prevent Kotlin from rounding this to a full digit (for example: 1.03 -> 1)
        val scaleNeeded = (bitmap.allocationByteCount.toDouble() / maxBitmapSize) * 1.1

        val newWidth = (bitmap.width / scaleNeeded).toInt()
        val newHeight = (bitmap.height / scaleNeeded).toInt()

        if (newWidth <= 0 || newHeight <= 0) {
            Log.w(TAG, "Tried to resize bitmap to impossibly small size to fit, returning nothing")
            return null
        }

        val output = bitmap.scale(newWidth, newHeight)

        Log.d(TAG, "Resized ${bitmap.width}x${bitmap.height} bitmap of ${bitmap.allocationByteCount} by $scaleNeeded to fit within ${maxBitmapSize}, ended up with ${output.width}x${output.height} bitmap of ${output.allocationByteCount}")

        return output
    }

    private fun createRemoteViews(context: Context, loyaltyCard: LoyaltyCard, maxBitmapSize: Long, isRealCard: Boolean): RemoteViews {
        // Create a single cell for the grid view, bind it to open in the LoyaltyCardViewActivity
        // Note: Android 5 will not use bitmaps
        val remoteViews = RemoteViews(context.packageName, R.layout.list_widget_item).apply {
            val headerColor = Utils.getHeaderColor(context, loyaltyCard)
            val foreground = if (ForegroundColorHelper.needsDarkForeground(headerColor)) Color.BLACK else Color.WHITE
            setInt(R.id.item_container_foreground, "setBackgroundColor", headerColor)
            val icon = tryMakeBitmapFit(loyaltyCard.getImageThumbnail(context), maxBitmapSize)
            // FIXME: The icon flow causes a crash up to Android 12L, so force anything below 33 down this path
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
            val fillInIntent = Intent()
            if (isRealCard) {
                fillInIntent.apply {
                    putExtra(LoyaltyCardViewActivity.BUNDLE_ID, loyaltyCard.id)
                }
            }

            setOnClickFillInIntent(R.id.item_container, fillInIntent)
        }

        return remoteViews
    }
}
