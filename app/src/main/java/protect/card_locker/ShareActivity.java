package protect.card_locker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ShareActivity extends CatimaAppCompatActivity {
    private static final String TAG = "ShareActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onSharedIntent();
    }

    private void onSharedIntent() {
        Intent intent = getIntent();
        String receivedAction = intent.getAction();
        String receivedType = intent.getType();

        if (receivedAction.equals(Intent.ACTION_SEND)) {

            if (receivedType.startsWith("image/")) {

                Uri receiveUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                if (receiveUri != null) {
                    BarcodeValues barcodeValues;

                    try {
                        barcodeValues = Utils.parseSetBarcodeActivityResult(Utils.BARCODE_IMPORT_FROM_SHARE_INTENT, -1, intent, this);
                    } catch (NullPointerException e) {
                        Toast.makeText(this, R.string.errorReadingImage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!barcodeValues.isEmpty()) {
                        Intent manualResult = new Intent(ShareActivity.this, LoyaltyCardEditActivity.class);
                        Bundle manualResultBundle = new Bundle();
                        manualResultBundle.putString(LoyaltyCardEditActivity.BUNDLE_CARDID, barcodeValues.content());
                        manualResultBundle.putString(LoyaltyCardEditActivity.BUNDLE_BARCODETYPE, barcodeValues.format());

                        manualResult.putExtras(manualResultBundle);
                        startActivity(manualResult);
                    }
                }
            } else {
                Log.e(TAG, "Wrong mime-type");
            }

        } else if (receivedAction.equals(Intent.ACTION_MAIN)) {

            Log.e(TAG, "onSharedIntent: nothing shared");
        }
        finish();
    }
}
