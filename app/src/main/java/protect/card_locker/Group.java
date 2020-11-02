package protect.card_locker;

import android.database.Cursor;

import java.io.Serializable;

import androidx.annotation.Nullable;

public class Group
{
    public final String name;

    public Group(final String name)
    {
        this.name = name;
    }

    public static Group toGroup(Cursor cursor)
    {
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.NAME));

        return new Group(name);
    }
}
