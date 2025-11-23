package protect.card_locker;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class HistoryActivity extends CatimaAppCompatActivity {
    private static final String TAG = "HistoryActivity";

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

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        TextView tvEmpty = findViewById(R.id.tvEmpty);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        long sevenDaysAgo = calendar.getTimeInMillis();

        new Thread(() -> {
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
