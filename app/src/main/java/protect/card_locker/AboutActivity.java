package protect.card_locker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.collection.ArrayMap;
import androidx.core.text.HtmlCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import protect.card_locker.databinding.AboutActivityBinding;

public class AboutActivity extends CatimaAppCompatActivity {

    private static final String TAG = "Catima";

    private AboutActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AboutActivityBinding.inflate(getLayoutInflater());
        setTitle(String.format(getString(R.string.about_title_fmt), getString(R.string.app_name)));
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        enableToolbarBackButton();


        TextView copyright = binding.creditsSub;
        copyright.setText(String.format(getString(R.string.app_copyright_fmt), getCurrentYear()));
        TextView versionHistory = binding.versionHistorySub;
        versionHistory.setText(String.format(getString(R.string.debug_version_fmt), getAppVersion()));

        bindClickListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearClickListeners();
        binding = null;
    }

    private void bindClickListeners() {
        View.OnClickListener openExternalBrowser = view -> (new AboutOpenLinkHandler()).open(this, view);

        binding.versionHistory.setOnClickListener(openExternalBrowser);
        binding.translate.setOnClickListener(openExternalBrowser);
        binding.license.setOnClickListener(openExternalBrowser);
        binding.repo.setOnClickListener(openExternalBrowser);
        binding.privacy.setOnClickListener(openExternalBrowser);
        binding.reportError.setOnClickListener(openExternalBrowser);
        binding.rate.setOnClickListener(openExternalBrowser);

        binding.credits
                .setOnClickListener(view -> new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.credits)
                        .setMessage(getContributorInfo())
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        })
                        .show());
    }

    private void clearClickListeners() {
        binding.versionHistory.setOnClickListener(null);
        binding.translate.setOnClickListener(null);
        binding.license.setOnClickListener(null);
        binding.repo.setOnClickListener(null);
        binding.privacy.setOnClickListener(null);
        binding.reportError.setOnClickListener(null);
        binding.rate.setOnClickListener(null);
        binding.credits.setOnClickListener(null);
    }

    private String getAppVersion() {
        String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package name not found", e);
        }

        return version;
    }

    private int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    private String getContributors() {
        StringBuilder contributors = new StringBuilder().append("<br/>");

        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.contributors), StandardCharsets.UTF_8));

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

    private String getThirdPartyLibraries() {
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

    private String getUsedThirdPartyAssets() {
        final List<ThirdPartyInfo> USED_ASSETS = new ArrayList<>();
        USED_ASSETS.add(new ThirdPartyInfo("Android icons", "https://fonts.google.com/icons?selected=Material+Icons", "Apache 2.0"));

        StringBuilder resources = new StringBuilder().append("<br/>");
        for (ThirdPartyInfo entry : USED_ASSETS) {
            resources.append("<br/><a href=\"").append(entry.url()).append("\">").append(entry.name()).append("</a> (").append(entry.license()).append(")");
        }

        return resources.toString();
    }

    private String getContributorInfo() {
        StringBuilder contributorInfo = new StringBuilder();
        contributorInfo.append(HtmlCompat.fromHtml(String.format(getString(R.string.app_contributors), getContributors()), HtmlCompat.FROM_HTML_MODE_COMPACT));
        contributorInfo.append("\n\n");
        contributorInfo.append(getString(R.string.app_copyright_old));
        contributorInfo.append("\n\n");
        contributorInfo.append(HtmlCompat.fromHtml(String.format(getString(R.string.app_libraries), getThirdPartyLibraries()), HtmlCompat.FROM_HTML_MODE_COMPACT));
        contributorInfo.append("\n\n");
        contributorInfo.append(HtmlCompat.fromHtml(String.format(getString(R.string.app_resources), getUsedThirdPartyAssets()), HtmlCompat.FROM_HTML_MODE_COMPACT));

        return contributorInfo.toString();
    }
}
