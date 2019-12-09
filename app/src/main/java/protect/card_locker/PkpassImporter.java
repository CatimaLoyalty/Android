package protect.card_locker;

import android.content.Context;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PkpassImporter {
    Context context;

    public PkpassImporter(Context context) {
        this.context = context;
    }

    public boolean isPkpass(String type) {
        return Arrays.asList("application/octet-stream", "application/zip", "application/vnd.apple.pkpass", "application/pkpass", "application/vndapplepkpass", "application/vnd-com.apple.pkpass").contains(type);
    }

    public LoyaltyCard fromURI(Uri uri) throws IOException, JSONException {
        ZipInputStream zipInputStream = new ZipInputStream(context.getContentResolver().openInputStream(uri));

        ZipEntry entry;

        while((entry = zipInputStream.getNextEntry()) != null)
        {
            if(!entry.getName().equals("pass.json"))
            {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            for(int c = zipInputStream.read(); c != -1; c = zipInputStream.read())
            {
                sb.append((char) c);
            }

            String readData = sb.toString();

            JSONObject json = new JSONObject(readData);

            String store = json.getString("description");
            // TODO: Note
            String cardId = json.getJSONObject("barcode").getString("message");
            String barcodeType = json.getJSONObject("barcode").getString("format").substring("PKBarcodeFormat".length());
            if(barcodeType.equals("QR"))
            {
                barcodeType = "QR_CODE";
            }
            return new LoyaltyCard(-1, store, "", cardId, barcodeType, null, null);
        }

        return null;
    }
}
