package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.ManageGroupsActivityBinding;

public class ManageGroupsActivity extends CatimaAppCompatActivity implements GroupCursorAdapter.GroupAdapterListener {
    private ManageGroupsActivityBinding binding;
    private static final String TAG = "Catima";

    private SQLiteDatabase mDatabase;
    private TextView mHelpText;
    private RecyclerView mGroupList;
    GroupCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ManageGroupsActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.groups);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        enableToolbarBackButton();

        mDatabase = new DBHelper(this).getWritableDatabase();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FloatingActionButton addButton = binding.fabAdd;
        addButton.setOnClickListener(v -> createGroup());
        addButton.bringToFront();

        mGroupList = binding.include.list;
        mHelpText = binding.include.helpText;

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
        mAdapter.swapCursor(DBHelper.getGroupCursor(mDatabase));

        if (DBHelper.getGroupCount(mDatabase) == 0) {
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
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);

        // Header
        builder.setTitle(R.string.enter_group_name);

        // Layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int contentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
        params.leftMargin = contentPadding;
        params.topMargin = contentPadding / 2;
        params.rightMargin = contentPadding;

        // EditText with spacing
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLayoutParams(params);
        layout.addView(input);

        // Set layout
        builder.setView(layout);

        // Buttons
        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            DBHelper.insertGroup(mDatabase, input.getText().toString().trim());
            updateGroupList();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();

        // Now that the dialog exists, we can bind something that affects the OK button
        input.addTextChangedListener(new SimpleTextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String groupName = s.toString().trim();

                if (groupName.length() == 0) {
                    input.setError(getString(R.string.group_name_is_empty));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    return;
                }

                if (DBHelper.getGroup(mDatabase, groupName) != null) {
                    input.setError(getString(R.string.group_name_already_in_use));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    return;
                }

                input.setError(null);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });

        dialog.show();

        // Disable button (must be done **after** dialog is shown to prevent crash
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        // Set focus on input field
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    private String getGroupName(View view) {
        TextView groupNameTextView = view.findViewById(R.id.name);
        return (String) groupNameTextView.getText();
    }

    private void moveGroup(View view, boolean up) {
        List<Group> groups = DBHelper.getGroups(mDatabase);
        final String groupName = getGroupName(view);

        int currentIndex = DBHelper.getGroup(mDatabase, groupName).order;
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
        DBHelper.reorderGroups(mDatabase, groups);

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

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.deleteConfirmationGroup);
        builder.setMessage(groupName);

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            DBHelper.deleteGroup(mDatabase, groupName);
            updateGroupList();
            // Delete may change ordering, so invalidate
            invalidateHomescreenActiveTab();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}