package protect.card_locker;

import android.database.Cursor;

import java.io.Serializable;

import androidx.annotation.Nullable;

public class Group
{
    public final int id;
    public final String name;

    public Group(final int id, final String name)
    {
        this.id = id;
        this.name = name;
    }

    public static Group toGroup(Cursor cursor)
    {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.NAME));

        return new Group(id, name);
    }
}
