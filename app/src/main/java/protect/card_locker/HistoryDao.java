package protect.card_locker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert
    void insert(HistoryEntity historyEntity);

    @Query("SELECT * FROM history WHERE timestamp >= :sevenDaysAgo ORDER BY timestamp DESC")
    List<HistoryEntity> getLast7Days(long sevenDaysAgo);

    @Query("DELETE FROM history WHERE timestamp < :sevenDaysAgo")
    void deleteOlderThan(long sevenDaysAgo);

    // Remove debug-sample rows inserted previously (cardId = -1)
    @Query("DELETE FROM history WHERE cardId = :debugCardId")
    void deleteByCardId(int debugCardId);
}
