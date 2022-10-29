package protect.card_locker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.text.HtmlCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    public String getContributors() {
        StringBuilder contributors = new StringBuilder().append("<br/>");

        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.contributors), StandardCharsets.UTF_8));

        try {
            while (true) {
                String tmp = reader.readLine();

                if (tmp == null || tmp.isEmpty()) {
                    reader.close();
                    break;
                }

                contributors.append("<br/>");
                contributors.append(tmp);
            }
        } catch (IOException ignored) {
        }

        return contributors.toString();
    }

    public String getThirdPartyLibraries() {
        final List<ThirdPartyInfo> USED_LIBRARIES = new ArrayList<>();
        USED_LIBRARIES.add(new ThirdPartyInfo("Color Picker", "https://github.com/jaredrummler/ColorPicker", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("Commons CSV", "https://commons.apache.org/proper/commons-csv/", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("NumberPickerPreference", "https://github.com/invissvenska/NumberPickerPreference", "GNU LGPL 3.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("uCrop", "https://github.com/Yalantis/uCrop", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("Zip4j", "https://github.com/srikanth-lingala/zip4j", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("ZXing", "https://github.com/zxing/zxing", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("ZXing Android Embedded", "https://github.com/journeyapps/zxing-android-embedded", "Apache 2.0"));
        StringBuilder libs = new StringBuilder().append("<br/>");
        for (ThirdPartyInfo entry : USED_LIBRARIES) {
            libs.append("<br/><a href=\"").append(entry.url()).append("\">").append(entry.name()).append("</a> (").append(entry.license()).append(")");
        }

        return libs.toString();
    }

    public String getUsedThirdPartyAssets() {
        final List<ThirdPartyInfo> USED_ASSETS = new ArrayList<>();
        USED_ASSETS.add(new ThirdPartyInfo("Android icons", "https://fonts.google.com/icons?selected=Material+Icons", "Apache 2.0"));

        StringBuilder resources = new StringBuilder().append("<br/>");
        for (ThirdPartyInfo entry : USED_ASSETS) {
            resources.append("<br/><a href=\"").append(entry.url()).append("\">").append(entry.name()).append("</a> (").append(entry.license()).append(")");
        }

        return resources.toString();
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
}
