package protect.card_locker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.ArrayMap
import android.util.Log
import com.google.zxing.BarcodeFormat
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.LocalFileHeader
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigDecimal
import java.text.DateFormat
import java.text.ParseException
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.Currency
import java.util.Date

class PkpassParser(context: Context, uri: Uri?) {
    private var mContext = context

    private var translations: ArrayMap<String, Map<String, String>> = ArrayMap()

    private var passContent: JSONObject? = null

    private var store: String? = null
    private var note: String? = null
    private var validFrom: Date? = null
    private var expiry: Date? = null
    private val balance: BigDecimal = BigDecimal(0)
    private val balanceType: Currency? = null
    private var cardId: String? = null
    private var barcodeId: String? = null
    private var barcodeType: CatimaBarcode? = null
    private var headerColor: Int? = null
    private val starStatus = 0
    private val lastUsed: Long = 0
    private val zoomLevel = DBHelper.DEFAULT_ZOOM_LEVEL
    private val zoomLevelWidth = DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH
    private var archiveStatus = 0

    var image: Bitmap? = null
        private set
    private var logoSize = 0

    init {
        if (passContent != null) {
            throw IllegalStateException("Pkpass instance already initialized!")
        }

        mContext = context

        Log.i(TAG, "Received Pkpass file")
        if (uri == null) {
            Log.e(TAG, "Uri did not contain any data")
            throw IOException(context.getString(R.string.errorReadingFile))
        }

        try {
            mContext.contentResolver.openInputStream(uri).use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var localFileHeader: LocalFileHeader
                    while ((zipInputStream.nextEntry.also { localFileHeader = it }) != null) {
                        // Ignore directories
                        if (localFileHeader.isDirectory) continue

                        // We assume there are three options, as per spec:
                        // language.lproj/pass.strings
                        // file.extension
                        // More directories are ignored
                        val filenameParts = localFileHeader.fileName.split('/')
                        if (filenameParts.size > 2) {
                            continue
                        } else if (filenameParts.size == 2) {
                            // Doesn't seem like a language directory, ignore
                            if (!filenameParts[0].endsWith(".lproj")) continue

                            val locale = filenameParts[0].removeSuffix(".lproj")

                            translations[locale] = parseLanguageStrings(ZipUtils.read(zipInputStream))
                        }

                        // Not a language, parse as normal files
                        when (localFileHeader.fileName) {
                            "logo.png" -> loadImageIfBiggerSize(1, zipInputStream)
                            "logo@2x.png" -> loadImageIfBiggerSize(2, zipInputStream)
                            "logo@3x.png" -> loadImageIfBiggerSize(3, zipInputStream)
                            "pass.json" -> passContent = ZipUtils.readJSON(zipInputStream) // Parse this last, so we're sure we have all language info
                        }
                    }

                    checkNotNull(passContent) { "File lacks pass.json" }
                }
            }
        } catch (e: FileNotFoundException) {
            throw IOException(mContext.getString(R.string.errorReadingFile))
        } catch (e: Exception) {
            throw e
        }
    }

    fun listLocales(): List<String> {
        return translations.keys.toList()
    }

    fun toLoyaltyCard(locale: String?): LoyaltyCard {
        parsePassJSON(checkNotNull(passContent) { "Pkpass instance not yet initialized!" }, locale)

        return LoyaltyCard(
            -1,
            store,
            note,
            validFrom,
            expiry,
            balance,
            balanceType,
            cardId,
            barcodeId,
            barcodeType,
            headerColor,
            starStatus,
            lastUsed,
            zoomLevel,
            zoomLevelWidth,
            archiveStatus,
            image,
            null,
            null,
            null,
            null,
            null
        )
    }

    private fun getTranslation(string: String, locale: String?): String {
        if (locale == null) {
            return string
        }

        val localeStrings = translations[locale]

        return localeStrings?.get(string) ?: string
    }

    private fun loadImageIfBiggerSize(fileLogoSize: Int, zipInputStream: ZipInputStream) {
        if (logoSize < fileLogoSize) {
            image = ZipUtils.readImage(zipInputStream)
            logoSize = fileLogoSize
        }
    }

    private fun parseColor(color: String): Int? {
        // First, try formats supported by Android natively
        try {
            return Color.parseColor(color)
        } catch (ignored: IllegalArgumentException) {}

        // If that didn't work, try parsing it as a rbg(0,0,255) value
        val red: Int;
        val green: Int;
        val blue: Int;

        // Parse rgb(0,0,0) string
        val rgbInfo = Regex("""^rgb\(\s*(?<red>\d+)\s*,\s*(?<green>\d+)\s*,\s*(?<blue>\d+)\s*\)$""").find(color)
        if (rgbInfo == null) {
            return null
        }

        // Convert to integers
        try {
            red = rgbInfo.groups[1]!!.value.toInt()
            green = rgbInfo.groups[2]!!.value.toInt()
            blue = rgbInfo.groups[3]!!.value.toInt()
        } catch (e: NumberFormatException) {
            return null
        }

        // Ensure everything is in a valid range as Color.rgb does not do range checks
        if (red < 0 || red > 255) return null
        if (green < 0 || green > 255) return null
        if (blue < 0 || blue > 255) return null

        return Color.rgb(red, green, blue)
    }

    private fun parseDateTime(dateTime: String): Date {
        return Date.from(ZonedDateTime.parse(dateTime).toInstant())
    }

    private fun parseLanguageStrings(data: String): Map<String, String> {
        val output = ArrayMap<String, String>()

        // Translations look like this:
        // "key_name" = "Translated value";
        //
        // However, "Translated value" may be multiple lines and may contain " (however, it'll be escaped)
        var translationLine = StringBuilder()

        for (line in data.lines()) {
            translationLine.append(line)

            // Make sure we don't have a false ending (this is the escaped double quote: \";)
            if (!line.endsWith("\\\";") and line.endsWith("\";")) {
                // We reached a translation ending, time to parse it

                // 1. Split into key and value
                // 2. Remove cruft of each
                // 3. Clean up escape sequences
                val keyValue = translationLine.toString().split("=", ignoreCase = false, limit = 2)
                val key = keyValue[0].trim().removePrefix("\"").removeSuffix("\"")
                val value = keyValue[1].trim().removePrefix("\"").removeSuffix("\";").replace("\\", "")

                output[key] = value

                translationLine = StringBuilder()
            } else {
                translationLine.append("\n")
            }
        }

        return output
    }

    private fun parsePassJSON(jsonObject: JSONObject, locale: String?) {
        if (jsonObject.getInt("formatVersion") != 1) {
            throw IllegalArgumentException(mContext.getString(R.string.unsupportedFile))
        }

        // Prefer logoText for store, it's generally shorter
        try {
            store = jsonObject.getString("logoText")
        } catch (ignored: JSONException) {}

        if (store.isNullOrEmpty()) {
            store = jsonObject.getString("organizationName")
        }

        val noteText = StringBuilder()
        noteText.append(getTranslation(jsonObject.getString("description"), locale))

        try {
            validFrom = parseDateTime(jsonObject.getString("relevantDate"))
        } catch (ignored: JSONException) {}

        try {
            expiry = parseDateTime(jsonObject.getString("expirationDate"))
        } catch (ignored: JSONException) {}

        try {
            headerColor = parseColor(jsonObject.getString("backgroundColor"))
        } catch (ignored: JSONException) {}

        var pkPassHasBarcodes = false
        var validBarcodeFound = false

        // Create a list of possible barcodes
        val barcodes = ArrayList<JSONObject>()

        // Append the non-deprecated entries
        try {
            val foundInBarcodesField = jsonObject.getJSONArray("barcodes")

            for (i in 0 until foundInBarcodesField.length()) {
                barcodes.add(foundInBarcodesField.getJSONObject(i))
            }
        } catch (ignored: JSONException) {}

        // Append the deprecated entry if it exists
        try {
            barcodes.add(jsonObject.getJSONObject("barcode"))
        } catch (ignored: JSONException) {}

        for (barcode in barcodes) {
            pkPassHasBarcodes = true

            try {
                parsePassJSONBarcodeField(barcode)

                validBarcodeFound = true
                break
            } catch (ignored: IllegalArgumentException) {}
        }

        if (pkPassHasBarcodes && !validBarcodeFound) {
            throw FormatException(mContext.getString(R.string.errorReadingFile))
        }

        // An used card being "archived" probably is the most sensible way to map "voided"
        archiveStatus = try {
            if (jsonObject.getBoolean("voided")) 1 else 0
        } catch (ignored: JSONException) {
            0
        }

        // Append type-specific info to the pass
        noteText.append("\n\n")

        // Find the relevant pass type and parse it
        var hasPassData = false
        for (passType in listOf("boardingPass", "coupon", "eventTicket", "generic")) {
            try {
                noteText.append(
                    parsePassJSONPassFields(
                        jsonObject.getJSONObject(passType),
                        locale
                    )
                )

                hasPassData = true

                break
            } catch (ignored: JSONException) {}
        }

        // Failed to parse anything, error out
        if (!hasPassData) {
            throw FormatException(mContext.getString(R.string.errorReadingFile))
        }

        note = noteText.toString()
    }

    /* Return success or failure */
    private fun parsePassJSONBarcodeField(barcodeInfo: JSONObject) {
        val format = barcodeInfo.getString("format")

        // We only need to check these 4 formats as no other options are valid in the PkPass spec
        barcodeType = when(format) {
            "PKBarcodeFormatQR" -> CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE)
            "PKBarcodeFormatPDF417" -> CatimaBarcode.fromBarcode(BarcodeFormat.PDF_417)
            "PKBarcodeFormatAztec" -> CatimaBarcode.fromBarcode(BarcodeFormat.AZTEC)
            "PKBarcodeFormatCode128" -> CatimaBarcode.fromBarcode(BarcodeFormat.CODE_128)
            else -> throw IllegalArgumentException("No valid barcode type")
        }

        // FIXME: We probably need to do something with the messageEncoding field
        try {
            cardId = barcodeInfo.getString("altText")
            barcodeId = barcodeInfo.getString("message")
        } catch (ignored: JSONException) {
            cardId = barcodeInfo.getString("message")
            barcodeId = null
        }

        // Don't set barcodeId if it's the same as cardId
        if (cardId == barcodeId) {
            barcodeId = null
        }
    }

    private fun parsePassJSONPassFields(fieldsParent: JSONObject, locale: String?): String {
        // These fields contain a lot of info on where we're supposed to display them, but Catima doesn't really have anything for that
        // So for now, throw them all into the description field in a logical order
        val noteContents: MutableList<String> = ArrayList()

        // Collect all the groups of fields that exist
        for (fieldType in listOf("headerFields", "primaryFields", "secondaryFields", "auxiliaryFields", "backFields")) {
            val content = StringBuilder()

            try {
                val fieldArray = fieldsParent.getJSONArray(fieldType)
                for (i in 0 until fieldArray.length()) {
                    val entry = fieldArray.getJSONObject(i)

                    content.append(parsePassJSONPassField(entry, locale))

                    // If this is not the last part, add spacing on the end
                    if (i < (fieldArray.length() - 1)) {
                        content.append("\n")
                    }
                }
            } catch (ignore: JSONException) {
            } catch (ignore: ParseException) {
            }

            if (content.isNotEmpty()) {
                noteContents.add(content.toString())
            }
        }

        // Merge all field groups together, one paragraph for field group
        val output = StringBuilder()

        for (i in 0 until noteContents.size) {
            output.append(noteContents[i])

            // If this is not the last part, add newlines to separate
            if (i < (noteContents.size - 1)) {
                output.append("\n\n")
            }
        }

        return output.toString()
    }

    private fun parsePassJSONPassField(field: JSONObject, locale: String?): String {
        // Value may be a localizable string, a date or a number. So let's try to parse it as a date first

        var value = getTranslation(field.getString("value"), locale)
        try {
            value = DateFormat.getDateTimeInstance().format(parseDateTime(value))
        } catch (ignored: DateTimeParseException) {
            // It's fine if it's not a date
        }

        // FIXME: Use the Android thing for formatted strings here
        if (field.has("currencyCode")) {
            val valueCurrency = Currency.getInstance(field.getString("currencyCode"))

            value = Utils.formatBalance(
                mContext,
                Utils.parseBalance(value, valueCurrency),
                valueCurrency
            )
        } else if (field.has("numberStyle")) {
            if (field.getString("numberStyle") == "PKNumberStylePercent") {
                // FIXME: Android formatting string
                value = "${value}%"
            }
        }

        val label = getTranslation(field.getString("label"), locale)

        if (label.isNotEmpty()) {
            return "$label: $value"
        }

        return value
    }

    companion object {
        private const val TAG = "Catima"
    }
}
