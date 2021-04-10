package protect.card_locker;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * The configuration screen for creating a shortcut.
 */
public class CardShortcutConfigure extends AppCompatActivity
{
    static final String TAG = "Catima";

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

        // Hide new button because it won't work here anyway
        FloatingActionButton newFab = findViewById(R.id.fabAdd);
        newFab.setVisibility(View.GONE);

        final DBHelper db = new DBHelper(this);

        // If there are no cards, bail
        if(db.getLoyaltyCardCount() == 0)
        {
            Toast.makeText(this, R.string.noCardsMessage, Toast.LENGTH_LONG).show();
            finish();
        }

        final ListView cardList = findViewById(R.id.list);
        cardList.setVisibility(View.VISIBLE);

        Cursor cardCursor = db.getLoyaltyCardCursor();

        final LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(this, cardCursor);
        cardList.setAdapter(adapter);

        cardList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Cursor selected = (Cursor) parent.getItemAtPosition(position);
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

                Bitmap icon = Utils.generateIcon(CardShortcutConfigure.this, loyaltyCard.store, loyaltyCard.headerColor, true).getLetterTile();

                ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(CardShortcutConfigure.this, String.valueOf(loyaltyCard.id))
                        .setIntent(shortcutIntent)
                        .setIcon(IconCompat.createWithAdaptiveBitmap(icon))
                        .setShortLabel(loyaltyCard.store)
                        .build();

                Intent intent = ShortcutManagerCompat.createShortcutResultIntent(CardShortcutConfigure.this, shortcutInfo);
                setResult(RESULT_OK, intent);

                finish();
            }
        });
    }
}
