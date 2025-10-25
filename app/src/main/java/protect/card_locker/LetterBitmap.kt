package protect.card_locker

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.util.Log
import androidx.core.graphics.PaintCompat
import java.util.Locale
import kotlin.math.abs

/**
 * Original from https://github.com/andOTP/andOTP/blob/master/app/src/main/java/org/shadowice/flocke/andotp/Utilities/LetterBitmap.java
 * which was originally from http://stackoverflow.com/questions/23122088/colored-boxed-with-letters-a-la-gmail
 * Used to create a {@link Bitmap} that contains a letter used in the English
 * alphabet or digit, if there is no letter or digit available, a default image
 * is shown instead.
 */
class LetterBitmap(
    context: Context, displayName: String, key: String, tileLetterFontSize: Int,
    width: Int, height: Int, backgroundColor: Int?, textColor: Int?
) {
    /**
     * A {@link Bitmap} that contains a letter used in the English
     * alphabet or digit, if there is no letter or digit available, a
     * default image is shown instead
     */
    val letterTile: Bitmap

    /**
     * The background color of the letter bitmap
     */
    private val mColor: Int

    /**
     * Constructor for `LetterTileProvider`
     *
     * @param context            The {@link Context} to use
     * @param displayName        The name used to create the letter for the tile
     * @param key                The key used to generate the background color for the tile
     * @param tileLetterFontSize The font size used to display the letter
     * @param width              The desired width of the tile
     * @param height             The desired height of the tile
     * @param backgroundColor    (optional) color to use for background.
     * @param textColor          (optional) color to use for text.
     */
    init {
        val paint = TextPaint().apply {
            color = textColor ?: Color.WHITE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            textSize = tileLetterFontSize.toFloat()
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }

        mColor = backgroundColor ?: getDefaultColor(context, key)

        this.letterTile = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        var firstChar = displayName.substring(0, 1).uppercase(Locale.getDefault())
        var firstCharEnd = 2
        while (firstCharEnd <= displayName.length) {
            // Test for the longest render-able string
            // But ignore containing only a-Z0-9 to not render things like ffi as a single character
            val test = displayName.substring(0, firstCharEnd)
            if (!isAlphabetical(test) && PaintCompat.hasGlyph(paint, test)) {
                firstChar = test
            }
            firstCharEnd++
        }

        Log.d(
            "LetterBitmap",
            "using sequence $firstChar to render first char which has length ${firstChar.length}"
        )

        Canvas().apply {
            setBitmap(this@LetterBitmap.letterTile)
            drawColor(mColor)

            val bounds = Rect()
            paint.getTextBounds(firstChar, 0, firstChar.length, bounds)
            drawText(
                firstChar,
                0, firstChar.length,
                width / 2.0f, (height - (bounds.bottom + bounds.top)) / 2.0f,
                paint
            )
        }
    }

    val backgroundColor: Int
        /**
         * @return background color used for letter title.
         */
        get() = mColor

    companion object {
        /**
         * @param key The key used to generate the tile color
         * @return A new or previously chosen color for `key` used as the
         * tile background color
         */
        private fun pickColor(key: String, colors: TypedArray): Int {
            // String.hashCode() is not supposed to change across java versions, so
            // this should guarantee the same key always maps to the same color
            val color = abs(key.hashCode()) % colors.length()
            return colors.getColor(color, Color.BLACK)
        }

        private fun isAlphabetical(string: String): Boolean {
            return string.matches("[a-zA-Z0-9]*".toRegex())
        }

        /**
         * Determine the color which the letter tile will use if no default
         * color is provided.
         */
        @JvmStatic
        fun getDefaultColor(context: Context, key: String): Int {
            val res = context.resources

            val colors = res.obtainTypedArray(R.array.letter_tile_colors)
            val color: Int = pickColor(key, colors)
            colors.recycle()

            return color
        }
    }
}