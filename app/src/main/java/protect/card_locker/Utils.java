package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

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
            value = value.replaceAll("\\.", "").replaceAll(",", "");
            return new BigDecimal(value);
        }

        // There are many ways users can write a currency, so we fix it up a bit
        // 1. Replace all commas with dots
        value = value.replace(',', '.');

        // 2. Remove all but the last dot
        while (value.split("\\.").length > 2) {
            value = value.replaceFirst("\\.", "");
        }

        // Parse as BigDecimal
        return new BigDecimal(value);
    }
}
