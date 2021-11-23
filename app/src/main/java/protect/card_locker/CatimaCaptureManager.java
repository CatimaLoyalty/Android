package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CatimaCaptureManager extends CaptureManager {
    private final Context mContext;

    public CatimaCaptureManager(Activity activity, DecoratedBarcodeView barcodeView) {
        super(activity, barcodeView);

        mContext = activity.getApplicationContext();
    }

    @Override
    protected void displayFrameworkBugMessageAndExit(String message) {
        // We don't want to exit, as we also have a enter from card image and add manually button here
        // So we show a toast instead
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }
}
