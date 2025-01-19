package protect.card_locker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.lang.ref.WeakReference;

import protect.card_locker.async.CompatCallable;

/**
 * This task will generate a barcode and load it into an ImageView.
 * Only a weak reference of the ImageView is kept, so this class will not
 * prevent the ImageView from being garbage collected.
 */
public class BarcodeImageWriterTask implements CompatCallable<Bitmap> {
    private static final String TAG = "Catima";

    private static final int IS_VALID = 999;
    private final Context mContext;
    private boolean isSuccesful;

    // When drawn in a smaller window 1D barcodes for some reason end up
    // squished, whereas 2D barcodes look fine.
    private static final int MAX_WIDTH_1D = 1500;
    private static final int MAX_WIDTH_2D = 500;

    private final WeakReference<ImageView> imageViewReference;
    private final WeakReference<TextView> textViewReference;
    private String cardId;
    private final CatimaBarcode format;
    private final int imageHeight;
    private final int imageWidth;
    private final int imagePadding;
    private final boolean widthPadding;
    private final boolean showFallback;
    private final BarcodeImageWriterResultCallback callback;

    BarcodeImageWriterTask(
            Context context, ImageView imageView, String cardIdString,
            CatimaBarcode barcodeFormat, TextView textView,
            boolean showFallback, BarcodeImageWriterResultCallback callback, boolean roundCornerPadding
    ) {
        mContext = context;

        isSuccesful = true;
        this.callback = callback;

        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<>(imageView);
        textViewReference = new WeakReference<>(textView);

        cardId = cardIdString;
        format = barcodeFormat;

        int imageViewHeight = imageView.getHeight();
        int imageViewWidth = imageView.getWidth();

        // Some barcodes already have internal whitespace and shouldn't get extra padding
        // TODO: Get rid of this hack by somehow detecting this extra whitespace
        if (roundCornerPadding && !barcodeFormat.hasInternalPadding()) {
            imagePadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics()));
        } else {
            imagePadding = 0;
        }

        if (format.isSquare() && imageViewWidth > imageViewHeight) {
            imageViewWidth -= imagePadding;
            widthPadding = true;
        } else {
            imageViewHeight -= imagePadding;
            widthPadding = false;
        }

        final int MAX_WIDTH = getMaxWidth(format);

        if (format.isSquare()) {
            imageHeight = imageWidth = Math.min(imageViewHeight, Math.min(MAX_WIDTH, imageViewWidth));
        } else if (imageView.getWidth() < MAX_WIDTH) {
            imageHeight = imageViewHeight;
            imageWidth = imageViewWidth;
        } else {
            // Scale down the image to reduce the memory needed to produce it
            imageWidth = MAX_WIDTH;
            double ratio = (double) MAX_WIDTH / (double) imageViewWidth;
            imageHeight = (int) (imageViewHeight * ratio);
        }

        this.showFallback = showFallback;
    }

    private int getMaxWidth(CatimaBarcode format) {
        switch (format.format()) {
            // 2D barcodes
            case AZTEC:
            case MAXICODE:
            case PDF_417:
            case QR_CODE:
                return MAX_WIDTH_2D;

            // 2D but rectangular versions get blurry otherwise
            case DATA_MATRIX:
                return MAX_WIDTH_1D;

            // 1D barcodes:
            case CODABAR:
            case CODE_39:
            case CODE_93:
            case CODE_128:
            case EAN_8:
            case EAN_13:
            case ITF:
            case UPC_A:
            case UPC_E:
            case RSS_14:
            case RSS_EXPANDED:
            case UPC_EAN_EXTENSION:
            default:
                return MAX_WIDTH_1D;
        }
    }

    private String getFallbackString(CatimaBarcode format) {
        switch (format.format()) {
            // 2D barcodes
            case AZTEC:
                return "AZTEC";
            case DATA_MATRIX:
                return "DATA_MATRIX";
            case PDF_417:
                return "PDF_417";
            case QR_CODE:
                return "QR_CODE";

            // 1D barcodes:
            case CODABAR:
                return "C0C";
            case CODE_39:
                return "CODE_39";
            case CODE_93:
                return "CODE_93";
            case CODE_128:
                return "CODE_128";
            case EAN_8:
                return "32123456";
            case EAN_13:
                return "5901234123457";
            case ITF:
                return "1003";
            case UPC_A:
                return "123456789012";
            case UPC_E:
                return "0123456";
            default:
                throw new IllegalArgumentException("No fallback known for this barcode type");
        }
    }

    private Bitmap generate() {
        if (cardId.isEmpty()) {
            return null;
        }

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix;
        try {
            try {
                bitMatrix = writer.encode(cardId, format.format(), imageWidth, imageHeight, null);
            } catch (Exception e) {
                // Cast a wider net here and catch any exception, as there are some
                // cases where an encoder may fail if the data is invalid for the
                // barcode type. If this happens, we want to fail gracefully.
                throw new WriterException(e);
            }

            final int WHITE = 0xFFFFFFFF;
            final int BLACK = 0xFF000000;

            int bitMatrixWidth = bitMatrix.getWidth();
            int bitMatrixHeight = bitMatrix.getHeight();

            int min_x = bitMatrixWidth / 2;
            int max_x = bitMatrixWidth / 2;
            int min_y = bitMatrixHeight / 2;
            int max_y = bitMatrixHeight / 2;

            int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

            for (int y = 0; y < bitMatrixHeight; y++) {
                int offset = y * bitMatrixWidth;
                for (int x = 0; x < bitMatrixWidth; x++) {

                    int color = bitMatrix.get(x, y) ? BLACK : WHITE;
                    pixels[offset + x] = color;

                    // Get the real bounds of the barcode
                    if (color == BLACK){
                        min_x = Math.min(x, min_x);
                        max_x = Math.max(x, max_x);
                        min_y = Math.min(y, min_y);
                        max_y = Math.max(y, max_y);
                    }
                }
            }

            // imagePadding may have been calculated before. Now it is used to be able to only crop extra added white space and not crop legit padding.
            int imagePadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, this.mContext.getResources().getDisplayMetrics()));

            min_x = Math.max(min_x - imagePadding, 0);
            min_y = Math.max(min_y - imagePadding, 0);
            max_x = Math.min(max_x + imagePadding, bitMatrixWidth);
            max_y = Math.min(max_y + imagePadding, bitMatrixHeight);

            int croppedWidth = max_x - min_x;
            int croppedHeight = max_y - min_y;
            int[] croppedpixels = new int[croppedWidth * croppedHeight];

            for (int y = 0; y < croppedHeight; y++) {
                int offset = y * croppedWidth;
                for (int x = 0; x < croppedWidth; x++) {
                    croppedpixels[offset + x] = pixels[(y + min_y) * bitMatrixWidth + x + min_x];
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(croppedWidth, croppedHeight,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(croppedpixels, 0, croppedWidth, 0, 0, croppedWidth, croppedHeight);

            // Scaling the image.
            // This is necessary because the datamatrix barcode generator
            // ignores the requested size and returns the smallest image necessary
            // to represent the barcode. If we let the ImageView scale the image
            // it will use bi-linear filtering, which results in a blurry barcode.
            // To avoid this, scale without filtering.

            bitmap = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, false);

            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "Failed to generate barcode of type " + format + ": " + cardId, e);
        } catch (OutOfMemoryError e) {
            Log.w(TAG, "Insufficient memory to render barcode, "
                    + imageWidth + "x" + imageHeight + ", " + format.name()
                    + ", length=" + cardId.length(), e);
        }

        return null;
    }

    public Bitmap doInBackground(Void... params) {
        // Only do the hard tasks if we've not already been cancelled
        if (!Thread.currentThread().isInterrupted()) {
            Bitmap bitmap = generate();

            if (bitmap == null) {
                isSuccesful = false;

                if (showFallback && !Thread.currentThread().isInterrupted()) {
                    Log.i(TAG, "Barcode generation failed, generating fallback...");
                    cardId = getFallbackString(format);
                    bitmap = generate();
                    return bitmap;
                }
            } else {
                return bitmap;
            }
        }

        // We've been interrupted - create a empty fallback
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        return Bitmap.createBitmap(imageWidth, imageHeight, config);
    }

    public void onPostExecute(Object castResult) {
        Bitmap result = (Bitmap) castResult;

        Log.i(TAG, "Finished generating barcode image of type " + format + ": " + cardId);
        ImageView imageView = imageViewReference.get();
        if (imageView == null) {
            // The ImageView no longer exists, nothing to do
            return;
        }

        String formatPrettyName = format.prettyName();

        imageView.setTag(isSuccesful);

        imageView.setImageBitmap(result);
        imageView.setContentDescription(mContext.getString(R.string.barcodeImageDescriptionWithType, formatPrettyName));
        TextView textView = textViewReference.get();

        if (result != null) {
            Log.i(TAG, "Displaying barcode");
            if (widthPadding) {
                imageView.setPadding(imagePadding / 2, 0, imagePadding / 2, 0);
            } else {
                imageView.setPadding(0, imagePadding / 2, 0, imagePadding / 2);
            }
            imageView.setVisibility(View.VISIBLE);

            if (isSuccesful) {
                imageView.setColorFilter(null);
            } else {
                imageView.setColorFilter(Color.LTGRAY, PorterDuff.Mode.LIGHTEN);
            }

            if (textView != null) {
                textView.setVisibility(View.VISIBLE);
                textView.setText(formatPrettyName);
            }
        } else {
            Log.i(TAG, "Barcode generation failed, removing image from display");
            imageView.setVisibility(View.GONE);
            if (textView != null) {
                textView.setVisibility(View.GONE);
            }
        }

        if (callback != null) {
            callback.onBarcodeImageWriterResult(isSuccesful);
        }
    }

    @Override
    public void onPreExecute() {
        // No Action
    }

    /**
     * Provided to comply with Callable while keeping the original Syntax of AsyncTask
     *
     * @return generated Bitmap
     */
    @Override
    public Bitmap call() {
        return doInBackground();
    }
}
