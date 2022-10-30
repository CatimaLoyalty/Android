package protect.card_locker

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.text.HtmlCompat
import java.io.IOException
import java.util.*

class AboutContent(var context: Context?) {

    fun destroy() {
        context = null
    }

    val pageTitle: String
        get() {
            val context = context ?: return ""
            return String.format(
                context.getString(R.string.about_title_fmt),
                context.getString(R.string.app_name)
            )
        }

    private val appVersion: String
        get() {
            val context = context ?: return ""
            var version = "?"
            try {
                val pi = context.packageManager.getPackageInfo(
                    context.packageName, 0
                )
                version = pi.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Package name not found", e)
            }
            return version
        }

    private val currentYear: Int = Calendar.getInstance()[Calendar.YEAR]

    val copyright: String
        get() {
        val context = context ?: return ""
        return String.format(context.getString(R.string.app_copyright_fmt), currentYear)
    }

    private val contributors: String
        get() {
            val context = context ?: return ""
            val contributors = try {
                "<br/>" + context.resources.openRawResource(R.raw.contributors)
                    .bufferedReader(Charsets.UTF_8)
                    .readText()
            } catch (ignored: IOException) {
                return ""
            }
            return contributors.replace("\n", "<br />")
        }

    val versionHistory: String
        get() {
            val context = context ?: return ""
            return String.format(context.getString(R.string.debug_version_fmt), appVersion)
        }

    private val thirdPartyLibraries: String
        get() {
            val usedLibraries = listOf(
                ThirdPartyInfo(
                    "Color Picker",
                    "https://github.com/jaredrummler/ColorPicker",
                    "Apache 2.0"
                ),
                ThirdPartyInfo(
                    "Commons CSV",
                    "https://commons.apache.org/proper/commons-csv/",
                    "Apache 2.0"
                ),
                ThirdPartyInfo(
                    "NumberPickerPreference",
                    "https://github.com/invissvenska/NumberPickerPreference",
                    "GNU LGPL 3.0"
                ),
                ThirdPartyInfo(
                    "uCrop",
                    "https://github.com/Yalantis/uCrop",
                    "Apache 2.0"
                ),
                ThirdPartyInfo(
                    "Zip4j",
                    "https://github.com/srikanth-lingala/zip4j",
                    "Apache 2.0"
                ),
                ThirdPartyInfo(
                    "ZXing",
                    "https://github.com/zxing/zxing",
                    "Apache 2.0"
                ),
                ThirdPartyInfo(
                    "ZXing Android Embedded",
                    "https://github.com/journeyapps/zxing-android-embedded",
                    "Apache 2.0"
                )
            )
            return "<br/>" + usedLibraries.joinToString("<br/>") { it.toHtml() }
        }

    private val usedThirdPartyAssets: String
        get() {
            val usedAssets = listOf(
                ThirdPartyInfo(
                    "Android icons",
                    "https://fonts.google.com/icons?selected=Material+Icons",
                    "Apache 2.0"
                )
            )
            return "<br/>" + usedAssets.joinToString("<br/>") { it.toHtml() }
        }

    val contributorInfo: String
        get() {
            val context = context ?: return ""
            val contributors = HtmlCompat.fromHtml(
                String.format(
                    context.getString(R.string.app_contributors),
                    contributors
                ), HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            val copyright = context.getString(R.string.app_copyright_old)
            val thirdPartyLibraries = HtmlCompat.fromHtml(
                String.format(
                    context.getString(R.string.app_libraries),
                    thirdPartyLibraries
                ), HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            val thirdPartyAssets = HtmlCompat.fromHtml(
                String.format(
                    context.getString(R.string.app_resources),
                    usedThirdPartyAssets
                ), HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            return """
$contributors

$copyright

$thirdPartyLibraries

$thirdPartyAssets
            """.trimIndent()
        }

    companion object {
        const val TAG = "Catima"
    }
}