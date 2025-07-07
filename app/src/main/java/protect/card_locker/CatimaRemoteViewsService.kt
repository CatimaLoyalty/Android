package protect.card_locker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RequiresApi
import protect.card_locker.DBHelper.LoyaltyCardArchiveFilter
import kotlin.math.max



class CatimaWidgetRemoteViewsFactory(private var context: Context) :
    RemoteViewsService.RemoteViewsFactory {

    private var mDatabase: SQLiteDatabase = DBHelper(context).readableDatabase

    override fun onCreate() {
        onDataSetChanged()
    }

    private var mCards: ArrayList<LoyaltyCard> = ArrayList<LoyaltyCard>()
    private var mOrderDirection = DBHelper.LoyaltyCardOrderDirection.Ascending
    private var mOrder =  DBHelper.LoyaltyCardOrder.LastUsed

    override fun onDataSetChanged() {
        val sortPref: SharedPreferences = context.getSharedPreferences(
            "sharedpreference_sort",
            Context.MODE_PRIVATE
        )

        val orderString = sortPref.getString("sharedpreference_sort_order", null)
        val orderDirectionString =
            sortPref.getString("sharedpreference_sort_direction", null)

        if (orderString != null && orderDirectionString != null) {
            try {
                mOrder = DBHelper.LoyaltyCardOrder.valueOf(orderString)
                mOrderDirection = DBHelper.LoyaltyCardOrderDirection.valueOf(orderDirectionString)
            } catch (ignored: IllegalArgumentException) {
            }
        }

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

    @RequiresApi(23)
    private fun createRemoteView(item: LoyaltyCard): RemoteViews
    {
        val rv = RemoteViews(context.packageName, R.layout.catima_widget_item
        ).apply {
            val backColor = Utils.getHeaderColor(context, item)
            setInt(R.id.item_container, "setBackgroundColor", backColor)
            val icon = item.getImageThumbnail(context)
            if (icon != null) {
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
                    if (Utils.needsDarkForeground(backColor)) Color.BLACK else Color.WHITE
                )
            }

            val fillInIntent = Intent().apply {
                putExtra(LoyaltyCardViewActivity.BUNDLE_ID, item.id)
            }

            setOnClickFillInIntent(R.id.item_container, fillInIntent)
        }

        return rv
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun getViewAt(position: Int): RemoteViews {
        if (mCards.isEmpty()) {
            return RemoteViews(context.packageName, R.layout.catima_widget_item).apply {
                setImageViewBitmap(R.id.item_image, null)
                setTextViewText(R.id.item_text, context.getString(R.string.no_loyalty_cards))
                setTextViewTextSize(R.id.item_text, TypedValue.COMPLEX_UNIT_DIP, 25F);
                setViewVisibility(R.id.item_text, View.VISIBLE)
                setViewVisibility(R.id.item_image, View.INVISIBLE)
                setInt(R.id.item_container, "setBackgroundColor",  Color.WHITE)
                setTextColor(
                    R.id.item_text,
                    Color.BLACK
                )

            }
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