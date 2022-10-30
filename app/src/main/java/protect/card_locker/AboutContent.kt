package protect.card_locker

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.text.HtmlCompat
import java.io.IOException
import java.lang.StringBuilder
import java.util.*

class AboutContent(var context: Context?) {
    fun destroy() {
        context = null
    }

    val pageTitle: String
        get() = String.format(
            context!!.getString(R.string.about_title_fmt),
            context!!.getString(R.string.app_name)
        )
    val appVersion: String
        get() {
            var version = "?"
            try {
                val pi = context!!.packageManager.getPackageInfo(
                    context!!.packageName, 0
                )
                version = pi.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Package name not found", e)
            }
            return version
        }
    val currentYear: Int
        get() = Calendar.getInstance()[Calendar.YEAR]
    val copyright: String
        get() = String.format(context!!.getString(R.string.app_copyright_fmt), currentYear)
    val contributors: String
        get() {
            val contributors: String
            contributors = try {
                "<br/>" + Utils.readTextFile(context, R.raw.contributors)
            } catch (ignored: IOException) {
                return ""
            }
            return contributors.replace("\n", "<br />")
        }
    val thirdPartyLibraries: String
        get() {
            val usedLibraries: MutableList<ThirdPartyInfo> = ArrayList()
            usedLibraries.add(
                ThirdPartyInfo(
                    "Color Picker",
                    "https://github.com/jaredrummler/ColorPicker",
                    "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "Commons CSV",
                    "https://commons.apache.org/proper/commons-csv/",
                    "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "NumberPickerPreference",
                    "https://github.com/invissvenska/NumberPickerPreference",
                    "GNU LGPL 3.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "uCrop",
                    "https://github.com/Yalantis/uCrop",
                    "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "Zip4j",
                    "https://github.com/srikanth-lingala/zip4j",
                    "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "ZXing",
                    "https://github.com/zxing/zxing",
                    "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "ZXing Android Embedded",
                    "https://github.com/journeyapps/zxing-android-embedded",
                    "Apache 2.0"
                )
            )
            val result = StringBuilder("<br/>")
            for (entry in usedLibraries) {
                result.append("<br/>")
                    .append(entry.toHtml())
            }
            return result.toString()
        }
    val usedThirdPartyAssets: String
        get() {
            val usedAssets: MutableList<ThirdPartyInfo> = ArrayList()
            usedAssets.add(
                ThirdPartyInfo(
                    "Android icons",
                    "https://fonts.google.com/icons?selected=Material+Icons",
                    "Apache 2.0"
                )
            )
            val result = StringBuilder().append("<br/>")
            for (entry in usedAssets) {
                result.append("<br/>")
                    .append(entry.toHtml())
            }
            return result.toString()
        }
    val contributorInfo: String
        get() {
            val contributorInfo = StringBuilder()
            contributorInfo.append(
                HtmlCompat.fromHtml(
                    String.format(
                        context!!.getString(R.string.app_contributors),
                        contributors
                    ), HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
            contributorInfo.append("\n\n")
            contributorInfo.append(context!!.getString(R.string.app_copyright_old))
            contributorInfo.append("\n\n")
            contributorInfo.append(
                HtmlCompat.fromHtml(
                    String.format(
                        context!!.getString(R.string.app_libraries),
                        thirdPartyLibraries
                    ), HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
            contributorInfo.append("\n\n")
            contributorInfo.append(
                HtmlCompat.fromHtml(
                    String.format(
                        context!!.getString(R.string.app_resources),
                        usedThirdPartyAssets
                    ), HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
            return contributorInfo.toString()
        }
    val versionHistory: String
        get() = String.format(context!!.getString(R.string.debug_version_fmt), appVersion)

    companion object {
        const val TAG = "Catima"
    }
}