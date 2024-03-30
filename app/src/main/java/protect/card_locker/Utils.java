package protect.card_locker;

import android.annotation.SuppressLint;
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
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.palette.graphics.Palette;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import protect.card_locker.preferences.Settings;

public class Utils {
    private static final String TAG = "Catima";

    // Activity request codes
    public static final int MAIN_REQUEST = 1;
    public static final int SELECT_BARCODE_REQUEST = 2;
    public static final int BARCODE_SCAN = 3;
    public static final int BARCODE_IMPORT_FROM_IMAGE_FILE = 4;
    public static final int BARCODE_IMPORT_FROM_PDF_FILE = 5;
    public static final int CARD_IMAGE_FROM_CAMERA_FRONT = 6;
    public static final int CARD_IMAGE_FROM_CAMERA_BACK = 7;
    public static final int CARD_IMAGE_FROM_CAMERA_ICON = 8;
    public static final int CARD_IMAGE_FROM_FILE_FRONT = 9;
    public static final int CARD_IMAGE_FROM_FILE_BACK = 10;
    public static final int CARD_IMAGE_FROM_FILE_ICON = 11;

    public static final String CARD_IMAGE_FILENAME_REGEX = "^(card_)(\\d+)(_(?:front|back|icon)\\.png)$";

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

