package protect.card_locker;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends CatimaAppCompatActivity {
<<<<<<< HEAD
    private static final String TAG = "HistoryActivity";
=======
>>>>>>> 6c35fca7 (making UI compatible)

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

<<<<<<< HEAD
        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        TextView tvEmpty = findViewById(R.id.tvEmpty);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        long sevenDaysAgo = calendar.getTimeInMillis();
=======
        final RecyclerView historyList = findViewById(R.id.history_list);
        final View emptyView = findViewById(R.id.history_empty);
>>>>>>> 6c35fca7 (making UI compatible)

        final AppDatabase db = AppDatabase.getInstance(this);
        new Thread(() -> {
<<<<<<< HEAD
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                // Remove any previously seeded debug-sample rows (cardId = -1)
                db.historyDao().deleteByCardId(-1);
                Log.d(TAG, "Removed any debug-sample history rows (cardId=-1)");
                List<HistoryEntity> historyItems = db.historyDao().getLast7Days(sevenDaysAgo);
                Log.d(TAG, "Loaded history items: " + (historyItems == null ? "null" : historyItems.size()));

                final List<HistoryEntity> finalItems = historyItems;
                runOnUiThread(() -> {
                    if (finalItems == null || finalItems.isEmpty()) {
                        rvHistory.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvHistory.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        rvHistory.setLayoutManager(new LinearLayoutManager(this));
                        rvHistory.setAdapter(new HistoryAdapter(finalItems));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load history", e);
                runOnUiThread(() -> {
                    rvHistory.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                });
            }
=======
            final List<HistoryEntity> history = db.historyDao().getAll();
            runOnUiThread(() -> {
                if (history.isEmpty()) {
                    historyList.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    historyList.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);

                    historyList.setLayoutManager(new LinearLayoutManager(this));
                    historyList.setAdapter(new HistoryAdapter(history));
                }
            });
>>>>>>> 6c35fca7 (making UI compatible)
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
