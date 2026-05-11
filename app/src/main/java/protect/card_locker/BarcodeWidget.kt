package protect.card_locker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.nio.charset.Charset

class BarcodeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
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

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120)

        val barcodeBitmap = generateBarcode(
            context,
            cardIdStr,
            barcodeType,
            card.barcodeEncoding,
            minWidth,
            minHeight
        )

        val views = RemoteViews(context.packageName, R.layout.barcode_widget)
        views.setTextViewText(R.id.store_name, card.store)

        if (barcodeBitmap != null) {
            views.setImageViewBitmap(R.id.barcode_image, barcodeBitmap)
            views.setViewVisibility(R.id.barcode_image, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.barcode_image, View.GONE)
        }

        views.setTextViewText(
            R.id.barcode_format,
            barcodeType.prettyName()
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createEmptyViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.barcode_widget)
        views.setTextViewText(
            R.id.store_name,
            context.getString(R.string.barcode_widget_not_configured)
        )
        views.setViewVisibility(R.id.barcode_image, View.GONE)
        views.setTextViewText(R.id.barcode_format, "")
        return views
    }

    private fun generateBarcode(
        context: Context,
        cardId: String,
        format: CatimaBarcode,
        encoding: Charset,
        widgetWidthDp: Int,
        widgetHeightDp: Int
    ): Bitmap? {
        if (cardId.isEmpty()) return null

        val metrics = context.resources.displayMetrics
        val density = metrics.density

        val widthPx = (widgetWidthDp * density).toInt()
        val heightPx = (widgetHeightDp * density).toInt()

        val textAreaPx = (40 * density).toInt()
        val paddingPx = (16 * density).toInt()

        val barcodeWidth = widthPx - paddingPx
        val barcodeHeight = heightPx - textAreaPx

        if (barcodeWidth <= 0 || barcodeHeight <= 0) return null

        val maxWidth = if (format.isSquare()) 500 else 1500
        val finalWidth = minOf(barcodeWidth, maxWidth)
        val finalHeight = if (format.isSquare()) finalWidth else minOf(barcodeHeight, 500)

        return try {
            val writer = MultiFormatWriter()

            val encodeHints = mutableMapOf<EncodeHintType, Any>()
            // Only pass encoding hint if not ISO-8859-1, to avoid ECI issues
            if (encoding.name() != "ISO-8859-1") {
                encodeHints[EncodeHintType.CHARACTER_SET] = encoding.name()
            }

            val bitMatrix = try {
                if (encodeHints.isNotEmpty()) {
                    writer.encode(cardId, format.format(), finalWidth, finalHeight, encodeHints)
                } else {
                    writer.encode(cardId, format.format(), finalWidth, finalHeight)
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

        fun saveCardPref(context: Context, appWidgetId: Int, cardId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
