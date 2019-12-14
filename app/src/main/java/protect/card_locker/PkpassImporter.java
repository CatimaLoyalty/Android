package protect.card_locker;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.zxing.BarcodeFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

    private JSONObject appendFieldDictionaryValues(JSONObject original, JSONObject pkpassJSON, String styleKey, String arrayName) throws JSONException
    {
        // https://developer.apple.com/library/archive/documentation/UserExperience/Reference/PassKit_Bundle/Chapters/FieldDictionary.html#//apple_ref/doc/uid/TP40012026-CH4-SW1
        // TODO: Do something with label

        JSONArray fields;
        // These are all optional, so don't throw an exception if they don't exist
        try
        {
            fields = pkpassJSON.getJSONObject(styleKey).getJSONArray(arrayName);
        }
        catch (JSONException ex)
        {
            return original;
        }

        for(int i = 0; i < fields.length(); i++)
        {
            JSONObject fieldObject = fields.getJSONObject(i);
            original.put(fieldObject.getString("key"), fieldObject.getString("value"));
        }

        return original;
    }

    public boolean isPkpass(String type) {
        return Arrays.asList("application/octet-stream", "application/zip", "application/vnd.apple.pkpass", "application/pkpass", "application/vndapplepkpass", "application/vnd-com.apple.pkpass").contains(type);
    }

    public LoyaltyCard fromURI(Uri uri) throws IOException, JSONException {
        return fromInputStream(context.getContentResolver().openInputStream(uri));
    }

    public LoyaltyCard fromInputStream(InputStream inputStream) throws IOException, JSONException
    {
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

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
            try
            {
                headerColor = Color.parseColor(json.getString("backgroundColor"));
            }
            catch (IllegalArgumentException ex) {}
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
            try
            {
                headerTextColor = Color.parseColor(json.getString("labelColor"));
            }
            catch (IllegalArgumentException ex) {}
        }

        // https://developer.apple.com/library/archive/documentation/UserExperience/Reference/PassKit_Bundle/Chapters/TopLevel.html#//apple_ref/doc/uid/TP40012026-CH2-SW6
        // There needs to be exactly one style key

        String styleKey = null;
        ImmutableList<String> possibleStyleKeys = ImmutableList.<String>builder()
                .add("boardingPass")
                .add("coupon")
                .add("eventTicket")
                .add("generic")
                .add("storeCard")
                .build();
        for(int i = 0; i < possibleStyleKeys.size(); i++)
        {
            String possibleStyleKey = possibleStyleKeys.get(i);
            if(json.has(possibleStyleKey))
            {
                styleKey = possibleStyleKey;
                break;
            }
        }

        if(styleKey == null)
        {
            return null;
        }

        // https://developer.apple.com/library/archive/documentation/UserExperience/Reference/PassKit_Bundle/Chapters/LowerLevel.html#//apple_ref/doc/uid/TP40012026-CH3-SW14
        JSONObject extras = new JSONObject();
        appendFieldDictionaryValues(extras, json, styleKey, "headerFields");
        appendFieldDictionaryValues(extras, json, styleKey, "primaryFields");
        appendFieldDictionaryValues(extras, json, styleKey, "secondaryFields");
        appendFieldDictionaryValues(extras, json, styleKey, "auxiliaryFields");
        appendFieldDictionaryValues(extras, json, styleKey, "backFields");

        return new LoyaltyCard(-1, store, note, cardId, barcodeType, headerColor, headerTextColor, extras);
    }
}