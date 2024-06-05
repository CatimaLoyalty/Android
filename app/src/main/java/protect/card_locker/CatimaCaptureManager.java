package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.core.util.Consumer;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CatimaCaptureManager extends CaptureManager {
    private final Consumer<String> mErrorCallback;

    public CatimaCaptureManager(Activity activity, DecoratedBarcodeView barcodeView, Consumer<String> errorCallback) {
        super(activity, barcodeView);

        mErrorCallback = errorCallback;
    }

    @Override
    protected void displayFrameworkBugMessageAndExit(String message) {
        // We don't want to exit, as we also have a enter from card image and add manually button here
        // So, instead, we call our error callback
        mErrorCallback.accept(message);
    }
}
