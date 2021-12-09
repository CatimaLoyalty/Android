package protect.card_locker;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * The configuration screen for creating a shortcut.
 */
public class GroupShortcutConfigure extends AppCompatActivity implements LoyaltyCardCursorAdapter.CardAdapterListener {
    static final String TAG = "Catima";
    final DBHelper mDb = new DBHelper(this);

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Set the result to CANCELED.  This will cause nothing to happen if the
        // aback button is pressed.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.GONE);

        // Hide new button because it won't work here anyway
        FloatingActionButton newFab = findViewById(R.id.fabAdd);
        newFab.setVisibility(View.GONE);

        final DBHelper db = new DBHelper(this);

        // If there are no groups, bail
        if (db.getGroupCount() == 0) {
            Toast.makeText(this, R.string.noGroups, Toast.LENGTH_LONG).show();
            finish();
        }


        final RecyclerView groupList = findViewById(R.id.list);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        groupList.setLayoutManager(mLayoutManager);
        groupList.setItemAnimator(new DefaultItemAnimator());

        groupList.setVisibility(View.VISIBLE);

        Cursor groupCursor = db.getGroupCursor();

        final GroupCursorAdapter adapter = new GroupCursorAdapter(this, groupCursor, (GroupCursorAdapter.GroupAdapterListener) this);
        groupList.setAdapter(adapter);
    }

    private void onClickAction(int position) {
        Cursor selected = mDb.getGroupCursor();
        selected.moveToPosition(position);
        Group group = Group.toGroup(selected);

        Log.d(TAG, "Creating shortcut for group " + group._id + "," + group._id);

        ShortcutInfoCompat shortcut = ShortcutHelper.createGroupShortcutBuilder(GroupShortcutConfigure.this, group).build();

        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(GroupShortcutConfigure.this, shortcut));

        finish();
    }


    @Override
    public void onRowClicked(int inputPosition) {
        onClickAction(inputPosition);
    }

    @Override
    public void onRowLongClicked(int inputPosition) {
        // do nothing
    }
}
