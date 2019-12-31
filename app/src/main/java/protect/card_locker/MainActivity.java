package protect.card_locker;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
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
import protect.card_locker.preferences.SettingsActivity;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "LoyaltyCardLocker";
    private static final int MAIN_REQUEST_CODE = 1;

    private Menu menu;
    protected String filter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateLoyaltyCardList("");

        SharedPreferences prefs = getSharedPreferences("protect.card_locker", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            startIntro();
            prefs.edit().putBoolean("firstrun", false).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (menu != null)
        {
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

            if (!searchView.isIconified()) {
                filter = searchView.getQuery().toString();
            }
        }

        updateLoyaltyCardList(filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAIN_REQUEST_CODE)
        {
            // We're coming back from another view so clear the search
            // We only do this now to prevent a flash of all entries right after picking one
            filter = "";
            if (menu != null)
            {
                MenuItem searchItem = menu.findItem(R.id.action_search);
                searchItem.collapseActionView();
            }

            // In case the theme changed
            recreate();
        }
    }

    @Override
    public void onBackPressed() {
        if (menu == null)
        {
            super.onBackPressed();
            return;
        }

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    private void updateLoyaltyCardList(String filterText)
    {
        final ListView cardList = findViewById(R.id.list);
        final TextView helpText = findViewById(R.id.helpText);
        final TextView noMatchingCardsText = findViewById(R.id.noMatchingCardsText);
        final DBHelper db = new DBHelper(this);

        if(db.getLoyaltyCardCount() > 0)
        {
            // We want the cardList to be visible regardless of the filtered match count
            // to ensure that the noMatchingCardsText doesn't end up being shown below
            // the keyboard
            cardList.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
            if(db.getLoyaltyCardCount(filterText) > 0)
            {
                noMatchingCardsText.setVisibility(View.GONE);
            }
            else
            {
                noMatchingCardsText.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            cardList.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);
            noMatchingCardsText.setVisibility(View.GONE);
        }

        Cursor cardCursor = db.getLoyaltyCardCursor(filterText);

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
                i.setAction("");
                final Bundle b = new Bundle();
                b.putInt("id", loyaltyCard.id);
                i.putExtras(b);

                ShortcutHelper.updateShortcuts(MainActivity.this, loyaltyCard, i);

                startActivityForResult(i, MAIN_REQUEST_CODE);
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
        ListView listView = findViewById(R.id.list);

        Cursor cardCursor = (Cursor)listView.getItemAtPosition(info.position);
        LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cardCursor);

        if(card != null)
        {
            if(item.getItemId() == R.id.action_clipboard)
            {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(card.store, card.cardId);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(this, R.string.copy_to_clipboard_toast, Toast.LENGTH_LONG).show();
                return true;
            }
            else if(item.getItemId() == R.id.action_share)
            {
                final ImportURIHelper importURIHelper = new ImportURIHelper(this);
                importURIHelper.startShareIntent(card);
                return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.menu = menu;

        getMenuInflater().inflate(R.menu.main_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setSubmitButtonEnabled(false);

            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    invalidateOptionsMenu();
                    return false;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filter = newText;
                    updateLoyaltyCardList(newText);
                    return true;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_add)
        {
            Intent i = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
            startActivityForResult(i, MAIN_REQUEST_CODE);
            return true;
        }

        if(id == R.id.action_import_export)
        {
            Intent i = new Intent(getApplicationContext(), ImportExportActivity.class);
            startActivityForResult(i, MAIN_REQUEST_CODE);
            return true;
        }

        if(id == R.id.action_settings)
        {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(i, MAIN_REQUEST_CODE);
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
        final Map<String, String> USED_LIBRARIES = new ImmutableMap.Builder<String, String>()
            .put("Commons CSV", "https://commons.apache.org/proper/commons-csv/")
            .put("Guava", "https://github.com/google/guava")
            .put("ZXing", "https://github.com/zxing/zxing")
            .put("ZXing Android Embedded", "https://github.com/journeyapps/zxing-android-embedded")
            .put("AppIntro", "https://github.com/apl-devs/AppIntro")
            .put("Color Picker", "https://github.com/jaredrummler/ColorPicker")
            .put("VNTNumberPickerPreference", "https://github.com/vanniktech/VNTNumberPickerPreference")
            .build();

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

        // Set CSS for dark mode if dark mode
        String css = "";
        Configuration config = getResources().getConfiguration();
        int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if(currentNightMode == Configuration.UI_MODE_NIGHT_YES)
        {
            css = "<style>body {color:white; background-color:black;}</style>";
        }

        String html =
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" +
            css +
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
        startActivityForResult(intent, MAIN_REQUEST_CODE);
    }
}