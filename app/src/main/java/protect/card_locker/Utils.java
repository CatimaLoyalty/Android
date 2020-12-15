package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import androidx.core.graphics.ColorUtils;

public class Utils {
    private static final String TAG = "Catima";

    // Activity request codes
    public static final int MAIN_REQUEST = 1;
    public static final int SELECT_BARCODE_REQUEST = 2;
    public static final int BARCODE_SCAN = 3;

    static final double LUMINANCE_MIDPOINT = 0.5;

    static public LetterBitmap generateIcon(Context context, String store, Integer backgroundColor) {
        if (store.length() == 0) {
            return null;
        }

        int tileLetterFontSize = context.getResources().getDimensionPixelSize(R.dimen.tileLetterFontSize);
        int pixelSize = context.getResources().getDimensionPixelSize(R.dimen.cardThumbnailSize);

        if (backgroundColor == null) {
            backgroundColor = LetterBitmap.getDefaultColor(context, store);
        }

        return new LetterBitmap(context, store, store,
                tileLetterFontSize, pixelSize, pixelSize, backgroundColor, needsDarkForeground(backgroundColor) ? Color.BLACK : Color.WHITE);
    }

    static public boolean needsDarkForeground(Integer backgroundColor) {
        return ColorUtils.calculateLuminance(backgroundColor) > LUMINANCE_MIDPOINT;
    }

    static public BarcodeValues parseSetBarcodeActivityResult(int requestCode, int resultCode, Intent intent) {
        String contents = null;
        String format = null;

        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == Utils.BARCODE_SCAN) {
                Log.i(TAG, "Received barcode information from camera");
            } else if (requestCode == Utils.SELECT_BARCODE_REQUEST) {
                Log.i(TAG, "Received barcode information from typing it");
            } else {
                return new BarcodeValues(null, null);
            }

            contents = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_CONTENTS);
            format = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_FORMAT);
        }

        Log.i(TAG, "Read barcode id: " + contents);
        Log.i(TAG, "Read format: " + format);

        return new BarcodeValues(format, contents);
    }
}
