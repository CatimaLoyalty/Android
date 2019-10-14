package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import androidx.core.graphics.ColorUtils;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class Utils {
    private static final String TAG = "Catima";

    // Activity request codes
    public static final int MAIN_REQUEST = 1;
    public static final int SELECT_BARCODE_REQUEST = 2;
    public static final int BARCODE_SCAN = 3;
    public static final int BARCODE_IMPORT = 4;

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

    static public BarcodeValues parseSetBarcodeActivityResult(int requestCode, int resultCode, Intent intent, Context context) {
        String contents = null;
        String format = null;

        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode != Utils.BARCODE_IMPORT) {
                if (requestCode == Utils.BARCODE_SCAN) {
                    Log.i(TAG, "Received barcode information from camera");
                } else if (requestCode == Utils.SELECT_BARCODE_REQUEST) {
                    Log.i(TAG, "Received barcode information from typing it");
                } else {
                    return new BarcodeValues(null, null);
                }

                contents = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_CONTENTS);
                format = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_FORMAT);
            } else {
                Log.i(TAG, "Received barcode image");

                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), intent.getData());
                } catch (IOException e) {
                    Log.e(TAG, "Error getting the image data");
                    e.printStackTrace();
                    Toast.makeText(context, R.string.errorReadingImage, Toast.LENGTH_LONG);
                    return new BarcodeValues(null, null);
                }

                // In order to decode it, the Bitmap must first be converted into a pixel array...
                int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                // ...and then turned into a binary bitmap from its luminance
                LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

                try {
                    Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);

                    contents = qrCodeResult.getText();
                    format = qrCodeResult.getBarcodeFormat().name();
                } catch (NotFoundException e) {
                    Log.i(TAG, "No barcode was found");
                    Toast.makeText(context, R.string.noBarcodeFound, Toast.LENGTH_LONG);
                }
            }
        }

        Log.i(TAG, "Read barcode id: " + contents);
        Log.i(TAG, "Read format: " + format);

        return new BarcodeValues(format, contents);
    }

    static public Boolean hasExpired(Date expiryDate) {
        // today
        Calendar date = new GregorianCalendar();
        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        return expiryDate.before(date.getTime());
    }
}
