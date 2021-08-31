package protect.card_locker;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.controls.Control;
import android.service.controls.ControlsProviderService;
import android.service.controls.DeviceTypes;
import android.service.controls.actions.ControlAction;
import android.service.controls.templates.StatelessTemplate;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

@RequiresApi(Build.VERSION_CODES.R)
public class CardsOnPowerScreenService extends ControlsProviderService {

    public static final String PREFIX = "catima-";
    static final String TAG = "Catima";
    private final DBHelper dbHelper = new DBHelper(this);

    @NonNull
    @Override
    public Flow.Publisher<Control> createPublisherForAllAvailable() {
        Cursor loyaltyCardCursor = dbHelper.getLoyaltyCardCursor();
        return subscriber -> {
            while (loyaltyCardCursor.moveToNext()) {
                LoyaltyCard card = LoyaltyCard.toLoyaltyCard(loyaltyCardCursor);
                Intent openIntent = new Intent(this, LoyaltyCardViewActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("id", card.id);
                PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), card.id, openIntent, PendingIntent.FLAG_IMMUTABLE);
                subscriber.onNext(
                        new Control.StatelessBuilder(PREFIX + card.id, pendingIntent)
                                .setControlId(PREFIX + card.id)
                                .setTitle(card.store)
                                .setDeviceType(DeviceTypes.TYPE_GENERIC_OPEN_CLOSE)
                                .setSubtitle(card.note)
                                .setCustomIcon(Icon.createWithBitmap(Utils.generateIcon(this, card.store, card.headerColor).getLetterTile()))
                                .build()
                );
            }
            subscriber.onComplete();
        };
    }

    @NonNull
    @Override
    public Flow.Publisher<Control> createPublisherFor(@NonNull List<String> controlIds) {
        return subscriber -> {
            subscriber.onSubscribe(new NoOpSubscription());
            for (String controlId : controlIds) {
                Integer cardId = this.controlIdToCardId(controlId);
                if (cardId == null)
                    continue;
                LoyaltyCard card = dbHelper.getLoyaltyCard(cardId);
                Intent openIntent = new Intent(this, LoyaltyCardViewActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("id", card.id);
                PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), card.id, openIntent, PendingIntent.FLAG_IMMUTABLE);
                var ret = new Control.StatefulBuilder(controlId, pendingIntent)
                        .setTitle(card.store)
                        .setDeviceType(DeviceTypes.TYPE_GENERIC_OPEN_CLOSE)
                        .setSubtitle(card.note)
                        .setStatus(Control.STATUS_OK)
                        .setControlTemplate(new StatelessTemplate(controlId))
                        .setCustomIcon(Icon.createWithBitmap(Utils.generateIcon(this, card.store, card.headerColor).getLetterTile()))
                        .build();
                Log.d(TAG, "Dispatching widget " + controlId);
                subscriber.onNext(ret);
            }
            subscriber.onComplete();
        };
    }

    private Integer controlIdToCardId(String controlId) {
        if (controlId == null)
            return null;
        if (!controlId.startsWith(PREFIX)) {
            Log.w(TAG, "Unsupported control ID format: " + controlId);
            return null;
        }
        controlId = controlId.substring(PREFIX.length());
        try {
            return Integer.parseInt(controlId);
        } catch (RuntimeException ex) {
            Log.e(TAG, "Unsupported control ID format. Expected numeric after prefix, found: " + controlId);
            return null;
        }
    }

    @Override
    public void performControlAction(@NonNull String controlId, @NonNull ControlAction action, @NonNull Consumer<Integer> consumer) {
        consumer.accept(ControlAction.RESPONSE_OK);
    }

    /**
     * A no-op subscription.
     * <p>
     * Flow.Subscriptions are made to last during time and receive periodic updates.
     * Our app does not require sending periodic updates of loyalty cards, so we are just ignoring anything in the subscription
     * Also, our db is quick enough to respond that the Publisher is immediately sending and completing data.
     * This facility is overkill, but if we don't call onSubscribe the service won't work
     */
    private static class NoOpSubscription implements Flow.Subscription {
        @Override
        public void request(long l) {
        }

        @Override
        public void cancel() {
        }
    }
}
