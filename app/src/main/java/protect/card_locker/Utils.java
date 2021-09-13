package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.LocaleList;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

import androidx.core.graphics.ColorUtils;
import androidx.exifinterface.media.ExifInterface;
import protect.card_locker.preferences.Settings;

public class Utils {
    private static final String TAG = "Catima";

    // Activity request codes
    public static final int MAIN_REQUEST = 1;
    public static final int SELECT_BARCODE_REQUEST = 2;
    public static final int BARCODE_SCAN = 3;
    public static final int BARCODE_IMPORT_FROM_IMAGE_FILE = 4;
    public static final int CARD_IMAGE_FROM_CAMERA_FRONT = 5;
    public static final int CARD_IMAGE_FROM_CAMERA_BACK = 6;
    public static final int CARD_IMAGE_FROM_FILE_FRONT = 7;
    public static final int CARD_IMAGE_FROM_FILE_BACK = 8;

    static final double LUMINANCE_MIDPOINT = 0.5;

    static final int BITMAP_SIZE_BIG = 512;

    static public LetterBitmap generateIcon(Context context, LoyaltyCard loyaltyCard, boolean forShortcut) {
        return generateIcon(context, loyaltyCard.store, loyaltyCard.headerColor, forShortcut);
    }

    static public LetterBitmap generateIcon(Context context, String store, Integer backgroundColor) {
        return generateIcon(context, store, backgroundColor, false);
    }

