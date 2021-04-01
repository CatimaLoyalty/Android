package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

import protect.card_locker.preferences.Settings;

class LoyaltyCardCursorAdapter extends CursorAdapter
{
    Settings settings;

    public LoyaltyCardCursorAdapter(Context context, Cursor cursor)
    {
        super(context, cursor, 0);
        settings = new Settings(context);
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
        ImageView thumbnail = view.findViewById(R.id.thumbnail);
        TextView storeField = view.findViewById(R.id.store);
        TextView noteField = view.findViewById(R.id.note);
        TextView balanceField = view.findViewById(R.id.balance);
        TextView expiryField = view.findViewById(R.id.expiry);
        ImageView star = view.findViewById(R.id.star);

        // Extract properties from cursor
        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(cursor);

        // Populate fields with extracted properties
        storeField.setText(loyaltyCard.store);

        storeField.setTextSize(settings.getFontSizeMax(settings.getMediumFont()));

        if(!loyaltyCard.note.isEmpty())
        {
            noteField.setVisibility(View.VISIBLE);
            noteField.setText(loyaltyCard.note);
            noteField.setTextSize(settings.getFontSizeMax(settings.getSmallFont()));
        }
        else
        {
            noteField.setVisibility(View.GONE);
        }

        if(!loyaltyCard.balance.equals(new BigDecimal("0"))) {
            balanceField.setVisibility(View.VISIBLE);
            balanceField.setText(context.getString(R.string.balanceSentence, Utils.formatBalance(context, loyaltyCard.balance, loyaltyCard.balanceType)));
            balanceField.setTextSize(settings.getFontSizeMax(settings.getSmallFont()));
        }
        else
        {
            balanceField.setVisibility(View.GONE);
        }

        if(loyaltyCard.expiry != null)
        {
            expiryField.setVisibility(View.VISIBLE);
            int expiryString = R.string.expiryStateSentence;
            if(Utils.hasExpired(loyaltyCard.expiry)) {
                expiryString = R.string.expiryStateSentenceExpired;
                expiryField.setTextColor(context.getResources().getColor(R.color.alert));
            }
            expiryField.setText(context.getString(expiryString, DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.expiry)));
            expiryField.setTextSize(settings.getFontSizeMax(settings.getSmallFont()));
        }
        else
        {
            expiryField.setVisibility(View.GONE);
        }

        if (loyaltyCard.starStatus!=0) star.setVisibility(View.VISIBLE);
            else star.setVisibility(View.GONE);

        thumbnail.setImageBitmap(Utils.generateIcon(context, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile());
    }
}
