package protect.card_locker;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

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
    private static final String TAG = "LoyaltyCardLocker";
    private static final int MAX_WIDTH = 500;

    private final WeakReference<ImageView> imageViewReference;
    private final String cardId;
    private final BarcodeFormat format;
    private final int imageHeight;
    private final int imageWidth;

    BarcodeImageWriterTask(ImageView imageView, String cardIdString,
                                  BarcodeFormat barcodeFormat)
    {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<>(imageView);

        cardId = cardIdString;
        format = barcodeFormat;

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
    }

    public Bitmap doInBackground(Void... params)
    {
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

        return null;
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

        imageView.setImageBitmap(result);

        if(result != null)
        {
            Log.i(TAG, "Displaying barcode");
            imageView.setVisibility(View.VISIBLE);
        }
        else
        {
            Log.i(TAG, "Barcode generation failed, removing image from display");
            imageView.setVisibility(View.GONE);
        }
    }
}