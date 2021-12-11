package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ManageGroupsActivity extends CatimaAppCompatActivity implements GroupCursorAdapter.GroupAdapterListener {
    private static final String TAG = "Catima";

    private final DBHelper mDb = new DBHelper(this);
    private TextView mHelpText;
    private RecyclerView mGroupList;
    GroupCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.groups);
        setContentView(R.layout.manage_groups_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        FloatingActionButton addButton = findViewById(R.id.fabAdd);
        addButton.setOnClickListener(v -> createGroup());
        addButton.bringToFront();

        mGroupList = findViewById(R.id.list);
        mHelpText = findViewById(R.id.helpText);

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

    private void updateGroupList() {
        mAdapter.swapCursor(mDb.getGroupCursor());

        if (mDb.getGroupCount() == 0) {
            mGroupList.setVisibility(View.GONE);
            mHelpText.setVisibility(View.VISIBLE);

            return;
        }

        mGroupList.setVisibility(View.VISIBLE);
        mHelpText.setVisibility(View.GONE);
    }

    private void invalidateHomescreenActiveTab() {
        SharedPreferences activeTabPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_active_tab),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor activeTabPrefEditor = activeTabPref.edit();
        activeTabPrefEditor.putInt(getString(R.string.sharedpreference_active_tab), 0);
        activeTabPrefEditor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void createGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(R.string.enter_group_name);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            String inputString = input.getText().toString().trim();
            if (inputString.length() == 0) {
                Toast.makeText(getApplicationContext(), R.string.group_name_is_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            if (mDb.getGroup(inputString) != null) {
                Toast.makeText(getApplicationContext(), R.string.group_name_already_in_use, Toast.LENGTH_SHORT).show();
                return;
            }
            mDb.insertGroup(inputString);
            updateGroupList();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    private String getGroupName(View view) {
        TextView groupNameTextView = view.findViewById(R.id.name);
        return (String) groupNameTextView.getText();
    }

    private void moveGroup(View view, boolean up) {
        List<Group> groups = mDb.getGroups();
        final String groupName = getGroupName(view);

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
    public void onMoveDownButtonClicked(View view) {
        moveGroup(view, false);
    }

    @Override
    public void onMoveUpButtonClicked(View view) {
        moveGroup(view, true);
    }

    @Override
    public void onEditButtonClicked(View view) {
        Intent intent = new Intent(this, ManageGroupActivity.class);
        intent.putExtra("group", getGroupName(view));
        startActivity(intent);
    }

    @Override
    public void onDeleteButtonClicked(View view) {
        final String groupName = getGroupName(view);

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