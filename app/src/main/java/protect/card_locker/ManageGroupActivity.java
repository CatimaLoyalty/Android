package protect.card_locker;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.RecyclerView;
import protect.card_locker.preferences.SettingsActivity;

public class ManageGroupActivity extends CatimaAppCompatActivity implements ManageGroupCursorAdapter.CardAdapterListener
{
    private static final String TAG = "Catima";

    private final DBHelper mDB = new DBHelper(this);
    private ManageGroupCursorAdapter mAdapter;
    private ActionMode mCurrentActionMode;
    private Menu mMenu;

    // currently unused
    protected String mFilter = "";
    protected DBHelper.LoyaltyCardOrderDirection mOrderDirection = DBHelper.LoyaltyCardOrderDirection.Ascending;
    protected DBHelper.LoyaltyCardOrder mOrder = DBHelper.LoyaltyCardOrder.Alpha;

    protected Group mGroup = null;
    private RecyclerView mCardList;
    private View mHelpText;
    private View mNoMatchingCardsText;
    private View mNoGroupCardsText;
    private EditText mGroupNameText;
    private TextView mGroupNameLabel;
    private ActionBar mActionBar;

    private HashMap<Integer, Boolean> mAdapterState;
    private String mCurrentGroupName;

    private boolean mGroupNameNotInUse;

    private boolean mDarkMode;

    @Override
    protected void onCreate(Bundle inputSavedInstanceState)
    {
        super.onCreate(inputSavedInstanceState);
        setContentView(R.layout.activity_manage_group);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHelpText = findViewById(R.id.helpText);
        mNoMatchingCardsText = findViewById(R.id.noMatchingCardsText);
        mNoGroupCardsText = findViewById(R.id.noGroupCardsText);
        mCardList = findViewById(R.id.list);

        mGroupNameText = findViewById(R.id.editTextGroupName);
        mGroupNameLabel = findViewById(R.id.textViewEditGroupName);

        mAdapter = new ManageGroupCursorAdapter(this, null, this);
        mCardList.setAdapter(mAdapter);
        registerForContextMenu(mCardList);

        mGroup = null;

        mDarkMode = Utils.isDarkModeEnabled(getApplicationContext());

        if (inputSavedInstanceState != null) {
            ManageGroupActivityInGroupState adapterState = inputSavedInstanceState.getParcelable("mAdapterState");
            if (adapterState != null) {
                mAdapterState = adapterState.getMap();
            }
            mCurrentGroupName = inputSavedInstanceState.getString("mCurrentGroupName");
        }

        mActionBar = getSupportActionBar();
        if (mActionBar == null){
            throw(new RuntimeException("mActionBar is not expected to be null here"));
        }
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
    }

    private void resetGroupNameTextColor() {
        if (mDarkMode) {
            mGroupNameText.setTextColor(Color.WHITE);
        } else{
            mGroupNameText.setTextColor(Color.BLACK);
        }
    }

    private void checkIfGroupNameIsInUse(){
        mGroupNameNotInUse = false;
        String currentGroupName = mGroupNameText.getText().toString();
        if (!mGroup._id.equals(currentGroupName.trim())) {
            Group group = mDB.getGroup(currentGroupName.trim());
            if (group != null) {
                mGroupNameNotInUse = false;
                mGroupNameText.setTextColor(Color.RED);
            } else {
                mGroupNameNotInUse = true;
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Intent intent = getIntent();
        mGroup = intent.getParcelableExtra("group");
        if (mGroup == null){
            throw(new IllegalArgumentException("this activity expects a group loaded into it's intent"));
        }

        setTitle(getString(R.string.edit) + ": " + mGroup._id);

        if (mCurrentGroupName == null){
            mGroupNameText.setText(mGroup._id);
        }else{
            mGroupNameText.setText(mCurrentGroupName);
            checkIfGroupNameIsInUse();
        }

        mGroupNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                return;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                return;
            }

            @Override
            public void afterTextChanged(Editable s) {
                resetGroupNameTextColor();
                checkIfGroupNameIsInUse();
            }
        });

