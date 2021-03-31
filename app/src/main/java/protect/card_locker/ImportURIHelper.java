package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;

import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

public class ImportURIHelper {
    private static final String STORE = DBHelper.LoyaltyCardDbIds.STORE;
    private static final String NOTE = DBHelper.LoyaltyCardDbIds.NOTE;
    private static final String EXPIRY = DBHelper.LoyaltyCardDbIds.EXPIRY;
    private static final String BALANCE = DBHelper.LoyaltyCardDbIds.BALANCE;
    private static final String BALANCE_TYPE = DBHelper.LoyaltyCardDbIds.BALANCE_TYPE;
    private static final String CARD_ID = DBHelper.LoyaltyCardDbIds.CARD_ID;
    private static final String BARCODE_ID = DBHelper.LoyaltyCardDbIds.BARCODE_ID;
    private static final String BARCODE_TYPE = DBHelper.LoyaltyCardDbIds.BARCODE_TYPE;

    private static final String HEADER_COLOR = DBHelper.LoyaltyCardDbIds.HEADER_COLOR;

    private final Context context;
    private final String host;
    private final String path;
    private final String oldHost;
    private final String oldPath;
    private final String shareText;

    public ImportURIHelper(Context context) {
        this.context = context;
        host = context.getResources().getString(R.string.intent_import_card_from_url_host);
        path = context.getResources().getString(R.string.intent_import_card_from_url_path_prefix);
        oldHost = "brarcher.github.io";
        oldPath = "/loyalty-card-locker/share";
        shareText = context.getResources().getString(R.string.intent_import_card_from_url_share_text);
    }

    private boolean isImportUri(Uri uri) {
        return (uri.getHost().equals(host) && uri.getPath().equals(path)) || (uri.getHost().equals(oldHost) && uri.getPath().equals(oldPath));
    }

    public LoyaltyCard parse(Uri uri) throws InvalidObjectException {
        if(!isImportUri(uri)) {
            throw new InvalidObjectException("Not an import URI");
        }

        try {
            // These values are allowed to be null
            BarcodeFormat barcodeType = null;
            Date expiry = null;
            BigDecimal balance = new BigDecimal("0");
            Currency balanceType = null;
            Integer headerColor = null;

            String store = uri.getQueryParameter(STORE);
            String note = uri.getQueryParameter(NOTE);
            String cardId = uri.getQueryParameter(CARD_ID);
            String barcodeId = uri.getQueryParameter(BARCODE_ID);
            if (store == null || note == null || cardId == null) throw new InvalidObjectException("Not a valid import URI");

            String unparsedBarcodeType = uri.getQueryParameter(BARCODE_TYPE);
            if(unparsedBarcodeType != null && !unparsedBarcodeType.equals(""))
            {
                barcodeType = BarcodeFormat.valueOf(unparsedBarcodeType);
            }

            String unparsedBalance = uri.getQueryParameter(BALANCE);
            if(unparsedBalance != null && !unparsedBalance.equals(""))
            {
                balance = new BigDecimal(unparsedBalance);
            }
            String unparsedBalanceType = uri.getQueryParameter(BALANCE_TYPE);
            if (unparsedBalanceType != null && !unparsedBalanceType.equals(""))
            {
                balanceType = Currency.getInstance(unparsedBalanceType);
            }
            String unparsedExpiry = uri.getQueryParameter(EXPIRY);
            if(unparsedExpiry != null && !unparsedExpiry.equals(""))
            {
                expiry = new Date(Long.parseLong(unparsedExpiry));
            }

            String unparsedHeaderColor = uri.getQueryParameter(HEADER_COLOR);
            if(unparsedHeaderColor != null)
            {
                headerColor = Integer.parseInt(unparsedHeaderColor);
            }

            return new LoyaltyCard(-1, store, note, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headerColor, 0);
        } catch (NullPointerException | NumberFormatException ex) {
            throw new InvalidObjectException("Not a valid import URI");
        }
    }

    // Protected for usage in tests
    protected Uri toUri(LoyaltyCard loyaltyCard) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https");
        uriBuilder.authority(host);
        uriBuilder.path(path);
        uriBuilder.appendQueryParameter(STORE, loyaltyCard.store);
        uriBuilder.appendQueryParameter(NOTE, loyaltyCard.note);
        uriBuilder.appendQueryParameter(BALANCE, loyaltyCard.balance.toString());
        if (loyaltyCard.balanceType != null) {
            uriBuilder.appendQueryParameter(BALANCE_TYPE, loyaltyCard.balanceType.getCurrencyCode());
        }
        if (loyaltyCard.expiry != null) {
            uriBuilder.appendQueryParameter(EXPIRY, String.valueOf(loyaltyCard.expiry.getTime()));
        }
        uriBuilder.appendQueryParameter(CARD_ID, loyaltyCard.cardId);
        if(loyaltyCard.barcodeId != null) {
            uriBuilder.appendQueryParameter(BARCODE_ID, loyaltyCard.barcodeId);
        }

        if(loyaltyCard.barcodeType != null) {
            uriBuilder.appendQueryParameter(BARCODE_TYPE, loyaltyCard.barcodeType.toString());
        }
        if(loyaltyCard.headerColor != null) {
            uriBuilder.appendQueryParameter(HEADER_COLOR, loyaltyCard.headerColor.toString());
        }
        //StarStatus will not be exported
        return uriBuilder.build();
    }

    private void startShareIntent(Uri uri) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText + "\n" + uri.toString());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        context.startActivity(shareIntent);
    }

    public void startShareIntent(LoyaltyCard loyaltyCard) {
        startShareIntent(toUri(loyaltyCard));
    }
}
