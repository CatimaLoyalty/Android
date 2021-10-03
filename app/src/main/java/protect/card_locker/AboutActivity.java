package protect.card_locker;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.text.HtmlCompat;

public class AboutActivity extends CatimaAppCompatActivity implements View.OnClickListener
{
    private static final String TAG = "Catima";
    ConstraintLayout version_history, translate, license, repo, privacy, error, credits, rate;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle(R.string.about);
        setContentView(R.layout.about_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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
        } catch (IOException ignored) {}

        final List<ThirdPartyInfo> USED_LIBRARIES = new ArrayList<>();
        USED_LIBRARIES.add(new ThirdPartyInfo("Color Picker", "https://github.com/jaredrummler/ColorPicker", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("Commons CSV", "https://commons.apache.org/proper/commons-csv/", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("NumberPickerPreference", "https://github.com/invissvenska/NumberPickerPreference", "GNU LGPL 3.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("Zip4j", "https://github.com/srikanth-lingala/zip4j", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("ZXing", "https://github.com/zxing/zxing", "Apache 2.0"));
        USED_LIBRARIES.add(new ThirdPartyInfo("ZXing Android Embedded", "https://github.com/journeyapps/zxing-android-embedded", "Apache 2.0"));

        final List<ThirdPartyInfo> USED_ASSETS = new ArrayList<>();
        USED_ASSETS.add(new ThirdPartyInfo("Android icons", "https://fonts.google.com/icons?selected=Material+Icons", "Apache 2.0"));

        StringBuilder libs = new StringBuilder().append("<br/>");
        for (ThirdPartyInfo entry : USED_LIBRARIES)
        {
            libs.append("<br/><a href=\"").append(entry.url()).append("\">").append(entry.name()).append("</a> (").append(entry.license()).append(")");
        }

        StringBuilder resources = new StringBuilder().append("<br/>");
        for (ThirdPartyInfo entry : USED_ASSETS)
        {
            resources.append("<br/><a href=\"").append(entry.url()).append("\">").append(entry.name()).append("</a> (").append(entry.license()).append(")");
        }

        String appName = getString(R.string.app_name);
        int year = Calendar.getInstance().get(Calendar.YEAR);

        String version = "?";
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.w(TAG, "Package name not found", e);
        }

        TextView copyright = findViewById(R.id.copyright);
        copyright.setText(String.format(getString(R.string.app_copyright_fmt), year));
        TextView vHistory = findViewById(R.id.history);
        vHistory.setText(String.format(getString(R.string.debug_version_fmt), version));

        setTitle(String.format(getString(R.string.about_title_fmt), appName));

        version_history = findViewById(R.id.version_history);
        translate = findViewById(R.id.translate);
        license = findViewById(R.id.License);
        repo = findViewById(R.id.repo);
        privacy = findViewById(R.id.privacy);
        error = findViewById(R.id.report_error);
        credits = findViewById(R.id.credits);
        rate = findViewById(R.id.rate);

        version_history.setOnClickListener(this);
        translate.setOnClickListener(this);
        license.setOnClickListener(this);
        repo.setOnClickListener(this);
        privacy.setOnClickListener(this);
        error.setOnClickListener(this);
        rate.setOnClickListener(this);

        StringBuilder contributorInfo = new StringBuilder();
        contributorInfo.append(HtmlCompat.fromHtml(String.format(getString(R.string.app_contributors), contributors.toString()),
                HtmlCompat.FROM_HTML_MODE_COMPACT));
        credits.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.credits)
                .setMessage(contributorInfo.toString())
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {})
                .show());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (R.id.version_history == view.getId()) {
            String url = "https://catima.app/changelog/";
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } else if (R.id.translate == view.getId()) {
            String translateUrl = "https://hosted.weblate.org/engage/catima/";
            intent.setData(Uri.parse(translateUrl));
            startActivity(intent);
        } else if (R.id.License == view.getId()) {
            String licenseUrl = "https://github.com/TheLastProject/Catima/blob/master/LICENSE";
            intent.setData(Uri.parse(licenseUrl));
            startActivity(intent);
        } else if (R.id.repo == view.getId()) {
            String repo = "https://github.com/TheLastProject/Catima/";
            intent.setData(Uri.parse(repo));
            startActivity(intent);
        } else if (R.id.privacy == view.getId()) {
            String privacy = "https://catima.app/privacy-policy/";
            intent.setData(Uri.parse(privacy));
            startActivity(intent);
        } else if (R.id.report_error == view.getId()) {
            String errorUrl = "https://github.com/TheLastProject/Catima/issues";
            intent.setData(Uri.parse(errorUrl));
            startActivity(intent);
        } else if (R.id.rate == view.getId()) {
            String rateUrl = "https://play.google.com/store/apps/details?id=me.hackerchick.catima";
            intent.setData(Uri.parse(rateUrl));
            startActivity(intent);
        }
    }

}
