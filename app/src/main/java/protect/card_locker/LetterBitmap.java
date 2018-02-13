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

/**
 * Original from https://github.com/andOTP/andOTP/blob/master/app/src/main/java/org/shadowice/flocke/andotp/Utilities/LetterBitmap.java
 * which was originally from http://stackoverflow.com/questions/23122088/colored-boxed-with-letters-a-la-gmail
 * Used to create a {@link Bitmap} that contains a letter used in the English
 * alphabet or digit, if there is no letter or digit available, a default image
 * is shown instead.
 */
class LetterBitmap
{

    /**
     * The number of available tile colors
     */
    private static final int NUM_OF_TILE_COLORS = 8;
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
     * @param context The {@link Context} to use
     * @param displayName The name used to create the letter for the tile
     * @param key         The key used to generate the background color for the tile
     * @param tileLetterFontSize The font size used to display the letter
     * @param width       The desired width of the tile
     * @param height      The desired height of the tile
     * @param backgroundColor  (optional) color to use for background.
     * @param textColor  (optional) color to use for text.
     */
    public LetterBitmap(Context context, String displayName, String key, int tileLetterFontSize,
                        int width, int height, Integer backgroundColor, Integer textColor)
    {
        TextPaint paint = new TextPaint();
        paint.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));

        if(textColor != null)
        {
            paint.setColor(textColor);
        }
        else
        {
            paint.setColor(Color.WHITE);
        }

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);

        if(backgroundColor == null)
        {
            mColor = getDefaultColor(context, key);
        }
        else
        {
            mColor = backgroundColor;
        }

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        String firstChar = displayName.substring(0, 1);

        final Canvas c = new Canvas();
        c.setBitmap(mBitmap);
        c.drawColor(mColor);

        char [] firstCharArray = new char[1];
        firstCharArray[0] = firstChar.toUpperCase().charAt(0);
        paint.setTextSize(tileLetterFontSize);

        // The bounds that enclose the letter
        Rect bounds = new Rect();

        paint.getTextBounds(firstCharArray, 0, 1, bounds);
        c.drawText(firstCharArray, 0, 1, width / 2.0f, height / 2.0f
                + (bounds.bottom - bounds.top) / 2.0f, paint);
    }

    /**
     * @return A {@link Bitmap} that contains a letter used in the English
     * alphabet or digit, if there is no letter or digit available, a
     * default image is shown instead
     */
    public Bitmap getLetterTile()
    {
        return mBitmap;
    }

    /**
     * @return background color used for letter title.
     */
    public int getBackgroundColor()
    {
        return mColor;
    }

    /**
     * @param key The key used to generate the tile color
     * @return A new or previously chosen color for <code>key</code> used as the
     * tile background color
     */
    private static int pickColor(String key, TypedArray colors)
    {
        // String.hashCode() is not supposed to change across java versions, so
        // this should guarantee the same key always maps to the same color
        final int color = Math.abs(key.hashCode()) % NUM_OF_TILE_COLORS;
        return colors.getColor(color, Color.BLACK);
    }

    /**
     * Determine the color which the letter tile will use if no default
     * color is provided.
     */
    public static int getDefaultColor(Context context, String key)
    {
        final Resources res = context.getResources();

        TypedArray colors = res.obtainTypedArray(R.array.letter_tile_colors);
        int color = pickColor(key, colors);
        colors.recycle();

        return color;
    }
}