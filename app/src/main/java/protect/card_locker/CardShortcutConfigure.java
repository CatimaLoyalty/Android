package protect.card_locker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The configuration screen for creating a shortcut.
 */
public class CardShortcutConfigure extends AppCompatActivity implements LoyaltyCardCursorAdapter.CardAdapterListener {
    static final String TAG = "Catima";
    private SQLiteDatabase mDatabase;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mDatabase = new DBHelper(this).getReadableDatabase();

        // Set the result to CANCELED.  This will cause nothing to happen if the
        // aback button is pressed.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.simple_toolbar_list_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.shortcutSelectCard);

        // If there are no cards, bail
        if (DBHelper.getLoyaltyCardCount(mDatabase) == 0) {
            Toast.makeText(this, R.string.noCardsMessage, Toast.LENGTH_LONG).show();
            finish();
        }

        final RecyclerView cardList = findViewById(R.id.list);
        GridLayoutManager layoutManager = (GridLayoutManager) cardList.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.setSpanCount(getResources().getInteger(R.integer.main_view_card_columns));
        }

        Cursor cardCursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        final LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(this, cardCursor, this);
        cardList.setAdapter(adapter);
    }

    private void onClickAction(int position) {
        Cursor selected = DBHelper.getLoyaltyCardCursor(mDatabase);
        selected.moveToPosition(position);
        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(selected);

        Log.d(TAG, "Creating shortcut for card " + loyaltyCard.store + "," + loyaltyCard.id);

        ShortcutInfoCompat shortcut = ShortcutHelper.createShortcutBuilder(CardShortcutConfigure.this, loyaltyCard).build();

        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(CardShortcutConfigure.this, shortcut));

        finish();
    }


    @Override
    public void onRowClicked(int inputPosition) {
        onClickAction(inputPosition);
    }

    @Override
    public void onRowLongClicked(int inputPosition) {
        // do nothing
    }
}
