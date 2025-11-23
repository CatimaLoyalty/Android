package protect.card_locker;

import android.os.Bundle;
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

    private AppDatabase appDatabase;
    private HistoryDao historyDao;

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

        appDatabase = AppDatabase.getInstance(this);
        historyDao = appDatabase.historyDao();

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        TextView tvEmpty = findViewById(R.id.tvEmpty);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        long sevenDaysAgo = calendar.getTimeInMillis();

        new Thread(() -> {
            List<HistoryEntity> historyItems = historyDao.getLast7Days(sevenDaysAgo);
            runOnUiThread(() -> {
                if (historyItems.isEmpty()) {
                    rvHistory.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvHistory.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    rvHistory.setLayoutManager(new LinearLayoutManager(this));
                    rvHistory.setAdapter(new HistoryAdapter(historyItems));
                }
            });
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
