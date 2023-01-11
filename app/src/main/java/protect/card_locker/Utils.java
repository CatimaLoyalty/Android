package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.LocaleList;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.exifinterface.media.ExifInterface;
import androidx.palette.graphics.Palette;

import com.google.android.material.color.DynamicColors;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

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
    public static final int CARD_IMAGE_FROM_CAMERA_ICON = 7;
    public static final int CARD_IMAGE_FROM_FILE_FRONT = 8;
    public static final int CARD_IMAGE_FROM_FILE_BACK = 9;
    public static final int CARD_IMAGE_FROM_FILE_ICON = 10;

    static final double LUMINANCE_MIDPOINT = 0.5;

    static final int BITMAP_SIZE_SMALL = 512;
    static final int BITMAP_SIZE_BIG = 2048;

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

        int pixelSize = context.getResources().getDimensionPixelSize(R.dimen.tileLetterImageSize);

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
                Uri data = intent.getData();
                bitmap = retrieveImageFromUri(context, data);
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

    static public Bitmap retrieveImageFromUri(Context context, Uri data) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ImageDecoder.Source image_source = ImageDecoder.createSource(context.getContentResolver(), data);
            return ImageDecoder.decodeBitmap(image_source, (decoder, info, source) -> decoder.setMutableRequired(true));
        } else {
            return getBitmapSdkLessThan29(data, context);
        }
    }

    @SuppressWarnings("deprecation")
    private static Bitmap getBitmapSdkLessThan29(Uri data, Context context) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), data);
    }

    static public BarcodeValues getBarcodeFromBitmap(Bitmap bitmap) {
        // This function is vulnerable to OOM, so we try again with a smaller bitmap is we get OOM
        for (int i = 0; i < 10; i++) {
            try {
                return Utils.getBarcodeFromBitmapReal(bitmap);
            } catch (OutOfMemoryError e) {
                Log.w(TAG, "Ran OOM in getBarcodeFromBitmap! Trying again with smaller picture! Retry " + i + " of 10.");
                bitmap = Bitmap.createScaledBitmap(bitmap, (int) Math.round(0.75 * bitmap.getWidth()), (int) Math.round(0.75 * bitmap.getHeight()), false);
            }
        }

        // Give up
        return new BarcodeValues(null, null);
    }

    static private BarcodeValues getBarcodeFromBitmapReal(Bitmap bitmap) {
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

        // Note: In #1083 it was discovered that `DatePickerFragment` may sometimes store the expiryDate
        // at 12:00 PM instead of 12:00 AM in the DB. While this has been fixed and the 12-hour difference
        // is not a problem for the way the comparison currently works, it's good to keep in mind such
        // dates may exist in the DB in case the comparison changes in the future and the new one relies
        // on both dates being set at 12:00 AM.
        return expiryDate.before(date.getTime());
    }

    static public String formatBalance(Context context, BigDecimal value, Currency currency) {
        NumberFormat numberFormat = NumberFormat.getInstance();

        if (currency == null) {
            numberFormat.setMaximumFractionDigits(0);
            return context.getResources().getQuantityString(R.plurals.balancePoints, value.intValue(), numberFormat.format(value));
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

    static public BigDecimal parseBalance(String value, Currency currency) throws ParseException {
        NumberFormat numberFormat = NumberFormat.getInstance();

        if (currency == null) {
            numberFormat.setMaximumFractionDigits(0);
        } else {
            numberFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
            numberFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        }

        Log.d(TAG, numberFormat.parse(value).toString());

        return new BigDecimal(numberFormat.parse(value).toString());
    }

    static public byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }

    static public Bitmap resizeBitmap(Bitmap bitmap, double maxSize) {
        if (bitmap == null) {
            return null;
        }

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

    static public String getCardImageFileName(int loyaltyCardId, ImageLocationType type) {
        StringBuilder cardImageFileNameBuilder = new StringBuilder();

        cardImageFileNameBuilder.append("card_");
        cardImageFileNameBuilder.append(loyaltyCardId);
        cardImageFileNameBuilder.append("_");
        if (type == ImageLocationType.front) {
            cardImageFileNameBuilder.append("front");
        } else if (type == ImageLocationType.back) {
            cardImageFileNameBuilder.append("back");
        } else if (type == ImageLocationType.icon) {
            cardImageFileNameBuilder.append("icon");
        } else {
            throw new IllegalArgumentException("Unknown image type");
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

    static public void saveCardImage(Context context, Bitmap bitmap, int loyaltyCardId, ImageLocationType type) throws FileNotFoundException {
        saveCardImage(context, bitmap, getCardImageFileName(loyaltyCardId, type));
    }

    public static File retrieveCardImageAsFile(Context context, String fileName) {
        return context.getFileStreamPath(fileName);
    }

    public static File retrieveCardImageAsFile(Context context, int loyaltyCardId, ImageLocationType type) {
        return retrieveCardImageAsFile(context, getCardImageFileName(loyaltyCardId, type));
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

    static public Bitmap retrieveCardImage(Context context, int loyaltyCardId, ImageLocationType type) {
        return retrieveCardImage(context, getCardImageFileName(loyaltyCardId, type));
    }

    static public <T, U> U mapGetOrDefault(Map<T, U> map, T key, U defaultValue) {
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
            setLocalesSdkLessThan24(chosenLocale, configuration, res);
            return context;
        }

        LocaleList localeList = chosenLocale != null ? new LocaleList(chosenLocale) : LocaleList.getDefault();
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);
        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static void setLocalesSdkLessThan24(Locale chosenLocale, Configuration configuration, Resources res) {
        configuration.locale = chosenLocale != null ? chosenLocale : Locale.getDefault();
        res.updateConfiguration(configuration, res.getDisplayMetrics());
    }

    static public long getUnixTime() {
        return System.currentTimeMillis() / 1000;
    }

    static public boolean isDarkModeEnabled(Context inputContext) {
        int nightModeSetting = new Settings(inputContext).getTheme();
        if (nightModeSetting == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            Configuration config = inputContext.getResources().getConfiguration();
            int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        } else {
            return nightModeSetting == AppCompatDelegate.MODE_NIGHT_YES;
        }
    }

    public static File createTempFile(Context context, String name) {
        return new File(context.getCacheDir() + "/" + name);
    }

    public static String saveTempImage(Context context, Bitmap in, String name, Bitmap.CompressFormat format) {
        File image = createTempFile(context, name);
        try (FileOutputStream out = new FileOutputStream(image)) {
            in.compress(format, 100, out);
            return image.getAbsolutePath();
        } catch (IOException e) {
            Log.d("store temp image", "failed writing temp file for temporary image, name: " + name);
            return null;
        }
    }

    public static Bitmap loadImage(String path) {
        try {
            return BitmapFactory.decodeStream(new FileInputStream(path));
        } catch (IOException e) {
            Log.d("load image", "failed loading image from " + path);
            return null;
        }
    }

    public static Bitmap loadTempImage(Context context, String name) {
        return loadImage(context.getCacheDir() + "/" + name);
    }

    // https://stackoverflow.com/a/59324801/8378787
    public static int getComplementaryColor(int color) {
        int R = color & 255;
        int G = (color >> 8) & 255;
        int B = (color >> 16) & 255;
        int A = (color >> 24) & 255;
        R = 255 - R;
        G = 255 - G;
        B = 255 - B;
        return R + (G << 8) + (B << 16) + (A << 24);
    }

    // replace colors in the current theme
    public static void patchColors(AppCompatActivity activity) {
        Settings settings = new Settings(activity);
        String color = settings.getColor();

        Resources.Theme theme = activity.getTheme();
        Resources resources = activity.getResources();
        if (color.equals(resources.getString(R.string.settings_key_pink_theme))) {
            theme.applyStyle(R.style.pink, true);
        } else if (color.equals(resources.getString(R.string.settings_key_magenta_theme))) {
            theme.applyStyle(R.style.magenta, true);
        } else if (color.equals(resources.getString(R.string.settings_key_violet_theme))) {
            theme.applyStyle(R.style.violet, true);
        } else if (color.equals(resources.getString(R.string.settings_key_blue_theme))) {
            theme.applyStyle(R.style.blue, true);
        } else if (color.equals(resources.getString(R.string.settings_key_sky_blue_theme))) {
            theme.applyStyle(R.style.skyblue, true);
        } else if (color.equals(resources.getString(R.string.settings_key_green_theme))) {
            theme.applyStyle(R.style.green, true);
        } else if (color.equals(resources.getString(R.string.settings_key_brown_theme))) {
            theme.applyStyle(R.style.brown, true);
        } else if (color.equals(resources.getString(R.string.settings_key_catima_theme))) {
            // catima theme is AppTheme itself, no dynamic colors nor applyStyle
        } else {
            // final catch all in case of invalid theme value from older versions
            // also handles R.string.settings_key_system_theme
            DynamicColors.applyToActivityIfAvailable(activity);
        }

        if (isDarkModeEnabled(activity) && settings.getOledDark()) {
            theme.applyStyle(R.style.DarkBackground, true);
        }
    }

    // XXX android 9 and below has issues with patched theme where the background becomes a
    // rendering mess
    // use after views are inflated
    public static void postPatchColors(AppCompatActivity activity) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        activity.findViewById(android.R.id.content).setBackgroundColor(typedValue.data);
    }

    public static void updateMenuCardDetailsButtonState(MenuItem item, boolean currentlyExpanded) {
        if (currentlyExpanded) {
            item.setIcon(R.drawable.ic_baseline_unfold_less_24);
            item.setTitle(R.string.action_hide_details);
        } else {
            item.setIcon(R.drawable.ic_baseline_unfold_more_24);
            item.setTitle(R.string.action_show_details);
        }
    }

    public static int getHeaderColorFromImage(Bitmap image, int fallback) {
        if (image == null) {
            return fallback;
        }

        return new Palette.Builder(image).generate().getDominantColor(R.attr.colorPrimary);
    }

    public static int getRandomHeaderColor(Context context) {
        TypedArray colors = context.getResources().obtainTypedArray(R.array.letter_tile_colors);
        final int color = (int) (Math.random() * colors.length());
        return colors.getColor(color, Color.BLACK);
    }
}
