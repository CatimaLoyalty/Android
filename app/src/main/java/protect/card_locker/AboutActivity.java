package protect.card_locker;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;

import java.util.Calendar;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

public class AboutActivity extends AppCompatActivity
{
    private static final String TAG = "Catima";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final Map<String, String> USED_LIBRARIES = new ImmutableMap.Builder<String, String>()
                .put("Commons CSV", "https://commons.apache.org/proper/commons-csv/")
                .put("Guava", "https://github.com/google/guava")
                .put("ZXing", "https://github.com/zxing/zxing")
                .put("ZXing Android Embedded", "https://github.com/journeyapps/zxing-android-embedded")
                .put("Color Picker", "https://github.com/jaredrummler/ColorPicker")
                .put("NumberPickerPreference", "https://github.com/invissvenska/NumberPickerPreference")
                .build();

        final Map<String, String> USED_ASSETS = ImmutableMap.of
                (
                        "Android icons", "https://www.apache.org/licenses/LICENSE-2.0.txt"
                );

        StringBuilder libs = new StringBuilder().append("<br/>");
        for (Map.Entry<String, String> entry : USED_LIBRARIES.entrySet())
        {
            libs.append("<br/><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a><br/>");
        }

        StringBuilder resources = new StringBuilder().append("<br/>");
        for (Map.Entry<String, String> entry : USED_ASSETS.entrySet())
        {
            resources.append("<br/><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a><br/>");
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

        setTitle(String.format(getString(R.string.about_title_fmt), appName));

        TextView aboutTextView = findViewById(R.id.aboutText);
        aboutTextView.setText(HtmlCompat.fromHtml(String.format(getString(R.string.debug_version_fmt), version) +
                "<br/><br/>" +
                String.format(getString(R.string.app_revision_fmt),
                        "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                                "GitHub" +
                                "</a>") +
                "<br/><br/>" +
                String.format(getString(R.string.app_copyright_fmt), year) +
                "<br/><br/>" +
                getString(R.string.app_copyright_old) +
                "<br/><br/>" +
                getString(R.string.app_license) +
                "<br/><br/>" +
                String.format(getString(R.string.app_libraries), appName, libs.toString()) +
                "<br/><br/>" +
                String.format(getString(R.string.app_resources), appName, resources.toString()), HtmlCompat.FROM_HTML_MODE_COMPACT));
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
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
}