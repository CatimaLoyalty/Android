package protect.card_locker.ui.screens.about

import android.content.Context
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.ui.models.ThirdPartyInfo
import java.io.IOException
import java.util.Calendar

object AboutContent {

    private val thirdPartyLibraries: String
        get() {
            val usedLibraries = mutableListOf<ThirdPartyInfo>()
            usedLibraries.add(
                ThirdPartyInfo(
                    "Color Picker", "https://github.com/jaredrummler/ColorPicker", "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "Commons CSV", "https://commons.apache.org/proper/commons-csv/", "Apache 2.0"
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
                    "uCrop", "https://github.com/Yalantis/uCrop", "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "Zip4j", "https://github.com/srikanth-lingala/zip4j", "Apache 2.0"
                )
            )
            usedLibraries.add(
                ThirdPartyInfo(
                    "ZXing", "https://github.com/zxing/zxing", "Apache 2.0"
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
            usedLibraries.forEach {
                result.append("<br/>").append(it.toHtml())
            }

            return result.toString()
        }

    private val usedThirdPartyAssets: String
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
            usedAssets.forEach {
                result.append("<br/>").append(it.toHtml())
            }

            return result.toString()
        }

    private fun getContributors(context: Context): String {
        return try {
            val contributors = "<br/>" + Utils.readTextFile(context, R.raw.contributors)
            contributors.replace("\n", "<br />")
        } catch (ignored: IOException) {
            return String()
        }
    }

    fun getContributorInfo(context: Context): String {
        val contributorInfo = StringBuilder()
        contributorInfo.append(
            context.getString(R.string.app_copyright_fmt, Calendar.getInstance()[Calendar.YEAR])
        )
        contributorInfo.append("<br/><br/>")
        contributorInfo.append(context.getString(R.string.app_copyright_old))
        contributorInfo.append("<br/><br/>")
        contributorInfo.append(
            context.getString(R.string.app_contributors, getContributors(context))
        )
        contributorInfo.append("<br/><br/>")
        contributorInfo.append(context.getString(R.string.app_libraries, thirdPartyLibraries))
        contributorInfo.append("<br/><br/>")
        contributorInfo.append(context.getString(R.string.app_resources, usedThirdPartyAssets))

        return contributorInfo.toString()
    }
}
