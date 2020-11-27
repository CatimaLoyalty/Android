package protect.card_locker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.google.zxing.integration.android.IntentIntegrator;

import androidx.core.graphics.ColorUtils;

public class Utils {
    // Barcode config dialog
    public static AlertDialog setBarcodeDialog;

    // Activity request codes
    public static final int MAIN_REQUEST = 1;
    public static final int SELECT_BARCODE_REQUEST = 2;

    static final double LUMINANCE_MIDPOINT = 0.5;

    static public LetterBitmap generateIcon(Context context, String store, Integer backgroundColor) {
        if (store.length() == 0) {
            return null;
        }

        int tileLetterFontSize = context.getResources().getDimensionPixelSize(R.dimen.tileLetterFontSize);
        int pixelSize = context.getResources().getDimensionPixelSize(R.dimen.cardThumbnailSize);

        if (backgroundColor == null) {
            backgroundColor = LetterBitmap.getDefaultColor(context, store);
        }

        return new LetterBitmap(context, store, store,
                tileLetterFontSize, pixelSize, pixelSize, backgroundColor, needsDarkForeground(backgroundColor) ? Color.BLACK : Color.WHITE);
    }

    static public boolean needsDarkForeground(Integer backgroundColor) {
        return ColorUtils.calculateLuminance(backgroundColor) > LUMINANCE_MIDPOINT;
    }

    static public void startCameraBarcodeScan(Context context, Activity activity) {
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setDesiredBarcodeFormats(BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES);

        String prompt = context.getResources().getString(R.string.scanCardBarcode);
        integrator.setPrompt(prompt);
        integrator.setBeepEnabled(false);
        integrator.initiateScan();
    }

    static public void createSetBarcodeDialog(final Context context, final Activity activity, boolean isUpdate, final String initialCardId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Get the layout inflater
        LayoutInflater inflater = activity.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_create, null));

        if (isUpdate) {
            builder.setTitle(context.getString(R.string.editCardTitle));
        } else {
            builder.setTitle(context.getString(R.string.addCardTitle));
        }

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                setBarcodeDialog.cancel();
            }
        });
        setBarcodeDialog = builder.create();
        setBarcodeDialog.show();

        View addFromCamera = setBarcodeDialog.getWindow().findViewById(R.id.add_from_camera);
        View addManually = setBarcodeDialog.getWindow().findViewById(R.id.add_manually);

        addFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.startCameraBarcodeScan(context, activity);

                setBarcodeDialog.hide();
            }
        });

        addManually.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, BarcodeSelectorActivity.class);
                if (initialCardId != null) {
                    final Bundle b = new Bundle();
                    b.putString("initialCardId", initialCardId);
                    i.putExtras(b);
                }
                activity.startActivityForResult(i, Utils.SELECT_BARCODE_REQUEST);

                setBarcodeDialog.hide();
            }
        });
    }
}
