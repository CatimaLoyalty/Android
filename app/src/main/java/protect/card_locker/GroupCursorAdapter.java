package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import protect.card_locker.preferences.Settings;

class GroupCursorAdapter extends CursorAdapter
{
    Settings settings;
    DBHelper db;

    public GroupCursorAdapter(Context context, Cursor cursor)
    {
        super(context, cursor, 0);
        settings = new Settings(context);

        db = new DBHelper(context);
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        return LayoutInflater.from(context).inflate(R.layout.group_layout, parent, false);
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        // Find fields to populate in inflated template
        TextView nameField = view.findViewById(R.id.name);
        TextView countField = view.findViewById(R.id.cardCount);

        // Extract properties from cursor
        Group group = Group.toGroup(cursor);

        Integer groupCardCount = db.getGroupCardCount(group._id);

        // Populate fields with extracted properties
        nameField.setText(group._id);

        countField.setText(context.getResources().getQuantityString(R.plurals.groupCardCount, groupCardCount, groupCardCount));

        nameField.setTextSize(settings.getFontSizeMax(settings.getMediumFont()));
        countField.setTextSize(settings.getFontSizeMax(settings.getSmallFont()));
    }
}
