package protect.card_locker;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;

import com.google.common.collect.ImmutableMap;
import com.google.zxing.BarcodeFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PkpassImporter {
    private Context context;

    public PkpassImporter(Context context) {
        this.context = context;
    }

    public boolean isPkpass(String type) {
        return Arrays.asList("application/octet-stream", "application/zip", "application/vnd.apple.pkpass", "application/pkpass", "application/vndapplepkpass", "application/vnd-com.apple.pkpass").contains(type);
    }

    public LoyaltyCard fromURI(Uri uri) throws IOException, JSONException {
        ZipInputStream zipInputStream = new ZipInputStream(context.getContentResolver().openInputStream(uri));

        ZipEntry entry;

        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.getName().equals("pass.json")) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
                sb.append((char) c);
            }

            String readData = sb.toString();

            return fromPassJSON(new JSONObject(readData));
        }

        return null;
    }

    public LoyaltyCard fromPassJSON(JSONObject json) throws JSONException {

        String store = json.getString("organizationName");
        String note = json.getString("description");

        // https://developer.apple.com/library/archive/documentation/UserExperience/Reference/PassKit_Bundle/Chapters/TopLevel.html#//apple_ref/doc/uid/TP40012026-CH2-SW1
        // barcodes is the new field
        // barcode is deprecated, but used on old iOS versions, so we do fall back to it
        JSONObject barcode = null;
        JSONArray barcodes = null;

        try {
            barcodes = json.getJSONArray("barcodes");
        } catch (JSONException ex) {
        }

        if (barcodes != null) {
            barcode = barcodes.getJSONObject(0);
        } else {
            barcode = json.getJSONObject("barcode");
        }

        if (barcode == null) {
            return null;
        }

        String cardId = barcode.getString("message");

        // https://developer.apple.com/library/archive/documentation/UserExperience/Reference/PassKit_Bundle/Chapters/LowerLevel.html#//apple_ref/doc/uid/TP40012026-CH3-SW3
        // Required. Barcode format. For the barcode dictionary, you can use only the following values: PKBarcodeFormatQR, PKBarcodeFormatPDF417, or PKBarcodeFormatAztec. For dictionaries in the barcodes array, you may also use PKBarcodeFormatCode128.
        ImmutableMap<String, String> supportedBarcodeTypes = ImmutableMap.<String, String>builder()
                .put("PKBarcodeFormatQR", BarcodeFormat.QR_CODE.name())
                .put("PKBarcodeFormatPDF417", BarcodeFormat.PDF_417.name())
                .put("PKBarcodeFormatAztec", BarcodeFormat.AZTEC.name())
                .put("PKBarcodeFormatCode128", BarcodeFormat.CODE_128.name())
                .build();

        String barcodeType = supportedBarcodeTypes.get(barcode.getString("format"));

        if(barcodeType == null)
        {
            return null;
        }

        // Prepare to parse colors
        Pattern rgbPattern = Pattern.compile("^rgb\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)$");

        // Optional. Background color of the pass, specified as an CSS-style RGB triple. For example, rgb(23, 187, 82).
        Integer headerColor = null;
        Matcher headerColorMatcher = rgbPattern.matcher(json.getString("backgroundColor"));
        if(headerColorMatcher.find())
        {
            headerColor = Color.rgb(
                    Integer.parseInt(headerColorMatcher.group(1)),
                    Integer.parseInt(headerColorMatcher.group(2)),
                    Integer.parseInt(headerColorMatcher.group(3)));
        }
        if(headerColor == null)
        {
            // Maybe they violate the spec, let's parse it in a format Android understands
            // Necessary for at least Eurowings
            headerColor = Color.parseColor(json.getString("backgroundColor"));
        }


        // Optional. Color of the label text, specified as a CSS-style RGB triple. For example, rgb(255, 255, 255).
        Integer headerTextColor = null;
        Matcher headerTextColorMatcher = rgbPattern.matcher(json.getString("labelColor"));
        if(headerTextColorMatcher.find())
        {
            headerTextColor = Color.rgb(
                Integer.parseInt(headerTextColorMatcher.group(1)),
                Integer.parseInt(headerTextColorMatcher.group(2)),
                Integer.parseInt(headerTextColorMatcher.group(3)));
        }
        if(headerTextColor == null)
        {
            // Maybe they violate the spec, let's parse it in a format Android understands
            // Necessary for at least Eurowings
            headerTextColor = Color.parseColor(json.getString("labelColor"));
        }

        return new LoyaltyCard(-1, store, note, cardId, barcodeType, headerColor, headerTextColor);
    }
}