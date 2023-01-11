package protect.card_locker

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.text.HtmlCompat
import java.io.IOException
import java.util.*

interface AboutEntry {
    val title: String
    val description: String
}

data class UrlAboutEntry(
    override val title: String,
    override val description: String,
    val url: String,
) : AboutEntry

data class CreditsAboutEntry(
    override val title: String,
    override val description: String,
) : AboutEntry

class AboutContent(var context: Context?) {

    fun destroy() {
        context = null
    }

    val pageTitle: String = R.string.about_title_fmt.format(R.string.app_name.getString())

    val items: List<AboutEntry>
        get() {
            return listOf(
                UrlAboutEntry(
                    title = R.string.version_history.getString(),
                    description = versionHistory,
                    url = "https://catima.app/changelog/",
                ),
                CreditsAboutEntry(
                    title = R.string.credits.getString(),
                    description = copyright,
                ),
                UrlAboutEntry(
                    title = R.string.help_translate_this_app.getString(),
                    description = R.string.translate_platform.getString(),
                    url = "https://hosted.weblate.org/engage/catima/",
                ),
                UrlAboutEntry(
                    title = R.string.license.getString(),
                    description = R.string.app_license.getString(),
                    url = "https://github.com/CatimaLoyalty/Android/blob/master/LICENSE",
                ),
                UrlAboutEntry(
                    title = R.string.source_repository.getString(),
                    description = R.string.on_github.getString(),
                    url = "https://github.com/CatimaLoyalty/Android/",
                ),
                UrlAboutEntry(
                    title = R.string.privacy_policy.getString(),
                    description = R.string.and_data_usage.getString(),
                    url = "https://catima.app/privacy-policy/",
                ),
                UrlAboutEntry(
                    title = R.string.rate_this_app.getString(),
                    description = R.string.on_google_play.getString(),
                    url = "https://play.google.com/store/apps/details?id=me.hackerchick.catima",
                ),
                UrlAboutEntry(
                    title = R.string.report_error.getString(),
                    description = R.string.on_github.getString(),
                    url = "https://github.com/CatimaLoyalty/Android/issues",
                ),
            )
        }

    private val appVersion: String
        get() {
            val context = context!!
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

    val copyright: String = R.string.app_copyright_fmt.format(currentYear)

    private val contributors: String
        get() {
            val context = context!!
            val contributors = try {
                "<br/>" + context.resources.openRawResource(R.raw.contributors)
                    .bufferedReader(Charsets.UTF_8)
                    .readText()
            } catch (ignored: IOException) {
                return ""
            }
            return contributors.replace("\n", "<br />")
        }

    val versionHistory: String = R.string.debug_version_fmt.format(appVersion)

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
            val context = context!!
            val contributors = R.string.app_contributors
                .format(contributors)
                .toHtml()
            val copyright = context.getString(R.string.app_copyright_old)
            val thirdPartyLibraries = R.string.app_libraries
                .format(thirdPartyLibraries)
                .toHtml()
            val thirdPartyAssets = R.string.app_resources
                .format(usedThirdPartyAssets)
                .toHtml()
            return """
$contributors

$copyright

$thirdPartyLibraries

$thirdPartyAssets
            """.trimIndent()
        }

    private fun Int.getString(): String {
        val context = context!!

        return context.getString(this)
    }

    private fun Int.format(vararg args: Any?): String {
        val context = context!!

        return String.format(context.getString(this), *args)
    }

    private fun String.toHtml(): CharSequence {
        return HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    companion object {
        const val TAG = "Catima"
    }
}