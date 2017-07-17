package protect.card_locker;

import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;

import java.util.Calendar;
import java.util.Map;

import protect.card_locker.intro.IntroActivity;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "LoyaltyCardLocker";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateLoyaltyCardList();

        SharedPreferences prefs = getSharedPreferences("protect.card_locker", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            startIntro();
            prefs.edit().putBoolean("firstrun", false).commit();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        updateLoyaltyCardList();
    }

    private void updateLoyaltyCardList()
    {
        final ListView cardList = (ListView) findViewById(R.id.list);
        final TextView helpText = (TextView) findViewById(R.id.helpText);
        final DBHelper db = new DBHelper(this);

        if(db.getLoyaltyCardCount() > 0)
        {
            cardList.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
        }
        else
        {
            cardList.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);
        }

        Cursor cardCursor = db.getLoyaltyCardCursor();

        final LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(this, cardCursor);
        cardList.setAdapter(adapter);

        registerForContextMenu(cardList);

        cardList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Cursor selected = (Cursor) parent.getItemAtPosition(position);
                LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(selected);

                Intent i = new Intent(view.getContext(), LoyaltyCardViewActivity.class);
                final Bundle b = new Bundle();
                b.putInt("id", loyaltyCard.id);
                b.putBoolean("view", true);
                i.putExtras(b);
                startActivity(i);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.list)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.card_longclick_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ListView listView = (ListView) findViewById(R.id.list);

        Cursor cardCursor = (Cursor)listView.getItemAtPosition(info.position);
        LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cardCursor);

        if(card != null && item.getItemId() == R.id.action_clipboard)
        {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(card.store, card.cardId);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.copy_to_clipboard_toast, Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_add)
        {
            Intent i = new Intent(getApplicationContext(), LoyaltyCardViewActivity.class);
            startActivity(i);
            return true;
        }

        if(id == R.id.action_import_export)
        {
            Intent i = new Intent(getApplicationContext(), ImportExportActivity.class);
            startActivity(i);
            return true;
        }

        if(id == R.id.action_intro)
        {
            startIntro();
            return true;
        }

        if(id == R.id.action_about)
        {
            displayAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayAboutDialog()
    {
        final Map<String, String> USED_LIBRARIES = ImmutableMap.of
        (
            "Commons CSV", "https://commons.apache.org/proper/commons-csv/",
            "Guava", "https://github.com/google/guava",
            "ZXing", "https://github.com/zxing/zxing",
            "ZXing Android Embedded", "https://github.com/journeyapps/zxing-android-embedded",
            "AppIntro", "https://github.com/apl-devs/AppIntro"
        );

        final Map<String, String> USED_ASSETS = ImmutableMap.of
        (
            "Save by Bernar Novalyi", "https://thenounproject.com/term/save/716011"
        );

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : USED_LIBRARIES.entrySet())
        {
            libs.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        libs.append("</ul>");

        StringBuilder resources = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : USED_ASSETS.entrySet())
        {
            resources.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        resources.append("</ul>");

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

        WebView wv = new WebView(this);
        String html =
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" +
            "<img src=\"file:///android_res/mipmap/ic_launcher.png\" alt=\"" + appName + "\"/>" +
            "<h1>" +
            String.format(getString(R.string.about_title_fmt),
                    "<a href=\"" + getString(R.string.app_webpage_url)) + "\">" +
            appName +
            "</a>" +
            "</h1><p>" +
            appName +
            " " +
            String.format(getString(R.string.debug_version_fmt), version) +
            "</p><p>" +
            String.format(getString(R.string.app_revision_fmt),
                    "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                            getString(R.string.app_revision_url) +
                            "</a>") +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_copyright_fmt), year) +
            "</p><hr/><p>" +
            getString(R.string.app_license) +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_libraries), appName, libs.toString()) +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_resources), appName, resources.toString());

        wv.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
        new AlertDialog.Builder(this)
            .setView(wv)
            .setCancelable(true)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            })
            .show();
    }

    private void startIntro()
    {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }
}