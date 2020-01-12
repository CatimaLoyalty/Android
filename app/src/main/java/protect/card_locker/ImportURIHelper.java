package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InvalidObjectException;

public class ImportURIHelper {
    private static final String STORE = DBHelper.LoyaltyCardDbIds.STORE;
    private static final String NOTE = DBHelper.LoyaltyCardDbIds.NOTE;
    private static final String CARD_ID = DBHelper.LoyaltyCardDbIds.CARD_ID;
    private static final String BARCODE_TYPE = DBHelper.LoyaltyCardDbIds.BARCODE_TYPE;
    private static final String HEADER_COLOR = DBHelper.LoyaltyCardDbIds.HEADER_COLOR;
    private static final String HEADER_TEXT_COLOR = DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR;
    private static final String ICON = DBHelper.LoyaltyCardDbIds.ICON;
    private static final String EXTRAS = DBHelper.LoyaltyCardDbIds.EXTRAS;

    private final Context context;
    private final String host;
    private final String path;
    private final String shareText;

    public ImportURIHelper(Context context) {
        this.context = context;
        host = context.getResources().getString(R.string.intent_import_card_from_url_host);
        path = context.getResources().getString(R.string.intent_import_card_from_url_path_prefix);
        shareText = context.getResources().getString(R.string.intent_import_card_from_url_share_text);
    }

    public boolean isImportUri(Uri uri) {
        return uri.getHost().equals(host) && uri.getPath().equals(path);
    }

    public LoyaltyCard parse(Uri uri) throws InvalidObjectException {
        if(!isImportUri(uri)) {
            throw new InvalidObjectException("Not an import URI");
        }

        try {
            // These values are allowed to be null
            Integer headerColor = null;
            Integer headerTextColor = null;

            String store = uri.getQueryParameter(STORE);
            String note = uri.getQueryParameter(NOTE);
            String cardId = uri.getQueryParameter(CARD_ID);
            String barcodeType = uri.getQueryParameter(BARCODE_TYPE);
            String unparsedHeaderColor = uri.getQueryParameter(HEADER_COLOR);
            if(unparsedHeaderColor != null)
            {
                headerColor = Integer.parseInt(unparsedHeaderColor);
            }
            String unparsedHeaderTextColor = uri.getQueryParameter(HEADER_TEXT_COLOR);
            if(unparsedHeaderTextColor != null)
            {
                headerTextColor = Integer.parseInt(unparsedHeaderTextColor);
            }
            String iconData = uri.getQueryParameter(ICON);
            Bitmap icon = null;

            if(!iconData.isEmpty())
            {
                byte[] iconBytes = Base64.decode(iconData, Base64.URL_SAFE);
                icon = DBHelper.convertBitmapBlobToBitmap(iconBytes);
            }
            ExtrasHelper extras = new ExtrasHelper().fromJSON(new JSONObject(uri.getQueryParameter(EXTRAS)));
            return new LoyaltyCard(-1, store, note, cardId, barcodeType, headerColor, headerTextColor, icon, extras);
        } catch (NullPointerException | NumberFormatException | JSONException ex) {
            throw new InvalidObjectException("Not a valid import URI");
        }
    }

    // Protected for usage in tests
    protected Uri toUri(LoyaltyCard loyaltyCard) throws JSONException {
        String icon = "";
        if(loyaltyCard.icon != null)
        {
            icon = Base64.encodeToString(DBHelper.convertBitmapToBlob(loyaltyCard.icon), Base64.URL_SAFE);
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https");
        uriBuilder.authority(host);
        uriBuilder.path(path);
        uriBuilder.appendQueryParameter(STORE, loyaltyCard.store);
        uriBuilder.appendQueryParameter(NOTE, loyaltyCard.note);
        uriBuilder.appendQueryParameter(CARD_ID, loyaltyCard.cardId);
        uriBuilder.appendQueryParameter(BARCODE_TYPE, loyaltyCard.barcodeType);
        if(loyaltyCard.headerColor != null)
        {
            uriBuilder.appendQueryParameter(HEADER_COLOR, loyaltyCard.headerColor.toString());
        }
        if(loyaltyCard.headerTextColor != null)
        {
            uriBuilder.appendQueryParameter(HEADER_TEXT_COLOR, loyaltyCard.headerTextColor.toString());
        }
        uriBuilder.appendQueryParameter(ICON, icon);
        uriBuilder.appendQueryParameter(EXTRAS, loyaltyCard.extras.toJSON().toString());

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

    public boolean startShareIntent(LoyaltyCard loyaltyCard) {
        try
        {
            startShareIntent(toUri(loyaltyCard));
            return true;
        }
        catch (JSONException ex) {}

        return false;
    }
}
