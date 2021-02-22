package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.io.InvalidObjectException;
import java.util.ArrayList;

public class ImportURIHelper {

    private static final String STORE = DBHelper.LoyaltyCardDbIds.STORE;
    private static final String NOTE = DBHelper.LoyaltyCardDbIds.NOTE;
    private static final String CARD_ID = DBHelper.LoyaltyCardDbIds.CARD_ID;
    private static final String BARCODE_TYPE = DBHelper.LoyaltyCardDbIds.BARCODE_TYPE;
    private static final String HEADER_COLOR = DBHelper.LoyaltyCardDbIds.HEADER_COLOR;
    private static final String HEADER_TEXT_COLOR = DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR;
    private static final String COUNT = "count";

    private final Context context;
    private final String host;
    private final String path;
    private final String oldHost;
    private final String oldPath;
    private String shareText;


    public ImportURIHelper(Context inputContext) {
        this.context = inputContext;
        host = inputContext.getResources().getString(R.string.intent_import_card_from_url_host);
        path = inputContext.getResources().getString(R.string.intent_import_card_from_url_path_prefix);
        oldHost = "brarcher.github.io";
        oldPath = "/loyalty-card-locker/share";
        shareText = inputContext.getResources().getString(R.string.intent_import_card_from_url_share_text_singular);
    }

    private boolean isImportUri(Uri inputURI) {
        return (inputURI.getHost().equals(host) && inputURI.getPath().equals(path)) || (inputURI.getHost().equals(oldHost) && inputURI.getPath().equals(oldPath));
    }

    public ArrayList<LoyaltyCard> parse(Uri inputURI) throws InvalidObjectException {
        if (!isImportUri(inputURI)) {
            throw new InvalidObjectException("Not an import URI");
        }

        ArrayList<LoyaltyCard> result = new ArrayList<>();
        try {
            // These values are allowed to be null
            Integer headerColor = null;
            Integer headerTextColor = null;

            int numberOfCards = Integer.parseInt(inputURI.getQueryParameter(COUNT));

            for (int i = 0; i < numberOfCards; i++) {
                String store = inputURI.getQueryParameter(STORE + i);
                String note = inputURI.getQueryParameter(NOTE + i);
                String cardId = inputURI.getQueryParameter(CARD_ID + i);
                String barcodeType = inputURI.getQueryParameter(BARCODE_TYPE + i);

                if (store == null || note == null || cardId == null || barcodeType == null) {
                    throw new InvalidObjectException("Not a valid import URI");
                }

                String unparsedHeaderColor = inputURI.getQueryParameter(HEADER_COLOR + i);
                if (unparsedHeaderColor != null) {
                    headerColor = Integer.parseInt(unparsedHeaderColor);
                }

                String unparsedHeaderTextColor = inputURI.getQueryParameter(HEADER_TEXT_COLOR + i);
                if (unparsedHeaderTextColor != null) {
                    headerTextColor = Integer.parseInt(unparsedHeaderTextColor);
                }

                result.add(new LoyaltyCard(-1, store, note, cardId, barcodeType, headerColor, headerTextColor, 0));
            }

        } catch (NullPointerException | NumberFormatException ex) {
            throw new InvalidObjectException("Not a valid import URI");
        }

        return result;
    }

    // Protected for usage in tests
    protected Uri toUri(LoyaltyCard inputLoyaltyCard) {
        shareText = context.getResources().getString(R.string.intent_import_card_from_url_share_text_singular);
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https");
        uriBuilder.authority(host);
        uriBuilder.path(path);

        uriBuilder.appendQueryParameter(COUNT, "1");

        uriBuilder.appendQueryParameter(STORE + 0, inputLoyaltyCard.store);
        uriBuilder.appendQueryParameter(NOTE + 0, inputLoyaltyCard.note);
        uriBuilder.appendQueryParameter(CARD_ID + 0, inputLoyaltyCard.cardId);
        uriBuilder.appendQueryParameter(BARCODE_TYPE + 0, inputLoyaltyCard.barcodeType);

        if (inputLoyaltyCard.headerColor != null) {
            uriBuilder.appendQueryParameter(HEADER_COLOR + 0, inputLoyaltyCard.headerColor.toString());
        }

        if (inputLoyaltyCard.headerTextColor != null) {
            uriBuilder.appendQueryParameter(HEADER_TEXT_COLOR + 0, inputLoyaltyCard.headerTextColor.toString());
        }
        // Star Status is a personal preferences and thus not exported/imported

        return uriBuilder.build();
    }

    protected Uri toUri(ArrayList<LoyaltyCard> inputLoyaltyCards) {
        shareText = context.getResources().getString(R.string.intent_import_card_from_url_share_text_plural);
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https");
        uriBuilder.authority(host);
        uriBuilder.path(path);

        uriBuilder.appendQueryParameter(COUNT, Integer.toString(inputLoyaltyCards.size()));

        for (int i = 0; i < inputLoyaltyCards.size(); i++) {
            LoyaltyCard currentCard = inputLoyaltyCards.get(i);
            uriBuilder.appendQueryParameter(STORE + i, currentCard.store);
            uriBuilder.appendQueryParameter(NOTE + i, currentCard.note);
            uriBuilder.appendQueryParameter(CARD_ID + i, currentCard.cardId);
            uriBuilder.appendQueryParameter(BARCODE_TYPE + i, currentCard.barcodeType);

            if (currentCard.headerColor != null) {
                uriBuilder.appendQueryParameter(HEADER_COLOR + i, currentCard.headerColor.toString());
            }

            if (currentCard.headerTextColor != null) {
                uriBuilder.appendQueryParameter(HEADER_TEXT_COLOR + i, currentCard.headerTextColor.toString());
            }
            // Star Status is a personal preferences and thus not exported/imported
        }

        return uriBuilder.build();
    }

    private void startShareIntent(Uri inputURI) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText + "\n" + inputURI.toString());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        context.startActivity(shareIntent);
    }

    public void startShareIntent(LoyaltyCard inputLoyaltyCard) {
        startShareIntent(toUri(inputLoyaltyCard));
    }

    public void startShareIntent(ArrayList<LoyaltyCard> inputLoyaltyCards) {
        startShareIntent(toUri(inputLoyaltyCards));
    }
}
