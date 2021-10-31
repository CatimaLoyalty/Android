package protect.card_locker;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.MainActivityBinding;

/**
 * The configuration screen for creating a shortcut.
 */
public class CardShortcutConfigure extends AppCompatActivity implements LoyaltyCardCursorAdapter.CardAdapterListener
{
    private MainActivityBinding binding;
    static final String TAG = "Catima";
    final DBHelper mDb = new DBHelper(this);

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        binding = MainActivityBinding.inflate(getLayoutInflater());

        // Set the result to CANCELED.  This will cause nothing to happen if the
        // aback button is pressed.
        setResult(RESULT_CANCELED);

        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        toolbar.setVisibility(View.GONE);

        // Hide new button because it won't work here anyway
        FloatingActionButton newFab = binding.fabAdd;
        newFab.setVisibility(View.GONE);

        final DBHelper db = new DBHelper(this);

        // If there are no cards, bail
        if (db.getLoyaltyCardCount() == 0) {
            Toast.makeText(this, R.string.noCardsMessage, Toast.LENGTH_LONG).show();
            finish();
        }

        final RecyclerView cardList = binding.contentMainLayout.list;

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        cardList.setLayoutManager(mLayoutManager);
        cardList.setItemAnimator(new DefaultItemAnimator());

        cardList.setVisibility(View.VISIBLE);

        Cursor cardCursor = db.getLoyaltyCardCursor();

        final LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(this, cardCursor, this);
        cardList.setAdapter(adapter);
    }

    private void onClickAction(int position) {
        Cursor selected = mDb.getLoyaltyCardCursor();
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
