package protect.card_locker;

import android.content.Context;

public class Utils {
    static public LetterBitmap generateIcon(Context context, String store, Integer backgroundColor, Integer textColor) {
        if (store.length() == 0) {
            return null;
        }

        int tileLetterFontSize = context.getResources().getDimensionPixelSize(R.dimen.tileLetterFontSize);
        int pixelSize = context.getResources().getDimensionPixelSize(R.dimen.cardThumbnailSize);

        return new LetterBitmap(context, store, store,
                tileLetterFontSize, pixelSize, pixelSize, backgroundColor, textColor);
    }
}
