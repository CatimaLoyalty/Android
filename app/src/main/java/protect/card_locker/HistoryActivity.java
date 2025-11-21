package protect.card_locker;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import java.util.Objects;

public class HistoryActivity extends CatimaAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        setTitle(R.string.history_title);

        final DBHelper dbHelper = new DBHelper(this);

        // TODO: Implement the rest of the history activity
    }
}