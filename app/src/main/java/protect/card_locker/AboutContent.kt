package protect.card_locker

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.IOException
import java.util.Calendar

object AboutContent {
    const val TAG: String = "Catima"

    fun getPageTitle(context: Context): String {
        return String.format(
            context.getString(R.string.about_title_fmt),
            context.getString(R.string.app_name)
        )
    }

    fun getAppVersion(context: Activity): String? {
        var version: String? = "?"
        try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            version = pi.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package name not found", e)
        }

        return version
    }

    val currentYear: Int
        get() = Calendar.getInstance().get(Calendar.YEAR)

    fun getCopyright(context: Context): String {
        return String.format(
            context.getString(R.string.app_copyright_fmt),
            this.currentYear
        )
    }

    fun getCopyrightShort(context: Context): String {
        return context.getString(R.string.app_copyright_short)
    }

    fun getContributorsHtml(context: Context): String {
        val contributors: String?
        try {
            contributors = "<br/>" + Utils.readTextFile(context, R.raw.contributors)
        } catch (_: IOException) {
            return ""
        }
        return contributors.replace("\n", "<br />")
    }

    fun getHistoryHtml(context: Context): String {
        val versionHistory: String?
        try {
            versionHistory = Utils.readTextFile(context, R.raw.changelog)
                .replace("# Changelog\n\n", "")
        } catch (_: IOException) {
            return ""
        }
        return Utils.linkify(Utils.basicMDToHTML(versionHistory))
            .replace("\n", "<br />")
    }

    fun getLicenseHtml(context: Context): String {
        return try {
            Utils.readTextFile(context, R.raw.license)
        } catch (_: IOException) {
            ""
        }
    }

    fun getPrivacyHtml(context: Context): String {
        val privacyPolicy: String?
        try {
            privacyPolicy = Utils.readTextFile(context, R.raw.privacy)
                .replace("# Privacy Policy\n", "")
        } catch (_: IOException) {
            return ""
        }
        return Utils.linkify(Utils.basicMDToHTML(privacyPolicy))
            .replace("\n", "<br />")
    }

    val thirdPartyLibrariesHtml: String
        get() {
            val usedLibraries: MutableList<ThirdPartyInfo> =
                ArrayList()
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

    val usedThirdPartyAssetsHtml: String
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

    fun getContributorInfoHtml(context: Context): String {
        return getCopyright(context) +
                "<br/><br/>" +
                context.getString(R.string.app_copyright_old) +
                "<br/><br/>" + String.format(
            context.getString(R.string.app_contributors),
            getContributorsHtml(context)
        ) +
                "<br/><br/>" + String.format(
            context.getString(R.string.app_libraries),
            this.thirdPartyLibrariesHtml
        ) +
                "<br/><br/>" + String.format(
            context.getString(R.string.app_resources),
            this.usedThirdPartyAssetsHtml
        )
    }

    fun getVersionHistory(context: Activity): String {
        return String.format(context.getString(R.string.debug_version_fmt), getAppVersion(context))
    }
}
