package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

class LoyaltyCardCursorAdapter extends CursorAdapter
{
    public LoyaltyCardCursorAdapter(Context context, Cursor cursor)
    {
        super(context, cursor, 0);
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        return LayoutInflater.from(context).inflate(R.layout.loyalty_card_layout, parent, false);
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        // Find fields to populate in inflated template
        TextView storeField = (TextView) view.findViewById(R.id.store);
        TextView cardIdField = (TextView) view.findViewById(R.id.cardId);

        // Extract properties from cursor
        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(cursor);

        // Populate fields with extracted properties
        storeField.setText(loyaltyCard.store);

        String cardIdFormat = view.getResources().getString(R.string.cardIdFormat);
        String cardIdLabel = view.getResources().getString(R.string.cardId);
        String cardIdText = String.format(cardIdFormat, cardIdLabel, loyaltyCard.cardId);
        cardIdField.setText(cardIdText);
    }
}
