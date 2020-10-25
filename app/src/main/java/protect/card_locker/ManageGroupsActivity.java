package protect.card_locker;

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                // FIXME: Don't just delete group, create some actual UI flow
                Cursor selected = (Cursor) parent.getItemAtPosition(position);
                Group group = Group.toGroup(selected);

                db.deleteGroup(group.id);
                updateGroupList();
            }
        });
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
}