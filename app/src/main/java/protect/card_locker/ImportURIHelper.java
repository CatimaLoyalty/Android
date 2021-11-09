package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
    private final String[] hosts = new String[3];
    private final String[] paths = new String[3];
    private final String shareText;
    private final String shareMultipleText;

    public ImportURIHelper(Context context) {
        this.context = context;
        hosts[0] = context.getResources().getString(R.string.intent_import_card_from_url_host_catima_app);
        paths[0] = context.getResources().getString(R.string.intent_import_card_from_url_path_prefix_catima_app);
        hosts[1] = context.getResources().getString(R.string.intent_import_card_from_url_host_thelastproject);
        paths[1] = context.getResources().getString(R.string.intent_import_card_from_url_path_prefix_thelastproject);
        hosts[2] = context.getResources().getString(R.string.intent_import_card_from_url_host_brarcher);
        paths[2] = context.getResources().getString(R.string.intent_import_card_from_url_path_prefix_brarcher);
        shareText = context.getResources().getString(R.string.intent_import_card_from_url_share_text);
        shareMultipleText = context.getResources().getString(R.string.intent_import_card_from_url_share_multiple_text);
    }

    private boolean isImportUri(Uri uri) {
        for (int i = 0; i < hosts.length; i++) {
            if (uri.getHost().equals(hosts[i]) && uri.getPath().equals(paths[i])) {
                return true;
            }
        }

        return false;
    }

    public LoyaltyCard parse(Uri uri) throws InvalidObjectException {
        if (!isImportUri(uri)) {
            throw new InvalidObjectException("Not an import URI");
        }

        try {
            // These values are allowed to be null
            CatimaBarcode barcodeType = null;
            Date expiry = null;
            BigDecimal balance = new BigDecimal("0");
            Currency balanceType = null;
            Integer headerColor = null;

            // Store everything in a simple key/value hashmap
            HashMap<String, String> kv = new HashMap<>();

            // First, grab all query parameters (backwards compatibility)
            for (String key : uri.getQueryParameterNames()) {
                kv.put(key, uri.getQueryParameter(key));
            }

            // Then, parse the new and more private fragment part
            // Overriding old format entries if they exist
            String fragment = uri.getFragment();
            if (fragment != null) {
                for (String fragmentPart : fragment.split("&")) {
                    String[] fragmentData = fragmentPart.split("=", 2);
                    kv.put(fragmentData[0], URLDecoder.decode(fragmentData[1], StandardCharsets.UTF_8.name()));
                }
            }

            // Then use all values we care about
            String store = kv.get(STORE);
            String note = kv.get(NOTE);
            String cardId = kv.get(CARD_ID);
            String barcodeId = kv.get(BARCODE_ID);
            if (store == null || note == null || cardId == null)
                throw new InvalidObjectException("Not a valid import URI: " + uri.toString());

            String unparsedBarcodeType = kv.get(BARCODE_TYPE);
            if (unparsedBarcodeType != null && !unparsedBarcodeType.equals("")) {
                barcodeType = CatimaBarcode.fromName(unparsedBarcodeType);
            }

            String unparsedBalance = kv.get(BALANCE);
            if (unparsedBalance != null && !unparsedBalance.equals("")) {
                balance = new BigDecimal(unparsedBalance);
            }
            String unparsedBalanceType = kv.get(BALANCE_TYPE);
            if (unparsedBalanceType != null && !unparsedBalanceType.equals("")) {
                balanceType = Currency.getInstance(unparsedBalanceType);
            }
            String unparsedExpiry = kv.get(EXPIRY);
            if (unparsedExpiry != null && !unparsedExpiry.equals("")) {
                expiry = new Date(Long.parseLong(unparsedExpiry));
            }

            String unparsedHeaderColor = kv.get(HEADER_COLOR);
            if (unparsedHeaderColor != null) {
                headerColor = Integer.parseInt(unparsedHeaderColor);
            }

            return new LoyaltyCard(-1, store, note, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headerColor, 0, Utils.getUnixTime(), 100);
        } catch (NullPointerException | NumberFormatException | UnsupportedEncodingException ex) {
            throw new InvalidObjectException("Not a valid import URI");
        }
    }

    private StringBuilder appendFragment(StringBuilder fragment, String key, String value) throws UnsupportedEncodingException {
        if (fragment.length() > 0) {
            fragment.append("&");
        }

        // Double-encode the value to make sure it can't accidentally contain symbols that'll break the parser
        fragment.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));

        return fragment;
    }

    // Protected for usage in tests
    protected Uri toUri(LoyaltyCard loyaltyCard) throws UnsupportedEncodingException {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https");
        uriBuilder.authority(hosts[0]);
        uriBuilder.path(paths[0]);

        // Use fragment instead of QueryParameter to not leak this data to the server
        StringBuilder fragment = new StringBuilder();

        fragment = appendFragment(fragment, STORE, loyaltyCard.store);
        fragment = appendFragment(fragment, NOTE, loyaltyCard.note);
        fragment = appendFragment(fragment, BALANCE, loyaltyCard.balance.toString());
        if (loyaltyCard.balanceType != null) {
            fragment = appendFragment(fragment, BALANCE_TYPE, loyaltyCard.balanceType.getCurrencyCode());
        }
        if (loyaltyCard.expiry != null) {
            fragment = appendFragment(fragment, EXPIRY, String.valueOf(loyaltyCard.expiry.getTime()));
        }
        fragment = appendFragment(fragment, CARD_ID, loyaltyCard.cardId);
        if (loyaltyCard.barcodeId != null) {
            fragment = appendFragment(fragment, BARCODE_ID, loyaltyCard.barcodeId);
        }

        if (loyaltyCard.barcodeType != null) {
            fragment = appendFragment(fragment, BARCODE_TYPE, loyaltyCard.barcodeType.name());
        }
        if (loyaltyCard.headerColor != null) {
            fragment = appendFragment(fragment, HEADER_COLOR, loyaltyCard.headerColor.toString());
        }
        // Star status will not be exported
        // Front and back pictures are often too big to fit into a message in base64 nicely, not sharing either...

        uriBuilder.fragment(fragment.toString());
        return uriBuilder.build();
    }

    public void startShareIntent(List<LoyaltyCard> loyaltyCards) throws UnsupportedEncodingException {
        int loyaltyCardCount = loyaltyCards.size();

        StringBuilder text = new StringBuilder();
        if (loyaltyCardCount == 1) {
            text.append(shareText);
        } else {
            text.append(shareMultipleText);
        }
        text.append("\n\n");

        for (int i = 0; i < loyaltyCardCount; i++) {
            LoyaltyCard loyaltyCard = loyaltyCards.get(i);

            text.append(loyaltyCard.store + ": " + toUri(loyaltyCard));

            if (i < (loyaltyCardCount - 1)) {
                text.append("\n\n");
            }
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        context.startActivity(shareIntent);
    }
}
