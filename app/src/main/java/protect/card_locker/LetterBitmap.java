package protect.card_locker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.Log;

import androidx.core.graphics.PaintCompat;

/**
 * Original from https://github.com/andOTP/andOTP/blob/master/app/src/main/java/org/shadowice/flocke/andotp/Utilities/LetterBitmap.java
 * which was originally from http://stackoverflow.com/questions/23122088/colored-boxed-with-letters-a-la-gmail
 * Used to create a {@link Bitmap} that contains a letter used in the English
 * alphabet or digit, if there is no letter or digit available, a default image
 * is shown instead.
 */
class LetterBitmap {
    /**
     * The letter bitmap
     */
    private final Bitmap mBitmap;
    /**
     * The background color of the letter bitmap
     */
    private final Integer mColor;

    /**
     * Constructor for <code>LetterTileProvider</code>
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
    public LetterBitmap(Context context, String displayName, String key, int tileLetterFontSize,
                        int width, int height, Integer backgroundColor, Integer textColor) {
        TextPaint paint = new TextPaint();

        if (textColor != null) {
            paint.setColor(textColor);
        } else {
            paint.setColor(Color.WHITE);
        }

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTextSize(tileLetterFontSize);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        if (backgroundColor == null) {
            mColor = getDefaultColor(context, key);
        } else {
            mColor = backgroundColor;
        }

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        String firstChar = displayName.substring(0, 1).toUpperCase();
        int firstCharEnd = 2;
        while (firstCharEnd <= displayName.length()) {
            // Test for the longest render-able string
            // But ignore containing only a-Z0-9 to not render things like ffi as a single character
            String test = displayName.substring(0, firstCharEnd);
            if (!isAlphabetical(test) && PaintCompat.hasGlyph(paint, test)) {
                firstChar = test;
            }
            firstCharEnd++;
        }

        Log.d("LetterBitmap", "using sequence " + firstChar + " to render first char which has length " + firstChar.length());

        final Canvas c = new Canvas();
        c.setBitmap(mBitmap);
        c.drawColor(mColor);

        Rect bounds = new Rect();
        paint.getTextBounds(firstChar, 0, firstChar.length(), bounds);
        c.drawText(firstChar,
                0, firstChar.length(),
                width / 2.0f, (height - (bounds.bottom + bounds.top)) / 2.0f
                , paint);

    }

    /**
     * @return A {@link Bitmap} that contains a letter used in the English
     * alphabet or digit, if there is no letter or digit available, a
     * default image is shown instead
     */
    public Bitmap getLetterTile() {
        return mBitmap;
    }

    /**
     * @return background color used for letter title.
     */
    public int getBackgroundColor() {
        return mColor;
    }

    /**
     * @param key The key used to generate the tile color
     * @return A new or previously chosen color for <code>key</code> used as the
     * tile background color
     */
    private static int pickColor(String key, TypedArray colors) {
        // String.hashCode() is not supposed to change across java versions, so
        // this should guarantee the same key always maps to the same color
        final int color = Math.abs(key.hashCode()) % colors.length();
        return colors.getColor(color, Color.BLACK);
    }

    private static boolean isAlphabetical(String string) {
        return string.matches("[a-zA-Z0-9]*");
    }

    /**
     * Determine the color which the letter tile will use if no default
     * color is provided.
     */
    public static int getDefaultColor(Context context, String key) {
        final Resources res = context.getResources();

        TypedArray colors = res.obtainTypedArray(R.array.letter_tile_colors);
        int color = pickColor(key, colors);
        colors.recycle();

        return color;
    }
}