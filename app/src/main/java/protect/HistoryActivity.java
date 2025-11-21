package protect.card_locker;

import android.os.Bundle;

public class HistoryActivity extends CatimaAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity);
        setTitle(R.string.history_title);

        final DBHelper dbHelper = new DBHelper(this);

        // TODO: Implement the rest of the history activity
    }
}
