package protect.card_locker;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.collect.ImmutableMap;
import java.util.Calendar;
import java.util.Map;
import protect.card_locker.preferences.SettingsActivity;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "Catima";
    private static final int MAIN_REQUEST_CODE = 1;
    LoyaltyCard card;
    private ActionMode currentActionMode;
    private Menu menu;
    protected String filter = "";
    private ActionMode.Callback currentActionModeCallback = new ActionMode.Callback()
    {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            mode.getMenuInflater().inflate(R.menu.card_longclick_menu, menu);
            mode.setTitle(getString(R.string.card_selected) + card.store);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            if (item.getItemId() == R.id.action_copy_to_clipboard)
            {
                mode.finish();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(card.store, card.cardId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.copy_to_clipboard_toast, Toast.LENGTH_LONG).show();
                return true;
            }
            else if (item.getItemId() == R.id.action_share)
            {
                mode.finish();
                final ImportURIHelper importURIHelper = new ImportURIHelper(MainActivity.this);
                importURIHelper.startShareIntent(card);
                return true;
            }
            else if(item.getItemId() == R.id.action_edit)
            {
                mode.finish();
                Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("id", card.id);
                bundle.putBoolean("update", true);
                intent.putExtras(bundle);
                startActivity(intent);
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            currentActionMode= null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateLoyaltyCardList("");

        ListView listView = findViewById(R.id.list);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (currentActionMode != null)
                {
                    return false;
                }

                ListView listView = findViewById(R.id.list);
                Cursor cardCursor = (Cursor) listView.getItemAtPosition(position);
                card = LoyaltyCard.toLoyaltyCard(cardCursor);

                currentActionMode = startSupportActionMode(currentActionModeCallback);
                return true;
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (menu != null)
        {
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

            if (!searchView.isIconified())
            {
                filter = searchView.getQuery().toString();
            }
        }

        updateLoyaltyCardList(filter);

        FloatingActionButton addButton = findViewById(R.id.fabAdd);
        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent i = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
                startActivityForResult(i, MAIN_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
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
    public void onBackPressed()
    {
        if (menu == null)
        {
            super.onBackPressed();
            return;
        }

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        if (!searchView.isIconified())
        {
            searchView.setIconified(true);
        }
        else
            {
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.menu = menu;

        getMenuInflater().inflate(R.menu.main_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null)
        {
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setSubmitButtonEnabled(false);

            searchView.setOnCloseListener(new SearchView.OnCloseListener()
            {
                @Override
                public boolean onClose()
                {
                    invalidateOptionsMenu();
                    return false;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
            {
                @Override
                public boolean onQueryTextSubmit(String query)
                {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText)
                {
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

        if (id == R.id.action_import_export)
        {
            Intent i = new Intent(getApplicationContext(), ImportExportActivity.class);
            startActivityForResult(i, MAIN_REQUEST_CODE);
            return true;
        }

        if (id == R.id.action_settings)
        {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(i, MAIN_REQUEST_CODE);
            return true;
        }

        if (id == R.id.action_about)
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
        if(isDarkModeEnabled(this))
        {
            css = "<style>body {color:white; background-color:black;}</style>";
        }

        String html =
                "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" +
                        css +
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
                                        "GitHub" +
                                        "</a>") +
                        "</p><hr/><p>" +
                        String.format(getString(R.string.app_copyright_fmt), year) +
                        "</p><p>" +
                        getString(R.string.app_copyright_old) +
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

    protected static boolean isDarkModeEnabled(Context inputContext)
    {
        Configuration config = inputContext.getResources().getConfiguration();
        int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
    }

}