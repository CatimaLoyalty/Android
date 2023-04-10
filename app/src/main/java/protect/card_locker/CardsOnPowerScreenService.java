package protect.card_locker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.controls.Control;
import android.service.controls.ControlsProviderService;
import android.service.controls.DeviceTypes;
import android.service.controls.actions.ControlAction;
import android.service.controls.templates.StatelessTemplate;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.R)
public class CardsOnPowerScreenService extends ControlsProviderService {

    public static final String PREFIX = "catima-";
    static final String TAG = "Catima";
    private SQLiteDatabase mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();

        mDatabase = new DBHelper(this).getReadableDatabase();
    }

    @NonNull
    @Override
    public Flow.Publisher<Control> createPublisherForAllAvailable() {
        Cursor loyaltyCardCursor = DBHelper.getLoyaltyCardCursor(mDatabase, DBHelper.LoyaltyCardArchiveFilter.Unarchived);
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
                                .setCustomIcon(Icon.createWithBitmap(getIcon(this, card)))
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
                Control control;
                Integer cardId = this.controlIdToCardId(controlId);
                LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, cardId);
                if (card != null) {
                    Intent openIntent = new Intent(this, LoyaltyCardViewActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("id", card.id);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), card.id, openIntent, PendingIntent.FLAG_IMMUTABLE);
                    control = new Control.StatefulBuilder(controlId, pendingIntent)
                            .setTitle(card.store)
                            .setDeviceType(DeviceTypes.TYPE_GENERIC_OPEN_CLOSE)
                            .setSubtitle(card.note)
                            .setStatus(Control.STATUS_OK)
                            .setControlTemplate(new StatelessTemplate(controlId))
                            .setCustomIcon(Icon.createWithBitmap(getIcon(this, card)))
                            .build();
                } else {
                    Intent mainScreenIntent = new Intent(this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), -1, mainScreenIntent, PendingIntent.FLAG_IMMUTABLE);
                    control = new Control.StatefulBuilder(controlId, pendingIntent)
                            .setStatus(Control.STATUS_NOT_FOUND)
                            .build();
                }
                Log.d(TAG, "Dispatching widget " + controlId);
                subscriber.onNext(control);
            }
            subscriber.onComplete();
        };
    }

    private Bitmap getIcon(Context context, LoyaltyCard loyaltyCard) {
        Bitmap cardIcon = Utils.retrieveCardImage(context, loyaltyCard.id, ImageLocationType.icon);

        if (cardIcon != null) {
            return cardIcon;
        }

        return Utils.generateIcon(this, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile();
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
        Intent openIntent = new Intent(this, LoyaltyCardViewActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("id", controlIdToCardId(controlId));
        startActivity(openIntent);

        closePowerScreenOnAndroid11();
    }

    @SuppressWarnings({"MissingPermission", "deprecation"})
    private void closePowerScreenOnAndroid11() {
        // Android 12 will auto-close the power screen, but earlier versions won't
        // Lint complains about this but on Android 11 the permission is not needed
        // On Android 12, we don't need it, and Google will probably get angry if we ask for it
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
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
