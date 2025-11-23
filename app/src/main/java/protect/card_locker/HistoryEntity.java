package protect.card_locker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int cardId;
    public String cardName;
    public long timestamp; // in milliseconds
}
