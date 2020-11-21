package protect.card_locker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ManageGroupsActivity extends AppCompatActivity
{
    private static final String TAG = "Catima";

    private AlertDialog newGroupDialog;
    private final DBHelper db = new DBHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_groups_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        newGroupDialog = createNewGroupDialog();

        updateGroupList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateGroupList();

        FloatingActionButton addButton = findViewById(R.id.fabAdd);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGroupDialog.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void updateGroupList()
    {
        final ListView groupList = findViewById(R.id.list);
        final TextView helpText = findViewById(R.id.helpText);
        final DBHelper db = new DBHelper(this);

        if(db.getGroupCount() > 0)
        {
            groupList.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
        }
        else
        {
            groupList.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);
        }

        Cursor groupCursor = db.getGroupCursor();

        final GroupCursorAdapter adapter = new GroupCursorAdapter(this, groupCursor);
        groupList.setAdapter(adapter);

        registerForContextMenu(groupList);
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

    public void moveGroupUp(View view) {
        moveGroup(view, true);
    }

    public void moveGroupDown(View view) {
        moveGroup(view, false);
    }

    public void editGroup(View view) {
        final String groupName = getGroupname(view);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_group_name);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(groupName);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.updateGroup(groupName, input.getText().toString());
                updateGroupList();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void deleteGroup(View view) {
        final String groupName = getGroupname(view);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.deleteConfirmationGroup);
        builder.setMessage(groupName);

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.deleteGroup(groupName);
                updateGroupList();
                // Delete may change ordering, so invalidate
                invalidateHomescreenActiveTab();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private AlertDialog createNewGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_group_name);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.insertGroup(input.getText().toString());
                updateGroupList();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();

        return dialog;
    }

    private String getGroupname(View view) {
        LinearLayout parentRow = (LinearLayout) view.getParent().getParent();
        TextView groupNameTextView = parentRow.findViewById(R.id.name);
        return (String) groupNameTextView.getText();
    }

    private void moveGroup(View view, boolean up) {
        final String groupName = getGroupname(view);

        List<Group> groups = db.getGroups();

        int currentIndex = -1;
        Integer newIndex;

        // Get current index in group list
        for (int i = 0; i < groups.size(); i++) {
            Log.w(TAG, groupName);
            if (groups.get(i)._id.equals(groupName)) {
                currentIndex = i;

                break;
            }
        }

        if (currentIndex == -1) {
            throw new IndexOutOfBoundsException();
        }

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
        db.reorderGroups(groups);

        // Update UI
        updateGroupList();

        // Ordering may have changed, so invalidate
        invalidateHomescreenActiveTab();
    }
}