    static public LetterBitmap generateIcon(Context context, String store, Integer backgroundColor, boolean forShortcut) {
        if (store.length() == 0) {
            return null;
        }

        int tileLetterFontSize;
        if (forShortcut) {
            tileLetterFontSize = context.getResources().getDimensionPixelSize(R.dimen.tileLetterFontSizeForShortcut);
        } else {
            tileLetterFontSize = context.getResources().getDimensionPixelSize(R.dimen.tileLetterFontSize);
        }

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
        String contents;
        String format;

        if (resultCode != Activity.RESULT_OK) {
            return new BarcodeValues(null, null);
        }

        if (requestCode == Utils.BARCODE_IMPORT_FROM_IMAGE_FILE) {
            Log.i(TAG, "Received image file with possible barcode");

            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), intent.getData());
            } catch (IOException e) {
                Log.e(TAG, "Error getting data from image file");
                e.printStackTrace();
                Toast.makeText(context, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
                return new BarcodeValues(null, null);
            }

            BarcodeValues barcodeFromBitmap = getBarcodeFromBitmap(bitmap);

            if (barcodeFromBitmap.isEmpty()) {
                Log.i(TAG, "No barcode found in image file");
                Toast.makeText(context, R.string.noBarcodeFound, Toast.LENGTH_LONG).show();
            }

            Log.i(TAG, "Read barcode id: " + barcodeFromBitmap.content());
            Log.i(TAG, "Read format: " + barcodeFromBitmap.format());

            return barcodeFromBitmap;
        }

        if (requestCode == Utils.BARCODE_SCAN || requestCode == Utils.SELECT_BARCODE_REQUEST) {
            if (requestCode == Utils.BARCODE_SCAN) {
                Log.i(TAG, "Received barcode information from camera");
            } else if (requestCode == Utils.SELECT_BARCODE_REQUEST) {
                Log.i(TAG, "Received barcode information from typing it");
            }

            contents = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_CONTENTS);
            format = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_FORMAT);

            Log.i(TAG, "Read barcode id: " + contents);
            Log.i(TAG, "Read format: " + format);

            return new BarcodeValues(format, contents);
        }

        throw new UnsupportedOperationException("Unknown request code for parseSetBarcodeActivityResult");
    }

    static public BarcodeValues getBarcodeFromBitmap(Bitmap bitmap) {
        // In order to decode it, the Bitmap must first be converted into a pixel array...
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // ...and then turned into a binary bitmap from its luminance
        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            Result barcodeResult = new MultiFormatReader().decode(binaryBitmap);

            return new BarcodeValues(barcodeResult.getBarcodeFormat().name(), barcodeResult.getText());
        } catch (NotFoundException e) {
            return new BarcodeValues(null, null);
        }
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

    static public String formatBalance(Context context, BigDecimal value, Currency currency) {
        NumberFormat numberFormat = NumberFormat.getInstance();

        if (currency == null) {
            numberFormat.setMaximumFractionDigits(0);
            return context.getString(R.string.balancePoints, numberFormat.format(value));
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setCurrency(currency);
        currencyFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        currencyFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());

        return currencyFormat.format(value);
    }

    static public String formatBalanceWithoutCurrencySymbol(BigDecimal value, Currency currency) {
        NumberFormat numberFormat = NumberFormat.getInstance();

        if (currency == null) {
            numberFormat.setMaximumFractionDigits(0);
            return numberFormat.format(value);
        }

        numberFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        numberFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());

        return numberFormat.format(value);
    }

    static public Boolean currencyHasDecimals(Currency currency) {
        if (currency == null) {
            return false;
        }

        return currency.getDefaultFractionDigits() != 0;
    }

    static public BigDecimal parseCurrency(String value, Boolean hasDecimals) throws NumberFormatException {
        // If there are no decimals expected, remove all separators before parsing
        if (!hasDecimals) {
            value = value.replaceAll("[^0-9]", "");
            return new BigDecimal(value);
        }

        // There are many ways users can write a currency, so we fix it up a bit
        // 1. Replace all non-numbers with dots
        value = value.replaceAll("[^0-9]", ".");

        // 2. Remove all but the last dot
        while (value.split("\\.").length > 2) {
            value = value.replaceFirst("\\.", "");
        }

        // Parse as BigDecimal
        return new BigDecimal(value);
    }

    static public byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }

    static public Bitmap resizeBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        double maxSize = BITMAP_SIZE_BIG;

        double width = bitmap.getWidth();
        double height = bitmap.getHeight();

        if (height > width) {
            double scale = height / maxSize;
            height = maxSize;
            width = width / scale;
        } else if (width > height) {
            double scale = width / maxSize;
            width = maxSize;
            height = height / scale;
        } else {
            height = maxSize;
            width = maxSize;
        }

        return Bitmap.createScaledBitmap(bitmap, (int) Math.round(width), (int) Math.round(height), true);
    }

    static public Bitmap rotateBitmap(Bitmap bitmap, ExifInterface exifInterface) {
        switch (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateBitmap(bitmap, 90f);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateBitmap(bitmap, 180f);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateBitmap(bitmap, 270f);
            default:
                return bitmap;
        }
    }

    static public Bitmap rotateBitmap(Bitmap bitmap, float rotation) {
        if (rotation == 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    static public String getCardImageFileName(int loyaltyCardId, boolean front) {
        StringBuilder cardImageFileNameBuilder = new StringBuilder();

        cardImageFileNameBuilder.append("card_");
        cardImageFileNameBuilder.append(loyaltyCardId);
        cardImageFileNameBuilder.append("_");
        if (front) {
            cardImageFileNameBuilder.append("front");
        } else {
            cardImageFileNameBuilder.append("back");
        }
        cardImageFileNameBuilder.append(".png");

        return cardImageFileNameBuilder.toString();
    }

    static public void saveCardImage(Context context, Bitmap bitmap, String fileName) throws FileNotFoundException {
        if (bitmap == null) {
            context.deleteFile(fileName);
            return;
        }

        FileOutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE);

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    }

    static public void saveCardImage(Context context, Bitmap bitmap, int loyaltyCardId, boolean front) throws FileNotFoundException {
        saveCardImage(context, bitmap, getCardImageFileName(loyaltyCardId, front));
    }

    static public Bitmap retrieveCardImage(Context context, String fileName) {
        FileInputStream in;
        try {
            in = context.openFileInput(fileName);
        } catch (FileNotFoundException e) {
            return null;
        }

        return BitmapFactory.decodeStream(in);
    }

    static public Bitmap retrieveCardImage(Context context, int loyaltyCardId, boolean front) {
        return retrieveCardImage(context, getCardImageFileName(loyaltyCardId, front));
    }

    static public <T,U> U mapGetOrDefault(Map<T,U> map, T key, U defaultValue) {
        U value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    static public Locale stringToLocale(String localeString) {
        String[] localeParts = localeString.split("-");
        if (localeParts.length == 1) {
            return new Locale(localeParts[0]);
        }

        if (localeParts[1].startsWith("r")) {
            localeParts[1] = localeParts[1].substring(1);
        }
        return new Locale(localeParts[0], localeParts[1]);
    }

    static public Context updateBaseContextLocale(Context context) {
        Settings settings = new Settings(context);

        Locale chosenLocale = settings.getLocale();

        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            configuration.locale = chosenLocale != null ? chosenLocale : Locale.getDefault();
            res.updateConfiguration(configuration, res.getDisplayMetrics());
            return context;
        }

        LocaleList localeList = chosenLocale != null ? new LocaleList(chosenLocale) : LocaleList.getDefault();
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);
        return context.createConfigurationContext(configuration);
    }

    static public long getUnixTime() {
        return System.currentTimeMillis() / 1000;
    }
}