        if (mAdapterState != null){
            mAdapter.importInGroupState(mAdapterState);
        }
        updateLoyaltyCardList();
    }

    @Override
    protected void onSaveInstanceState (Bundle outState){
        super.onSaveInstanceState(outState);
        if(mAdapterState != null){
            ManageGroupActivityInGroupState state = new ManageGroupActivityInGroupState(mAdapterState);
            outState.putParcelable("mAdapterState", state);
        }
        if(mCurrentGroupName != null){
            outState.putString("mCurrentGroupName", mCurrentGroupName);
        }

    }

    @Override
    protected void onPause(){
        super.onPause();

        mAdapterState = mAdapter.exportInGroupState();
        mCurrentGroupName = mGroupNameText.getText().toString();
    }

    private void updateLoyaltyCardList() {
        mAdapter.swapCursor(mDB.getIfLoyaltyCardsAreInGroupCursor(mFilter, mGroup, mOrder, mOrderDirection));

        if(mAdapter.getCountFromCursor() > 0)
        {
            // We want the cardList to be visible regardless of the filtered match count
            // to ensure that the noMatchingCardsText doesn't end up being shown below
            // the keyboard
            mHelpText.setVisibility(View.GONE);
            mNoGroupCardsText.setVisibility(View.GONE);
            if(mAdapter.getItemCount() > 0)
            {
                mCardList.setVisibility(View.VISIBLE);
                mNoMatchingCardsText.setVisibility(View.GONE);
            }
            else
            {
                mCardList.setVisibility(View.GONE);
                if (!mFilter.isEmpty()) {
                    // Actual Empty Search Result
                    mNoMatchingCardsText.setVisibility(View.VISIBLE);
                    mNoGroupCardsText.setVisibility(View.GONE);
                } else {
                    // Group Tab with no Group Cards
                    mNoMatchingCardsText.setVisibility(View.GONE);
                    mNoGroupCardsText.setVisibility(View.VISIBLE);
                }
            }
        }
        else
        {
            mCardList.setVisibility(View.GONE);
            mHelpText.setVisibility(View.VISIBLE);
            mNoMatchingCardsText.setVisibility(View.GONE);
            mNoGroupCardsText.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu inputMenu)
    {
        this.mMenu = inputMenu;

        getMenuInflater().inflate(R.menu.manage_group_menu, inputMenu);

        MenuItem confirmButton = inputMenu.findItem(R.id.action_confirm);
        Drawable icon = confirmButton.getIcon();
        icon.mutate();
        icon.setTint(Color.WHITE);
        confirmButton.setIcon(icon);
        return super.onCreateOptionsMenu(inputMenu);
    }

    private void leaveWithoutSaving(){
        if (hasChanged()){
            AlertDialog.Builder builder = new AlertDialog.Builder(ManageGroupActivity.this);
            builder.setTitle(R.string.discard_changes);
            builder.setMessage(R.string.discard_changes_confirm);
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> finish());
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }else{
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem inputItem)
    {
        int id = inputItem.getItemId();

        if (id == R.id.action_confirm)
        {
            String currentGroupName = mGroupNameText.getText().toString();
            if(!currentGroupName.trim().equals(mGroup._id)){
                if(!mGroupNameNotInUse) {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.group_name_already_in_use, Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
                if(currentGroupName.trim().length() == 0){
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.group_name_is_empty, Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
            }

            mAdapter.commitToDatabase(getApplicationContext(), mGroup._id);
            Toast toast = Toast.makeText(getApplicationContext(), R.string.group_updated, Toast.LENGTH_SHORT);
            if(!currentGroupName.trim().equals(mGroup._id)){
                mDB.updateGroup(mGroup._id, currentGroupName.trim());
            }
            toast.show();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(inputItem);
    }
    @Override
    public void onBackPressed()
    {
        leaveWithoutSaving();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private boolean hasChanged(){
        return mAdapter.hasChanged() || !mGroup._id.equals(mGroupNameText.getText().toString().trim());
    }

    private void setSort(DBHelper.LoyaltyCardOrder order, DBHelper.LoyaltyCardOrderDirection direction) {
        // Update values
        mOrder = order;
        mOrderDirection = direction;

        // Store in Shared Preference to restore next app launch
        SharedPreferences sortPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_sort),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor sortPrefEditor = sortPref.edit();
        sortPrefEditor.putString(getString(R.string.sharedpreference_sort_order), order.name());
        sortPrefEditor.putString(getString(R.string.sharedpreference_sort_direction), direction.name());
        sortPrefEditor.apply();

        // Update card list
        updateLoyaltyCardList();
    }

    @Override
    public void onRowLongClicked(int inputPosition)
    {
        // do nothing for now
        return;
    }

    @Override
    public void onRowClicked(int inputPosition)
    {
        mAdapter.toggleSelection(inputPosition);
    }
}
