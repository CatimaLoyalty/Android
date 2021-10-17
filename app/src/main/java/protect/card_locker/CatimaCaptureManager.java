package protect.card_locker;

import android.app.Activity;
import android.widget.Toast;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CatimaCaptureManager extends CaptureManager {
    private Activity activity;

    public CatimaCaptureManager(Activity activity, DecoratedBarcodeView barcodeView) {
        super(activity, barcodeView);

        this.activity = activity;
    }

    @Override
    protected void displayFrameworkBugMessageAndExit(String message) {
        // We don't want to exit, as we also have a enter from card image and add manually button here
        // So we show a toast instead
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}
