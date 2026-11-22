package protect.card_locker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BarcodeImageWriterTaskTest {
    private Bitmap render(BarcodeFormat format, String value, int width, int height,
                          boolean fullscreen, boolean padding) {
        Context context = ApplicationProvider.getApplicationContext();
        ImageView target = new ImageView(context);
        target.layout(0, 0, width, height);
        BarcodeImageWriterTask task = new BarcodeImageWriterTask(
                context, target, value, CatimaBarcode.fromBarcode(format),
                StandardCharsets.ISO_8859_1, null, false, null, padding, fullscreen);
        Bitmap bitmap = task.doInBackground();
        assertNotNull(bitmap);
        return bitmap;
    }

    private void assertDecodesTo(String value, Bitmap bitmap) throws Exception {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        BinaryBitmap barcode = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(width, height, pixels)));
        assertEquals(value, new MultiFormatReader().decode(barcode).getText());
    }

    @Test
    public void tallLinearPreviewIsCompactAndReadable() throws Exception {
        for (boolean padding : new boolean[]{false, true}) {
            Bitmap barcode = render(BarcodeFormat.CODE_128, "1234567890", 600, 1200, false, padding);
            assertEquals(600, barcode.getWidth());
            assertEquals(200, barcode.getHeight());
            assertDecodesTo("1234567890", barcode);
        }
    }

    @Test
    public void eanPreviewRemainsReadable() throws Exception {
        Bitmap barcode = render(BarcodeFormat.EAN_13, "5901234123457", 600, 1200, false, true);
        assertEquals(200, barcode.getHeight());
        assertDecodesTo("5901234123457", barcode);
    }

    @Test
    public void shortPreviewFitsAvailableHeight() throws Exception {
        Bitmap barcode = render(BarcodeFormat.CODE_128, "1234567890", 600, 80, false, false);
        assertEquals(80, barcode.getHeight());
        assertDecodesTo("1234567890", barcode);
    }

    @Test
    public void largePreviewKeepsCompactProportionsWhenDownscaled() throws Exception {
        Bitmap barcode = render(BarcodeFormat.CODE_128, "1234567890", 2400, 3000, false, false);
        assertTrue(barcode.getWidth() <= 1500);
        assertTrue(barcode.getHeight() <= barcode.getWidth() / 3);
        assertDecodesTo("1234567890", barcode);
    }

    @Test
    public void fullscreenPreservesUserSelectedHeight() {
        Bitmap barcode = render(BarcodeFormat.CODE_128, "1234567890", 600, 1200, true, false);
        assertEquals(2 * barcode.getWidth(), barcode.getHeight());
    }

    @Test
    public void twoDimensionalPreviewsKeepTheirGeometry() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        int width = context.getResources().getDisplayMetrics().widthPixels;
        for (BarcodeFormat format : new BarcodeFormat[]{
                BarcodeFormat.QR_CODE, BarcodeFormat.AZTEC, BarcodeFormat.DATA_MATRIX, BarcodeFormat.PDF_417}) {
            Bitmap preview = render(format, "1234567890", width, width * 2, false, false);
            Bitmap fullscreen = render(format, "1234567890", width, width * 2, true, false);
            assertEquals(format.name(), fullscreen.getWidth(), preview.getWidth());
            assertEquals(format.name(), fullscreen.getHeight(), preview.getHeight());
            assertDecodesTo("1234567890", preview);
        }
    }
}
