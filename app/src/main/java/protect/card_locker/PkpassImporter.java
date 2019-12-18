package protect.card_locker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.zxing.BarcodeFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PkpassImporter {
    private Context context;

    private Bitmap icon = null;

    private HashMap<String, HashMap<String, String>> translations = new HashMap<>();

    public PkpassImporter(Context context)
    {
        this.context = context;
    }

    private ByteArrayOutputStream readZipInputStream(ZipInputStream zipInputStream) throws IOException
    {
        byte[] buffer = new byte[2048];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int size;

        while((size = zipInputStream.read(buffer, 0, buffer.length)) != -1)
        {
            byteArrayOutputStream.write(buffer, 0, size);
        }

        return byteArrayOutputStream;
    }

    private void loadBitmap(ByteArrayOutputStream byteArrayOutputStream)
    {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        // Only keep the largest icon
        if(icon != null && bytes.length < icon.getByteCount())
        {
            return;
        }
        icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // FIXME: Probably very fragile
    private void parseTranslations(String language, String iOSTranslationFileContent)
    {
        if(!translations.containsKey(language))
        {
            translations.put(language, new HashMap<String, String>());
        }

        HashMap<String, String> values = translations.get(language);

        for (String entry : iOSTranslationFileContent.trim().split(";"))
        {
            String[] parts = entry.split(" = ", 2);

            // Remove all spaces around the key and value
            String key = parts[0].trim();
            String value = parts[1].trim();

            // iOS .string files quote everything in double quotes, we don't need to keep those
            values.put(key.substring(1, key.length()-1), value.substring(1, value.length()-1));
        }

        translations.put(language, values);
    }

    private ExtrasHelper appendData(ExtrasHelper extrasHelper, JSONObject pkpassJSON, String styleKey, String arrayName) throws JSONException
    {
        // https://developer.apple.com/library/archive/documentation/UserExperience/Reference/PassKit_Bundle/Chapters/FieldDictionary.html#//apple_ref/doc/uid/TP40012026-CH4-SW1

        JSONArray fields;
        // These are all optional, so don't throw an exception if they don't exist
        try
        {
            fields = pkpassJSON.getJSONObject(styleKey).getJSONArray(arrayName);
        }
        catch (JSONException ex)
        {
            return extrasHelper;
        }

        for(int i = 0; i < fields.length(); i++)
        {
            JSONObject fieldObject = fields.getJSONObject(i);
            String key = fieldObject.getString("key");
            String label = fieldObject.getString("label");
            String value = fieldObject.getString("value");

            // Label is optional
            if(label == null)
            {
                label = key;
            }

            // Add the completely untranslated stuff as fallback
            String formattedUntranslatedValue = value;
            if(!label.isEmpty())
            {
                formattedUntranslatedValue = label + ": " + value;
            }
            extrasHelper.addLanguageValue("", key, formattedUntranslatedValue);

            // Try to find translations
            for(Map.Entry<String, HashMap<String, String>> language : translations.entrySet())
            {
                String translatedLabel = label;
                if(language.getValue().containsKey(label))
                {
                    translatedLabel = language.getValue().get(label);
                }

                String translatedValue = value;
                if(language.getValue().containsKey(value))
                {
                    translatedValue = language.getValue().get(value);
                }

                String formattedValue = translatedValue;

                if(!translatedLabel.isEmpty())
                {
                    formattedValue = translatedLabel + ": " + translatedValue;
                }

                extrasHelper.addLanguageValue(language.getKey(), key, formattedValue);
            }
        }

        return extrasHelper;
    }

    public LoyaltyCard fromURI(Uri uri) throws IOException, JSONException {
        return fromInputStream(context.getContentResolver().openInputStream(uri));
    }

    public LoyaltyCard fromInputStream(InputStream inputStream) throws IOException, JSONException
    {
        String passJSONString = null;

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        ZipEntry entry;

        // We first want to parse the translations
        while((entry = zipInputStream.getNextEntry()) != null) {
            if(entry.getName().endsWith("pass.strings"))
            {
                // Example: en.lproj/pass.strings
                String language = entry.getName().substring(0, entry.getName().indexOf("."));

                parseTranslations(language, readZipInputStream(zipInputStream).toString("UTF-8"));
            }
            else if(entry.getName().equals("pass.json"))
            {
                passJSONString = readZipInputStream(zipInputStream).toString("UTF-8");
            }
            else if(entry.getName().equals("icon.png") || entry.getName().equals("icon@2x.png"))
            {
                loadBitmap(readZipInputStream(zipInputStream));
            }
        }

        if(passJSONString == null)
        {
            return null;
        }

        return fromPassJSON(new JSONObject(passJSONString));
    }

    public LoyaltyCard fromPassJSON(JSONObject json) throws JSONException
    {
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
        ExtrasHelper extras = new ExtrasHelper();
        extras = appendData(extras, json, styleKey, "headerFields");
        extras = appendData(extras, json, styleKey, "primaryFields");
        extras = appendData(extras, json, styleKey, "secondaryFields");
        extras = appendData(extras, json, styleKey, "auxiliaryFields");
        extras = appendData(extras, json, styleKey, "backFields");

        return new LoyaltyCard(-1, store, note, cardId, barcodeType, headerColor, headerTextColor, icon, extras);
    }
}