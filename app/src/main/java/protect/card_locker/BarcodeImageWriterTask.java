package protect.card_locker;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.lang.ref.WeakReference;

/**
 * This task will generate a barcode and load it into an ImageView.
 * Only a weak reference of the ImageView is kept, so this class will not
 * prevent the ImageView from being garbage collected.
 */
class BarcodeImageWriterTask extends AsyncTask<Void, Void, Bitmap>
{
    private static final String TAG = "Catima";

    private static final int IS_VALID = 999;
    private boolean isSuccesful;

    // When drawn in a smaller window 1D barcodes for some reason end up
    // squished, whereas 2D barcodes look fine.
    private static final int MAX_WIDTH_1D = 1500;
    private static final int MAX_WIDTH_2D = 500;

    private final WeakReference<ImageView> imageViewReference;
    private final WeakReference<TextView> textViewReference;
    private String cardId;
    private final BarcodeFormat format;
    private final int imageHeight;
    private final int imageWidth;
    private final boolean showFallback;
    private final Runnable callback;

    BarcodeImageWriterTask(ImageView imageView, String cardIdString,
                           BarcodeFormat barcodeFormat, TextView textView,
                           boolean showFallback, Runnable callback)
    {
        isSuccesful = true;
        this.callback = callback;

        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<>(imageView);
        textViewReference = new WeakReference<>(textView);

        cardId = cardIdString;
        format = barcodeFormat;

        final int MAX_WIDTH = getMaxWidth(format);

        if(imageView.getWidth() < MAX_WIDTH)
        {
            imageHeight = imageView.getHeight();
            imageWidth = imageView.getWidth();
        }
        else
        {
            // Scale down the image to reduce the memory needed to produce it
            imageWidth = MAX_WIDTH;
            double ratio = (double)MAX_WIDTH / (double)imageView.getWidth();
            imageHeight = (int)(imageView.getHeight() * ratio);
        }

        this.showFallback = showFallback;
    }

    private int getMaxWidth(BarcodeFormat format)
    {
        switch(format)
        {
            // 2D barcodes
            case AZTEC:
            case DATA_MATRIX:
            case MAXICODE:
            case PDF_417:
            case QR_CODE:
                return MAX_WIDTH_2D;

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

    private String getFallbackString(BarcodeFormat format)
    {
        switch(format)
        {
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

    private Bitmap generate()
    {
        if (cardId.isEmpty())
        {
            return null;
        }

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix;
        try
        {
            try
            {
                bitMatrix = writer.encode(cardId, format, imageWidth, imageHeight, null);
            }
            catch(Exception e)
            {
                // Cast a wider net here and catch any exception, as there are some
                // cases where an encoder may fail if the data is invalid for the
                // barcode type. If this happens, we want to fail gracefully.
                throw new WriterException(e);
            }

            final int WHITE = 0xFFFFFFFF;
            final int BLACK = 0xFF000000;

            int bitMatrixWidth = bitMatrix.getWidth();
            int bitMatrixHeight = bitMatrix.getHeight();

            int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

            for (int y = 0; y < bitMatrixHeight; y++)
            {
                int offset = y * bitMatrixWidth;
                for (int x = 0; x < bitMatrixWidth; x++)
                {
                    int color = bitMatrix.get(x, y) ? BLACK : WHITE;
                    pixels[offset + x] = color;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, bitMatrixWidth, 0, 0, bitMatrixWidth, bitMatrixHeight);

            // Determine if the image needs to be scaled.
            // This is necessary because the datamatrix barcode generator
            // ignores the requested size and returns the smallest image necessary
            // to represent the barcode. If we let the ImageView scale the image
            // it will use bi-linear filtering, which results in a blurry barcode.
            // To avoid this, if scaling is needed do so without filtering.

            int heightScale = imageHeight / bitMatrixHeight;
            int widthScale = imageWidth / bitMatrixHeight;
            int scalingFactor = Math.min(heightScale, widthScale);

            if(scalingFactor > 1)
            {
                bitmap = Bitmap.createScaledBitmap(bitmap, bitMatrixWidth * scalingFactor, bitMatrixHeight * scalingFactor, false);
            }

            return bitmap;
        }
        catch (WriterException e)
        {
            Log.e(TAG, "Failed to generate barcode of type " + format + ": " + cardId, e);
        }
        catch(OutOfMemoryError e)
        {
            Log.w(TAG, "Insufficient memory to render barcode, "
                    + imageWidth + "x" + imageHeight + ", " + format.name()
                    + ", length=" + cardId.length(), e);
        }

        return null;
    }

    public Bitmap doInBackground(Void... params)
    {
        Bitmap bitmap = generate();

        if (bitmap == null) {
            isSuccesful = false;

            if (showFallback) {
                Log.i(TAG, "Barcode generation failed, generating fallback...");
                cardId = getFallbackString(format);
                bitmap = generate();
            }
        }

        return bitmap;
    }

    protected void onPostExecute(Bitmap result)
    {
        Log.i(TAG, "Finished generating barcode image of type " + format + ": " + cardId);
        ImageView imageView = imageViewReference.get();
        if(imageView == null)
        {
            // The ImageView no longer exists, nothing to do
            return;
        }

        imageView.setTag(isSuccesful);

        imageView.setImageBitmap(result);
        TextView textView = textViewReference.get();

        if(result != null)
        {
            Log.i(TAG, "Displaying barcode");
            imageView.setVisibility(View.VISIBLE);

            if (isSuccesful) {
                imageView.setColorFilter(null);
            } else {
                imageView.setColorFilter(Color.LTGRAY, PorterDuff.Mode.LIGHTEN);
            }

            if (textView != null) {
                textView.setVisibility(View.VISIBLE);
                textView.setText(format.name());
            }
        }
        else
        {
            Log.i(TAG, "Barcode generation failed, removing image from display");
            imageView.setVisibility(View.GONE);
            if (textView != null) {
                textView.setVisibility(View.GONE);
            }
        }

        if (callback != null) {
            callback.run();
        }
    }
}
