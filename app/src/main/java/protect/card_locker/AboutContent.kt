package protect.card_locker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.text.HtmlCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AboutContent {

    public static final String TAG = "Catima";

    public Context context;

    public AboutContent(Context context) {
        this.context = context;
    }

    public void destroy() {
        this.context = null;
    }

    public String getPageTitle() {
        return String.format(context.getString(R.string.about_title_fmt), context.getString(R.string.app_name));
    }

    public String getAppVersion() {
        String version = "?";
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package name not found", e);
        }

        return version;
    }

    public int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public String getCopyright() {
        return String.format(context.getString(R.string.app_copyright_fmt), getCurrentYear());
    }

    public String getContributors() {
        String contributors;
        try {
            contributors = "<br/>" + Utils.readTextFile(context, R.raw.contributors);
        }  catch (IOException ignored) {
            return "";
        }
        return contributors.replace("\n", "<br />");
    }

    public String getThirdPartyLibraries() {
        final List<ThirdPartyInfo> usedLibraries = new ArrayList<>();
        usedLibraries.add(new ThirdPartyInfo("Color Picker", "https://github.com/jaredrummler/ColorPicker", "Apache 2.0"));
        usedLibraries.add(new ThirdPartyInfo("Commons CSV", "https://commons.apache.org/proper/commons-csv/", "Apache 2.0"));
        usedLibraries.add(new ThirdPartyInfo("NumberPickerPreference", "https://github.com/invissvenska/NumberPickerPreference", "GNU LGPL 3.0"));
        usedLibraries.add(new ThirdPartyInfo("uCrop", "https://github.com/Yalantis/uCrop", "Apache 2.0"));
        usedLibraries.add(new ThirdPartyInfo("Zip4j", "https://github.com/srikanth-lingala/zip4j", "Apache 2.0"));
        usedLibraries.add(new ThirdPartyInfo("ZXing", "https://github.com/zxing/zxing", "Apache 2.0"));
        usedLibraries.add(new ThirdPartyInfo("ZXing Android Embedded", "https://github.com/journeyapps/zxing-android-embedded", "Apache 2.0"));

        StringBuilder result = new StringBuilder("<br/>");
        for (ThirdPartyInfo entry : usedLibraries) {
            result.append("<br/>")
                .append(entry.toHtml());
        }

        return result.toString();
    }

    public String getUsedThirdPartyAssets() {
        final List<ThirdPartyInfo> usedAssets = new ArrayList<>();
        usedAssets.add(new ThirdPartyInfo("Android icons", "https://fonts.google.com/icons?selected=Material+Icons", "Apache 2.0"));

        StringBuilder result = new StringBuilder().append("<br/>");
        for (ThirdPartyInfo entry : usedAssets) {
            result.append("<br/>")
                    .append(entry.toHtml());
        }

        return result.toString();
    }

    public String getContributorInfo() {
        StringBuilder contributorInfo = new StringBuilder();
        contributorInfo.append(HtmlCompat.fromHtml(String.format(context.getString(R.string.app_contributors), getContributors()), HtmlCompat.FROM_HTML_MODE_COMPACT));
        contributorInfo.append("\n\n");
        contributorInfo.append(context.getString(R.string.app_copyright_old));
        contributorInfo.append("\n\n");
        contributorInfo.append(HtmlCompat.fromHtml(String.format(context.getString(R.string.app_libraries), getThirdPartyLibraries()), HtmlCompat.FROM_HTML_MODE_COMPACT));
        contributorInfo.append("\n\n");
        contributorInfo.append(HtmlCompat.fromHtml(String.format(context.getString(R.string.app_resources), getUsedThirdPartyAssets()), HtmlCompat.FROM_HTML_MODE_COMPACT));

        return contributorInfo.toString();
    }

    public String getVersionHistory() {
        return String.format(context.getString(R.string.debug_version_fmt), getAppVersion());
    }
}
