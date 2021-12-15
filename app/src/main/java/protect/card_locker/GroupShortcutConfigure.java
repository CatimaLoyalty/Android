package protect.card_locker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The configuration screen for creating a shortcut.
 */
public class GroupShortcutConfigure extends AppCompatActivity implements GroupSelectCursorAdapter.GroupAdapterListener {
    static final String TAG = "Catima";
    final SQLiteDatabase mDatabase = new DBHelper(this).getReadableDatabase();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Set the result to CANCELED.  This will cause nothing to happen if the
        // aback button is pressed.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.simple_toolbar_list_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.shortcutSelectGroup);

        // If there are no groups, bail
        if (DBHelper.getGroupCount(mDatabase) == 0) {
            Toast.makeText(this, R.string.noGroups, Toast.LENGTH_LONG).show();
            finish();
        }

        final RecyclerView groupList = findViewById(R.id.list);

        Cursor groupCursor = DBHelper.getGroupCursor(mDatabase);
        final GroupSelectCursorAdapter adapter = new GroupSelectCursorAdapter(this, groupCursor,this);
        groupList.setAdapter(adapter);
    }

    private String getGroupName(View view) {
        TextView groupNameTextView = view.findViewById(R.id.name);
        return (String) groupNameTextView.getText();
    }

    private void onClickAction(View view) {
        String groupId = getGroupName(view);
        if (groupId == null) {
            throw (new IllegalArgumentException("The widget expects a group"));
        }
        Log.d("groupId", "groupId: " + groupId);
        Group group = DBHelper.getGroup(mDatabase, groupId);
        if (group == null) {
            throw (new IllegalArgumentException("cannot load group " + groupId + " from database"));
        }

        Log.d(TAG, "Creating shortcut for group " + group._id + "," + group._id);

        ShortcutInfoCompat shortcut = ShortcutHelper.createGroupShortcutBuilder(GroupShortcutConfigure.this, group).build();

        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(GroupShortcutConfigure.this, shortcut));

        finish();
    }

    @Override
    public void onSelectButtonClicked(View view) {
        onClickAction(view);
    }

}
