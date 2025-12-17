package protect.card_locker;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends CatimaAppCompatActivity {

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

        final RecyclerView historyList = findViewById(R.id.history_list);
        final View emptyView = findViewById(R.id.history_empty);

        final AppDatabase db = AppDatabase.getInstance(this);
        new Thread(() -> {
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
