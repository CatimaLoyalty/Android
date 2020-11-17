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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.collect.ImmutableMap;
import java.util.Calendar;
import java.util.Map;
import protect.card_locker.preferences.SettingsActivity;

public class MainActivity extends AppCompatActivity implements LoyaltyCardCursorAdapter.CardAdapterListener
{
    private static final String TAG = "Catima";
    private static final int MAIN_REQUEST_CODE = 1;
    private final DBHelper mDB = new DBHelper(this);
    protected String mFilter = "";
    private ActionMode mCurrentActionMode;
    private Menu mMenu;
    private LoyaltyCardCursorAdapter mAdapter;
    private RecyclerView mCardList;
    private ActionMode.Callback mCurrentActionModeCallback = new ActionMode.Callback()
    {

        @Override
        public boolean onCreateActionMode(ActionMode inputMode, Menu inputMenu)
        {
            inputMode.getMenuInflater().inflate(R.menu.card_longclick_menu, inputMenu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode inputMode, Menu inputMenu)
        {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode inputMode, MenuItem inputItem)
        {
            if (inputItem.getItemId() == R.id.action_copy_to_clipboard)
            {
                String selectedCardsID= mAdapter.getSelectedItemsID();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.card_ids_copied), selectedCardsID);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.copy_to_clipboard_toast, Toast.LENGTH_LONG).show();
                inputMode.finish();
                return true;
            }
            else if (inputItem.getItemId() == R.id.action_share)
            {
//                final ImportURIHelper importURIHelper = new ImportURIHelper(MainActivity.this);
//                importURIHelper.startShareIntent(mCard);
                inputMode.finish();
                return true;
            }
            else if(inputItem.getItemId() == R.id.action_edit)
            {
//                Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
//                Bundle bundle = new Bundle();
//                bundle.putInt("id", mCard.id);
//                bundle.putBoolean("update", true);
//                intent.putExtras(bundle);
//                startActivity(intent);
                inputMode.finish();
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode inputMode)
        {
            mAdapter.clearSelections();
            mCurrentActionMode = null;
            mCardList.post(new Runnable()
            {
                @Override
                public void run()
                {
                    mAdapter.resetAnimationIndex();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle inputSavedInstanceState)
    {
        super.onCreate(inputSavedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateLoyaltyCardList("");
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(mCurrentActionMode != null)
        {
            mAdapter.clearSelections();
            mCurrentActionMode.finish();
        }

        if (mMenu != null)
        {
            SearchView searchView = (SearchView) mMenu.findItem(R.id.action_search).getActionView();

            if (!searchView.isIconified())
            {
                mFilter = searchView.getQuery().toString();
            }
        }

        updateLoyaltyCardList(mFilter);

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
    protected void onActivityResult(int inputRequestCode, int inputResultCode, Intent inputData)
    {
        super.onActivityResult(inputRequestCode, inputResultCode, inputData);

        if (inputRequestCode == MAIN_REQUEST_CODE)
        {
            mFilter = "";
            if (mMenu != null)
            {
                MenuItem searchItem = mMenu.findItem(R.id.action_search);
                searchItem.collapseActionView();
            }
            recreate();
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mMenu == null)
        {
            super.onBackPressed();
            return;
        }

        SearchView searchView = (SearchView) mMenu.findItem(R.id.action_search).getActionView();

        if (!searchView.isIconified())
        {
            searchView.setIconified(true);
        }
        else
            {
            super.onBackPressed();
        }
    }

    private void updateLoyaltyCardList(String inputFilterText)
    {
        mCardList = findViewById(R.id.list);
        RecyclerView.LayoutManager mLayoutManager= new LinearLayoutManager(getApplicationContext());
        mCardList.setLayoutManager(mLayoutManager);
        mCardList.setItemAnimator(new DefaultItemAnimator());

        DividerItemDecoration itemDecorator= new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.list_divider));
        mCardList.addItemDecoration(itemDecorator);

        final TextView helpText = findViewById(R.id.helpText);
        final TextView noMatchingCardsText = findViewById(R.id.noMatchingCardsText);

        if(mDB.getLoyaltyCardCount() > 0)
        {
            // We want the cardList to be visible regardless of the filtered match count
            // to ensure that the noMatchingCardsText doesn't end up being shown below
            // the keyboard
            mCardList.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
            if(mDB.getLoyaltyCardCount(inputFilterText) > 0)
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
            mCardList.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);
            noMatchingCardsText.setVisibility(View.GONE);
        }

        Cursor cardCursor = mDB.getLoyaltyCardCursor(inputFilterText);

        mAdapter = new LoyaltyCardCursorAdapter(this, cardCursor, this);
        mCardList.setAdapter(mAdapter);

        registerForContextMenu(mCardList);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu inputMenu)
    {
        this.mMenu = inputMenu;

        getMenuInflater().inflate(R.menu.main_menu, inputMenu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null)
        {
            SearchView searchView = (SearchView) inputMenu.findItem(R.id.action_search).getActionView();
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
                    mFilter = newText;
                    updateLoyaltyCardList(newText);
                    return true;
                }
            });
        }
        return super.onCreateOptionsMenu(inputMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem inputItem)
    {
        int id = inputItem.getItemId();

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

        return super.onOptionsItemSelected(inputItem);
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

    @Override
    public void onRowLongClicked(int inputPosition)
    {
        enableActionMode(inputPosition);
    }

    private void enableActionMode(int inputPosition)
    {
        if (mCurrentActionMode == null)
        {
            mCurrentActionMode = startSupportActionMode(mCurrentActionModeCallback);
        }
        toggleSelection(inputPosition);
    }

    private void toggleSelection(int inputPosition)
    {
        mAdapter.toggleSelection(inputPosition);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0)
        {
            mCurrentActionMode.finish();
        } else
        {
            mCurrentActionMode.setTitle("Selected: " + count + " Cards");

            mCurrentActionMode.invalidate();
        }
    }

    @Override
    public void onIconClicked(int inputPosition)
    {
        if (mCurrentActionMode == null)
        {
            mCurrentActionMode = startSupportActionMode(mCurrentActionModeCallback);
        }

        toggleSelection(inputPosition);
    }

    @Override
    public void onRowClicked(int inputPosition)
    {

        if (mAdapter.getSelectedItemCount() > 0)
        {
            enableActionMode(inputPosition);
        }
        else
        {
            Cursor selected = mDB.getLoyaltyCardCursor();
            selected.moveToPosition(inputPosition);
            LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(selected);

            Intent i = new Intent(this, LoyaltyCardViewActivity.class);
            i.setAction("");
            final Bundle b = new Bundle();
            b.putInt("id", loyaltyCard.id);
            i.putExtras(b);

            ShortcutHelper.updateShortcuts(MainActivity.this, loyaltyCard, i);

            startActivityForResult(i, MAIN_REQUEST_CODE);
        }
    }

}