    static public List<BarcodeValues> retrieveBarcodesFromImage(Context context, Uri uri) {
        Log.i(TAG, "Received image file with possible barcode");

        if (uri == null) {
            Log.e(TAG, "Uri did not contain any data");
            Toast.makeText(context, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }

        Bitmap bitmap;
        try {
            bitmap = retrieveImageFromUri(context, uri);
        } catch (IOException e) {
            Log.e(TAG, "Error getting data from image file");
            e.printStackTrace();
            Toast.makeText(context, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }

        List<BarcodeValues> barcodesFromBitmap = getBarcodesFromBitmap(bitmap);

        if (barcodesFromBitmap.isEmpty()) {
            Log.i(TAG, "No barcode found in image file");
            Toast.makeText(context, R.string.noBarcodeFound, Toast.LENGTH_LONG).show();
        }

        return barcodesFromBitmap;
    }

    static public List<BarcodeValues> retrieveBarcodesFromPdf(Context context, Uri uri) {
        Log.i(TAG, "Received PDF file with possible barcode");
        if (uri == null) {
            Log.e(TAG, "Uri did not contain any data");
            Toast.makeText(context, R.string.errorReadingFile, Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }
    
        ParcelFileDescriptor parcelFileDescriptor = null;
        PdfRenderer renderer = null;
        List<BarcodeValues> barcodesFromPdfPages = new ArrayList<>();
    
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                renderer = new PdfRenderer(parcelFileDescriptor);
    
                // Loop over all pages to find barcodes
                Bitmap renderedPage;
                for (int i = 0; i < renderer.getPageCount(); i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    renderedPage = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                    page.render(renderedPage, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
    
                    List<BarcodeValues> barcodesFromPage = getBarcodesFromBitmap(renderedPage);
                    for (BarcodeValues barcodeValues : barcodesFromPage) {
                        barcodeValues.setNote(String.format(context.getString(R.string.pageWithNumber), i+1));
                        barcodesFromPdfPages.add(barcodeValues);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading PDF file", e);
            Toast.makeText(context, R.string.errorReadingFile, Toast.LENGTH_LONG).show();
        } finally {
            // Resource handling
            if (renderer != null) {
                renderer.close();
            }
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing ParcelFileDescriptor", e);
                }
            }
        }
    
        if (barcodesFromPdfPages.isEmpty()) {
            Log.i(TAG, "No barcode found in pdf file");
            Toast.makeText(context, R.string.noBarcodeFound, Toast.LENGTH_LONG).show();
        }
        return barcodesFromPdfPages;
    }

    /**
     * Returns the Barcode format and content based on the result of an activity.
     * It shows toasts to notify the end-user as needed itself and will return an empty
     * BarcodeValues object if the activity was cancelled or nothing could be found.
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     * @param context
     * @return BarcodeValues
     */
    static public List<BarcodeValues> parseSetBarcodeActivityResult(int requestCode, int resultCode, Intent intent, Context context) {
        String contents;
        String format;

        if (resultCode != Activity.RESULT_OK) {
            return new ArrayList<>();
        }

        if (requestCode == Utils.BARCODE_IMPORT_FROM_IMAGE_FILE) {
            return retrieveBarcodesFromImage(context, intent.getData());
        }

        if (requestCode == Utils.BARCODE_IMPORT_FROM_PDF_FILE) {
            return retrieveBarcodesFromPdf(context, intent.getData());
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

            return Collections.singletonList(new BarcodeValues(format, contents));
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

    static public List<BarcodeValues> getBarcodesFromBitmap(Bitmap bitmap) {
        // This function is vulnerable to OOM, so we try again with a smaller bitmap is we get OOM
        for (int i = 0; i < 10; i++) {
            try {
                return Utils.getBarcodesFromBitmapReal(bitmap);
            } catch (OutOfMemoryError e) {
                Log.w(TAG, "Ran OOM in getBarcodesFromBitmap! Trying again with smaller picture! Retry " + i + " of 10.");
                bitmap = Bitmap.createScaledBitmap(bitmap, (int) Math.round(0.75 * bitmap.getWidth()), (int) Math.round(0.75 * bitmap.getHeight()), false);
            }
        }

        // Give up
        return new ArrayList<>();
    }

    static private List<BarcodeValues> getBarcodesFromBitmapReal(Bitmap bitmap) {
        // In order to decode it, the Bitmap must first be converted into a pixel array...
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // ...and then turned into a binary bitmap from its luminance
        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        List<BarcodeValues> barcodeValuesList = new ArrayList<>();
        try {
            MultiFormatReader multiFormatReader = new MultiFormatReader();
            MultipleBarcodeReader multipleBarcodeReader = new GenericMultipleBarcodeReader(multiFormatReader);

            Result[] barcodeResults = multipleBarcodeReader.decodeMultiple(binaryBitmap);

            for (Result barcodeResult : barcodeResults) {
                Log.i(TAG, "Read barcode id: " + barcodeResult.getText());
                Log.i(TAG, "Read format: " + barcodeResult.getBarcodeFormat().name());

                barcodeValuesList.add(new BarcodeValues(barcodeResult.getBarcodeFormat().name(), barcodeResult.getText()));
            }

            return barcodeValuesList;
        } catch (NotFoundException e) {
            return barcodeValuesList;
        }
    }

    static public void makeUserChooseBarcodeFromList(Context context, List<BarcodeValues> barcodeValuesList, BarcodeValuesListDisambiguatorCallback callback) {
        // If there is only one choice, consider it chosen
        if (barcodeValuesList.size() == 1) {
            callback.onUserChoseBarcode(barcodeValuesList.get(0));
            return;
        }

        // Ask user to choose a barcode
        // TODO: This should contain an image of the barcode in question to help users understand the choice they're making
        CharSequence[] barcodeDescriptions = new CharSequence[barcodeValuesList.size()];
        for (int i = 0; i < barcodeValuesList.size(); i++) {
            BarcodeValues barcodeValues = barcodeValuesList.get(i);
            CatimaBarcode catimaBarcode = CatimaBarcode.fromName(barcodeValues.format());

            String barcodeContent = barcodeValues.content();
            // Shorten overly long barcodes
            if (barcodeContent.length() > 22) {
                barcodeContent = barcodeContent.substring(0, 20) + "…";
            }

            if (barcodeValues.note() != null) {
                barcodeDescriptions[i] = String.format("%s: %s (%s)", barcodeValues.note(), catimaBarcode.prettyName(), barcodeContent);
            } else {
                barcodeDescriptions[i] = String.format("%s (%s)", catimaBarcode.prettyName(), barcodeContent);
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(context.getString(R.string.multipleBarcodesFoundPleaseChooseOne));
        builder.setItems(
                barcodeDescriptions,
                (dialogInterface, i) -> callback.onUserChoseBarcode(barcodeValuesList.get(i))
        );
        builder.setOnCancelListener(dialogInterface -> callback.onUserDismissedSelector());
        builder.show();
    }

    static public Boolean isNotYetValid(Date validFromDate) {
        // The note in `hasExpired` does not apply here, since the bug was fixed before this feature was added.
        return validFromDate.after(getStartOfToday().getTime());
    }

    static public Boolean hasExpired(Date expiryDate) {
        // Note: In #1083 it was discovered that `DatePickerFragment` may sometimes store the expiryDate
        // at 12:00 PM instead of 12:00 AM in the DB. While this has been fixed and the 12-hour difference
        // is not a problem for the way the comparison currently works, it's good to keep in mind such
        // dates may exist in the DB in case the comparison changes in the future and the new one relies
        // on both dates being set at 12:00 AM.
        return expiryDate.before(getStartOfToday().getTime());
    }

    static private Calendar getStartOfToday() {
        // today
        Calendar date = new GregorianCalendar();
        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date;
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

    /**
     * Returns a card image filename (string) with the ID replaced according to the map if the input is a valid card image filename (string), otherwise null.
     *
     * @param fileName e.g. "card_1_front.png"
     * @param idMap e.g. Map.of(1, 2)
     * @return String e.g. "card_2_front.png"
     */
    static public String getRenamedCardImageFileName(final String fileName, final Map<Integer, Integer> idMap) {
        Pattern pattern = Pattern.compile(CARD_IMAGE_FILENAME_REGEX);
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.matches()) {
            StringBuilder cardImageFileNameBuilder = new StringBuilder();
            cardImageFileNameBuilder.append(matcher.group(1));
            try {
                int id = Integer.parseInt(matcher.group(2));
                cardImageFileNameBuilder.append(idMap.getOrDefault(id, id));
            } catch (NumberFormatException _e) {
                return null;
            }
            cardImageFileNameBuilder.append(matcher.group(3));
            return cardImageFileNameBuilder.toString();
        }
        return null;
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

        // New API is broken on Android 6 and lower when selecting locales with both language and country, so still keeping this
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Resources res = context.getResources();
            Configuration configuration = res.getConfiguration();
            setLocalesSdkLessThan24(chosenLocale, configuration, res);
            return context;
        }

        /* Documentation at https://developer.android.com/reference/androidx/appcompat/app/AppCompatDelegate#setApplicationLocales(androidx.core.os.LocaleListCompat)
        For API levels below that, the developer has two options:
        - They can opt-in to automatic storage handled through the library...
        - The second option is that they can choose to handle storage themselves.
        In order to do so they must use this API to initialize locales during app-start up and provide their stored locales.
        In this case, API should be called before Activity.onCreate() in the activity lifecycle, e.g. in attachBaseContext().
        Note: Developers should gate this to API versions <33.

        We are handling storage ourselves (courtesy of the in-app language picker), so we take the second approach.
        So according to docs, we should have the API < 33 check.
        */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            AppCompatDelegate.setApplicationLocales(chosenLocale != null ? LocaleListCompat.create(chosenLocale) : LocaleListCompat.getEmptyLocaleList());
        }

        return context;
    }

    @SuppressWarnings("deprecation")
    private static void setLocalesSdkLessThan24(Locale chosenLocale, Configuration configuration, Resources res) {
        configuration.locale = chosenLocale != null ? chosenLocale : Locale.getDefault();
        res.updateConfiguration(configuration, res.getDisplayMetrics());
    }

    /**
     * Android 13 settings seems to "force" the user to select country of locale, but many app-supported locales either only have language, not country
     * or have a country the user doesn't want, which creates a mismatch between the app's supported locales and the system locale.
     * <br>
     * Example: The user chooses Espanol (Espana) in system settings, but the app only supports Espanol (Argentina) and the "plain" Espanol.
     * <br>
     * This method returns the app-supported locale that is most similar to the system one.
     * @param appLocales Locales supported by the app
     * @param sysLocale Per-app locale in system settings
     * @return The app-supported locale that best matches the system per-app locale
     */
    @NonNull
    public static Locale getBestMatchLocale(@NonNull List<Locale> appLocales, @NonNull Locale sysLocale) {
        int highestMatchMagnitude = appLocales.stream()
                .mapToInt(appLocale -> calculateMatchMagnitudeOfTwoLocales(appLocale, sysLocale))
                .max()
                .orElseThrow(() -> new IllegalArgumentException("appLocales is empty"));
        for (int i = 0; i < appLocales.size(); i++) {
            Locale appLocale = appLocales.get(i);
            if (calculateMatchMagnitudeOfTwoLocales(appLocale, sysLocale) == highestMatchMagnitude) {
                return appLocale;
            }
        }
        throw new AssertionError("This is not possible; there must be a locale whose match magnitude == " + highestMatchMagnitude + " with " + sysLocale.toLanguageTag());
    }

    private static int calculateMatchMagnitudeOfTwoLocales(@NonNull Locale appLocale, @NonNull Locale sysLocale) {
        List<String> appLocaleAdjusted = new ArrayList<>();
        List<String> sysLocaleAdjusted = new ArrayList<>();
        appLocaleAdjusted.add(appLocale.getLanguage());
        sysLocaleAdjusted.add(sysLocale.getLanguage());
        if (!appLocale.getCountry().isEmpty() && !sysLocale.getCountry().isEmpty()) {
            appLocaleAdjusted.add(appLocale.getCountry());
            sysLocaleAdjusted.add(sysLocale.getCountry());
        }
        if (!appLocale.getVariant().isEmpty() && !sysLocale.getVariant().isEmpty()) {
            appLocaleAdjusted.add(appLocale.getVariant());
            sysLocaleAdjusted.add(sysLocale.getVariant());
        }
        if (!appLocale.getScript().isEmpty() && !sysLocale.getScript().isEmpty()) {
            appLocaleAdjusted.add(appLocale.getScript());
            sysLocaleAdjusted.add(sysLocale.getScript());
        }
        if (appLocaleAdjusted.equals(sysLocaleAdjusted)) {
            return appLocaleAdjusted.size();
        }
        return 0;
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

    public static File copyToTempFile(Context context, InputStream input, String name) throws IOException {
        File file = createTempFile(context, name);
        try (input; FileOutputStream out = new FileOutputStream(file)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = input.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            return file;
        }
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

    // Force correct color
    // Fixes OLED dark mode in MainActivity
    public static void postPatchColors(AppCompatActivity activity) {
        activity.findViewById(android.R.id.content).setBackgroundColor(resolveBackgroundColor(activity));
    }

    // Either pass an Activity on which to call getWindow() or an existing Window (may be null) returned by that function.
    public static void setNavigationBarColor(@Nullable AppCompatActivity activity, @Nullable Window window, int color, boolean useLightBars) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            if (window == null && activity != null) {
                window = activity.getWindow();
            }
            if (window != null) {
                View decorView = window.getDecorView();
                WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
                wic.setAppearanceLightNavigationBars(useLightBars);
                window.setNavigationBarColor(color);
            }
        }
    }

    public static int resolveBackgroundColor(AppCompatActivity activity) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        return typedValue.data;
    }

    public static int getHeaderColorFromImage(Bitmap image, int fallback) {
        if (image == null) {
            return fallback;
        }

        return new Palette.Builder(image).generate().getDominantColor(androidx.appcompat.R.attr.colorPrimary);
    }

    public static int getRandomHeaderColor(Context context) {
        TypedArray colors = context.getResources().obtainTypedArray(R.array.letter_tile_colors);
        final int color = (int) (Math.random() * colors.length());
        return colors.getColor(color, Color.BLACK);
    }

    public static String readTextFile(Context context, @RawRes int resourceId) throws IOException {
        InputStream input = context.getResources().openRawResource(resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        while (true) {
            String nextLine = reader.readLine();

            if (nextLine == null) {
                reader.close();
                break;
            }

            result.append("\n");
            result.append(nextLine);
        }

        return result.toString();
    }

    // Very crude Markdown to HTML conversion.
    // Only supports what's currently being used in CHANGELOG.md and PRIVACY.md.
    // May break easily.
    public static String basicMDToHTML(final String input) {
        return input
                .replaceAll("(?m)^#\\s+(.*)", "<h1>$1</h1>")
                .replaceAll("(?m)^##\\s+(.*)", "<h2>$1</h2>")
                .replaceAll("\\[([^]]+)\\]\\((https?://[\\w@#%&+=:?/.-]+)\\)", "<a href=\"$2\">$1</a>")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>")
                .replaceAll("(?m)^-\\s+(.*)", "<ul><li>&nbsp;$1</li></ul>")
                .replace("</ul>\n<ul>", "");
    }

    // Very crude autolinking.
    // Only supports what's currently being used in CHANGELOG.md and PRIVACY.md.
    // May break easily.
    public static String linkify(final String input) {
        return input
                .replaceAll("([\\w.-]+@[\\w-]+(\\.[\\w-]+)+)", "<a href=\"mailto:$1\">$1</a>")
                .replaceAll("(?<!href=\")\\b(https?://[\\w@#%&+=:?/.-]*[\\w@#%&+=:?/-])", "<a href=\"$1\">$1</a>");
    }

    /**
     * Sets an icon or text with background on the given ImageView and/or TextView, including background colour.
     *
     * @param context Android context
     * @param loyaltyCard Loyalty Card
     * @param icon Bitmap of the icon to set, or null
     * @param backgroundOrIcon ImageView to draw the icon and background on to
     * @param textWhenNoImage TextView to write the loyalty card name into if icon is null
     * @return background colour
     */
    public static int setIconOrTextWithBackground(Context context, LoyaltyCard loyaltyCard, Bitmap icon, ImageView backgroundOrIcon, TextView textWhenNoImage) {
        int headerColor = getHeaderColor(context, loyaltyCard);
        backgroundOrIcon.setImageBitmap(icon);
        backgroundOrIcon.setBackgroundColor(headerColor);

        if (icon != null) {
            textWhenNoImage.setVisibility(View.GONE);
        } else {
            textWhenNoImage.setVisibility(View.VISIBLE);
            textWhenNoImage.setText(loyaltyCard.store);
            textWhenNoImage.setTextColor(Utils.needsDarkForeground(headerColor) ? Color.BLACK : Color.WHITE);
        }

        return headerColor;
    }

    public static boolean installedFromGooglePlay(Context context) {
        try {
            String packageName = context.getPackageName();
            String installer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                installer = context.getPackageManager().getInstallSourceInfo(packageName).getInstallingPackageName();
            } else {
                installer = context.getPackageManager().getInstallerPackageName(packageName);
            }
            return installer.equals("com.android.vending");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getHeaderColor(Context context, LoyaltyCard loyaltyCard) {
        return loyaltyCard.headerColor != null ? loyaltyCard.headerColor : LetterBitmap.getDefaultColor(context, loyaltyCard.store);
    }

    public static String checksum(InputStream input) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[4096];
            int len;
            while ((len = input.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException _e) {
            return null;
        }
    }

    public static boolean equals(final Object a, final Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void makeTextViewLinksClickable(final TextView textView, final Spanned text) {
        textView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX() - textView.getTotalPaddingLeft() + textView.getScrollX();
                int y = (int) event.getY() - textView.getTotalPaddingTop() + textView.getScrollY();
                Layout layout = textView.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                ClickableSpan[] links = text.getSpans(off, off, ClickableSpan.class);
                if (links.length != 0) {
                    ClickableSpan link = links[0];
                    if (action == MotionEvent.ACTION_UP) {
                        link.onClick(textView);
                    }
                    return true;
                }
            }
            return false;
        });
    }
}
