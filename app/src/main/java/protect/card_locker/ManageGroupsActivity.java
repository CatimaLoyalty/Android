package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.ManageGroupsActivityBinding;

public class ManageGroupsActivity extends CatimaAppCompatActivity implements GroupCursorAdapter.GroupAdapterListener
{
    private ManageGroupsActivityBinding binding;
    private static final String TAG = "Catima";

    private final DBHelper mDb = new DBHelper(this);
    private TextView mHelpText;
    private RecyclerView mGroupList;
    GroupCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ManageGroupsActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.groups);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        FloatingActionButton addButton = binding.fabAdd;
        addButton.setOnClickListener(v -> createGroup());
        addButton.bringToFront();

        mGroupList = binding.groupMainLayout.list;
        mHelpText = binding.groupMainLayout.helpText;

        // Init group list
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mGroupList.setLayoutManager(mLayoutManager);
        mGroupList.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new GroupCursorAdapter(this, null, this);
        mGroupList.setAdapter(mAdapter);

        updateGroupList();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void updateGroupList()
    {
        mAdapter.swapCursor(mDb.getGroupCursor());

        if (mDb.getGroupCount() == 0) {
            mGroupList.setVisibility(View.GONE);
            mHelpText.setVisibility(View.VISIBLE);

            return;
        }

        mGroupList.setVisibility(View.VISIBLE);
        mHelpText.setVisibility(View.GONE);
    }

    private void invalidateHomescreenActiveTab()
    {
        SharedPreferences activeTabPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_active_tab),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor activeTabPrefEditor = activeTabPref.edit();
        activeTabPrefEditor.putInt(getString(R.string.sharedpreference_active_tab), 0);
        activeTabPrefEditor.apply();
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

    private void createGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);
        builder.setTitle(R.string.enter_group_name);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            mDb.insertGroup(input.getText().toString());
            updateGroupList();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    private void moveGroup(TextView name, boolean up) {
        List<Group> groups = mDb.getGroups();
        final String groupName = (String) name.getText();

        int currentIndex = mDb.getGroup(groupName).order;
        int newIndex;

        // Reinsert group in correct position
        if (up) {
            newIndex = currentIndex - 1;
        } else {
            newIndex = currentIndex + 1;
        }

        // Don't try to move out of bounds
        if (newIndex < 0 || newIndex >= groups.size()) {
            return;
        }

        Group group = groups.remove(currentIndex);
        groups.add(newIndex, group);

        // Update database
        mDb.reorderGroups(groups);

        // Update UI
        updateGroupList();

        // Ordering may have changed, so invalidate
        invalidateHomescreenActiveTab();
    }

    @Override
    public void onMoveDownButtonClicked(View view, TextView name) {
        moveGroup(name, false);
    }

    @Override
    public void onMoveUpButtonClicked(View view, TextView name) {
        moveGroup(name, true);
    }

    @Override
    public void onEditButtonClicked(View view, TextView name) {
        final String groupName = (String) name.getText();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_group_name);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(groupName);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            String newGroupName = input.getText().toString();
            mDb.updateGroup(groupName, newGroupName);
            updateGroupList();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    @Override
    public void onDeleteButtonClicked(View view, TextView name) {
        final String groupName = (String) name.getText();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.deleteConfirmationGroup);
        builder.setMessage(groupName);

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            mDb.deleteGroup(groupName);
            updateGroupList();
            // Delete may change ordering, so invalidate
            invalidateHomescreenActiveTab();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}