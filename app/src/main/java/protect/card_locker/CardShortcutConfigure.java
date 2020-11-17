package protect.card_locker;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The configuration screen for creating a shortcut.
 */
public class CardShortcutConfigure extends AppCompatActivity implements LoyaltyCardCursorAdapter.CardAdapterListener
{
    static final String TAG = "Catima";
    final DBHelper db= new DBHelper(this);

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        // Set the result to CANCELED.  This will cause nothing to happen if the
        // aback button is pressed.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.GONE);

        // If there are no cards, bail
        if(db.getLoyaltyCardCount() == 0)
        {
            Toast.makeText(this, R.string.noCardsMessage, Toast.LENGTH_LONG).show();
            finish();
        }

        final RecyclerView cardList = findViewById(R.id.list);
        cardList.setVisibility(View.VISIBLE);

        Cursor cardCursor = db.getLoyaltyCardCursor();

        final LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(this, cardCursor, this);
        cardList.setAdapter(adapter);

    }

    private void onClickAction(int position) {
        Cursor selected = db.getLoyaltyCardCursor();
        selected.moveToPosition(position);
        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(selected);

        Log.d(TAG, "Creating shortcut for card " + loyaltyCard.store + "," + loyaltyCard.id);

        Intent shortcutIntent = new Intent(CardShortcutConfigure.this, LoyaltyCardViewActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        // Prevent instances of the view activity from piling up; if one exists let this
        // one replace it.
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Bundle bundle = new Bundle();
        bundle.putInt("id", loyaltyCard.id);
        bundle.putBoolean("view", true);
        shortcutIntent.putExtras(bundle);

        Parcelable icon = Intent.ShortcutIconResource.fromContext(CardShortcutConfigure.this, R.mipmap.ic_launcher);
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, loyaltyCard.store);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    public void onIconClicked(int inputPosition) {
        onClickAction(inputPosition);
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
