package protect.card_locker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import protect.card_locker.cardview.LoyaltyCardViewActivity
import protect.card_locker.preferences.Settings
import java.nio.charset.Charset

class BarcodeWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cardId = prefs.getInt(PREFS_CARD_ID_PREFIX + appWidgetId, -1)

        if (cardId == -1) {
            val views = createEmptyViews(context)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val db = DBHelper(context).readableDatabase
        val card = DBHelper.getLoyaltyCard(context, db, cardId)

        val barcodeType = card?.barcodeType
        val cardIdStr = card?.cardId

        if (card == null || barcodeType == null || cardIdStr.isNullOrEmpty()) {
            val views = createEmptyViews(context)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val showFormat = Settings(context).showBarcodeWidgetFormat()

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val isVertical =
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) >
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)

        var barcodeBitmap = generateBarcode(cardIdStr, barcodeType, card.barcodeEncoding)

        if (barcodeBitmap != null && isVertical && !barcodeType.isSquare()) {
            barcodeBitmap = rotateBitmap(barcodeBitmap, 90f)
        }

        val views = RemoteViews(context.packageName, R.layout.barcode_widget)
        views.setTextViewText(R.id.store_name, card.store)

        if (barcodeBitmap != null) {
            views.setImageViewBitmap(R.id.barcode_image, barcodeBitmap)
            views.setViewVisibility(R.id.barcode_image, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.barcode_image, View.GONE)
        }

        if (showFormat) {
            views.setTextViewText(R.id.barcode_format, barcodeType.prettyName())
            views.setViewVisibility(R.id.barcode_format, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.barcode_format, View.GONE)
        }

        val cardIntent =
            Intent(context, LoyaltyCardViewActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(LoyaltyCardViewActivity.BUNDLE_ID, card.id)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                cardIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createEmptyViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.barcode_widget)
        views.setTextViewText(
            R.id.store_name,
            context.getString(R.string.barcode_widget_not_configured),
        )
        views.setViewVisibility(R.id.barcode_image, View.GONE)
        views.setViewVisibility(R.id.barcode_format, View.GONE)
        return views
    }

    private fun generateBarcode(
        cardId: String,
        format: CatimaBarcode,
        encoding: Charset,
    ): Bitmap? {
        if (cardId.isEmpty()) return null

        val isSquare = format.isSquare()
        val genWidth = if (isSquare) 500 else 1000
        val genHeight = if (isSquare) 500 else 300

        return try {
            val writer = MultiFormatWriter()

            val encodeHints = mutableMapOf<EncodeHintType, Any>()
            if (encoding.name() != "ISO-8859-1") {
                encodeHints[EncodeHintType.CHARACTER_SET] = encoding.name()
            }

            val bitMatrix =
                try {
                    if (encodeHints.isNotEmpty()) {
                        writer.encode(cardId, format.format(), genWidth, genHeight, encodeHints)
                    } else {
                        writer.encode(cardId, format.format(), genWidth, genHeight)
                    }
                } catch (e: WriterException) {
                    return null
                } catch (e: Exception) {
                    return null
                }

            bitmapFromBitMatrix(bitMatrix)
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        degrees: Float,
    ): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun bitmapFromBitMatrix(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    companion object {
        private const val PREFS_NAME = "barcode_widget_prefs"
        private const val PREFS_CARD_ID_PREFIX = "card_id_"

        fun saveCardPref(
            context: Context,
            appWidgetId: Int,
            cardId: Int,
        ) {
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(PREFS_CARD_ID_PREFIX + appWidgetId, cardId)
                .apply()
        }

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BarcodeWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                BarcodeWidget().onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }
}
