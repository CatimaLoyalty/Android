package protect.card_locker

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import protect.card_locker.DBHelper.LoyaltyCardArchiveFilter
import kotlin.math.max

class CatimaWidgetRemoteViewsFactory(private var context: Context) :
    RemoteViewsService.RemoteViewsFactory {

    private var mDatabase: SQLiteDatabase = DBHelper(context).readableDatabase

    override fun onCreate() {
        onDataSetChanged()
    }

    private var mCards: ArrayList<LoyaltyCard> = ArrayList()

    override fun onDataSetChanged() {
        val mOrder = Utils.getLoyaltyCardOrder(context);
        val mOrderDirection = Utils.getLoyaltyCardOrderDirection(context);

        val cur = DBHelper.getLoyaltyCardCursor(
            mDatabase,
            "",
            null,
            mOrder,
            mOrderDirection,
            LoyaltyCardArchiveFilter.Unarchived
        )

        mCards.clear()
        if (cur.moveToFirst()) {
            do {
                val item = LoyaltyCard.fromCursor(context, cur)
                mCards.add(item)
            } while (cur.moveToNext())
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        return max(1, mCards.count())
    }

    private fun createRemoteView(item: LoyaltyCard): RemoteViews
    {
        val rv = RemoteViews(context.packageName, R.layout.catima_widget_item).apply {
            val headerColor = Utils.getHeaderColor(context, item)
            val foreground = if (Utils.needsDarkForeground(headerColor)) Color.BLACK else Color.WHITE
            setInt(R.id.item_container, "setBackgroundColor", headerColor)
            val icon = item.getImageThumbnail(context)
            // setImageViewIcon is not supported on Android 5, so force Android 5 down the text path
            if (icon != null && Build.VERSION.SDK_INT >= 23) {
                setInt(R.id.item_container, "setBackgroundColor", foreground)
                setImageViewIcon(R.id.item_image, Icon.createWithBitmap(icon))
                setViewVisibility(R.id.item_text, View.INVISIBLE)
                setViewVisibility(R.id.item_image, View.VISIBLE)
            } else {
                setImageViewBitmap(R.id.item_image, null)
                setTextViewText(R.id.item_text, item.store)
                setViewVisibility(R.id.item_text, View.VISIBLE)
                setViewVisibility(R.id.item_image, View.INVISIBLE)
                setTextColor(
                    R.id.item_text,
                    foreground
                )
            }

            val fillInIntent = Intent().apply {
                putExtra(LoyaltyCardViewActivity.BUNDLE_ID, item.id)
            }

            setOnClickFillInIntent(R.id.item_container, fillInIntent)
        }

        return rv
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (mCards.isEmpty()) {
            return RemoteViews(context.packageName, R.layout.catima_widget_empty)
        }

        val item = mCards[position]
        return createRemoteView(item)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}

class CatimaRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return CatimaWidgetRemoteViewsFactory(applicationContext)
    }
}