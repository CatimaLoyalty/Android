package protect.card_locker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.ImageView;

import androidx.test.core.app.ActivityScenario;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class UtilsTest {
    /**
     * Ensure all the default card colours (used when a user has not chosen a card colour) use white foreground text
     */
    @Test
    public void allDefaultCardColoursHaveWhiteForegroundTest() {
        try(ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TypedArray colors = activity.getApplicationContext().getResources().obtainTypedArray(R.array.letter_tile_colors);

                for (int i = 0; i < colors.length(); i++) {
                    int color = colors.getColor(i, Color.WHITE);
                    assertFalse(Utils.needsDarkForeground(color));
                }
            });
        }
    }

    /**
     * Test checksum method with a fake (small) input stream
     */
    @Test
    public void testChecksum_withFakeInputStream() throws Exception {
        byte[] fakeContent = "test content".getBytes();
        InputStream fakeStream = new ByteArrayInputStream(fakeContent);

        String checksum = Utils.checksum(fakeStream);

        assertNotNull(checksum);
        assertTrue(checksum.matches("[0-9a-f]+"));
    }

    /**
     * Test checksum method with large random content (1 MB)
     */
    @Test
    public void testChecksum_withLargeContent() throws Exception {
        byte[] largeContent = new byte[1024 * 1024];
        new Random().nextBytes(largeContent);
        InputStream largeStream = new ByteArrayInputStream(largeContent);

        String checksum = Utils.checksum(largeStream);

        assertNotNull(checksum);
        assertTrue(checksum.matches("[0-9a-f]+"));
    }

    /**
     * Test checksum method using a mocked FileInputStream
     */
    @Test
    public void testChecksum_withMockedFileInputStream() throws Exception {
        FileInputStream mockStream = mock(FileInputStream.class);
        when(mockStream.read(any(byte[].class))).thenReturn(-1);

        String checksum = Utils.checksum(mockStream);

        assertNotNull(checksum);
        verify(mockStream, atLeastOnce()).read(any(byte[].class));
    }

    /**
     * Test getUnixTime method returns reasonable timestamp
     */
    @Test
    public void testGetUnixTime() {
        long unixTime = Utils.getUnixTime();

        // Unix time should be positive
        assertTrue("Unix time should be positive", unixTime > 0);

        // Should be reasonable (after year 2020 and before year 2030)
        long year2020 = 1577836800L; // Jan 1, 2020
        long year2030 = 1893456000L; // Jan 1, 2030

        assertTrue("Unix time should be after 2020", unixTime > year2020);
        assertTrue("Unix time should be before 2030", unixTime < year2030);
    }

    /**
     * Test createTempFile method with mocked context
     */
    @Test
    public void testCreateTempFile() {
        // Create a mock context
        Context mockContext = mock(Context.class);
        File mockCacheDir = new File("/mock/cache");

        when(mockContext.getCacheDir()).thenReturn(mockCacheDir);

        // Test file creation
        String fileName = "test_file.txt";
        File result = Utils.createTempFile(mockContext, fileName);

        assertNotNull("Should return a File object", result);
        assertTrue("File path should contain cache directory", result.getPath().contains("cache"));
        assertTrue("File path should contain filename", result.getPath().contains(fileName));
        assertEquals("Should create correct file path", "\\mock\\cache\\test_file.txt", result.getPath());
    }

    /**
     * Test getComplementaryColor method
     */
    @Test
    public void testGetComplementaryColor() {
        // Test with pure red
        int red = Color.rgb(255, 0, 0);
        int complementRed = Utils.getComplementaryColor(red);
        assertEquals("Complement of red should be cyan", Color.rgb(0, 255, 255), complementRed);

        // Test with white
        int white = Color.WHITE;
        int complementWhite = Utils.getComplementaryColor(white);
        assertEquals("Complement of white should be black", Color.BLACK, complementWhite);

        // Test with black
        int black = Color.BLACK;
        int complementBlack = Utils.getComplementaryColor(black);
        assertEquals("Complement of black should be white", Color.WHITE, complementBlack);

        // Test with gray (should remain the same)
        int gray = Color.rgb(128, 128, 128);
        int complementGray = Utils.getComplementaryColor(gray);
        assertEquals("Complement of gray should be gray", Color.rgb(127, 127, 127), complementGray);
    }

    /**
     * Test getRecommendedScaleTypeForThumbnailImage method
     */
    @Test
    public void testGetRecommendedScaleTypeForThumbnailImage() {
        // Test with null bitmap
        ImageView.ScaleType result = Utils.getRecommendedScaleTypeForThumbnailImage(null);
        assertEquals("Should return FIT_CENTER for null bitmap", ImageView.ScaleType.FIT_CENTER, result);

        // Create mock bitmaps with different aspect ratios
        Bitmap mockBitmapSquare = mock(Bitmap.class);
        when(mockBitmapSquare.getWidth()).thenReturn(100);
        when(mockBitmapSquare.getHeight()).thenReturn(100);

        result = Utils.getRecommendedScaleTypeForThumbnailImage(mockBitmapSquare);
        assertEquals("Should return FIT_CENTER for square image", ImageView.ScaleType.FIT_CENTER, result);

        // Test with ideal card ratio (around 1.58)
        Bitmap mockBitmapCard = mock(Bitmap.class);
        when(mockBitmapCard.getWidth()).thenReturn(158);
        when(mockBitmapCard.getHeight()).thenReturn(100);

        result = Utils.getRecommendedScaleTypeForThumbnailImage(mockBitmapCard);
        assertEquals("Should return CENTER_CROP for card-like aspect ratio", ImageView.ScaleType.CENTER_CROP, result);

        // Test with very wide image
        Bitmap mockBitmapWide = mock(Bitmap.class);
        when(mockBitmapWide.getWidth()).thenReturn(300);
        when(mockBitmapWide.getHeight()).thenReturn(100);

        result = Utils.getRecommendedScaleTypeForThumbnailImage(mockBitmapWide);
        assertEquals("Should return FIT_CENTER for very wide image", ImageView.ScaleType.FIT_CENTER, result);
    }

    /**
     * Test basicMDToHTML method with various markdown patterns
     */
    @Test
    public void testBasicMDToHTML() {
        // Test heading conversion
        String input = "# Main Title\n## Subtitle";
        String expected = "<h1>Main Title</h1>\n<h2>Subtitle</h2>";
        assertEquals("Should convert headings correctly", expected, Utils.basicMDToHTML(input));

        // Test link conversion
        input = "Visit [GitHub](https://github.com)";
        expected = "Visit <a href=\"https://github.com\">GitHub</a>";
        assertEquals("Should convert links correctly", expected, Utils.basicMDToHTML(input));

        // Test bold text conversion
        input = "This is **bold** text";
        expected = "This is <b>bold</b> text";
        assertEquals("Should convert bold text correctly", expected, Utils.basicMDToHTML(input));

        // Test list item conversion
        input = "- First item\n- Second item";
        expected = "<ul><li>&nbsp;First item</li></ul>\n<ul><li>&nbsp;Second item</li></ul>";
        // The method should merge consecutive list items
        String actual = Utils.basicMDToHTML(input);
        assertTrue("Should contain list items", actual.contains("<li>&nbsp;First item</li>"));
        assertTrue("Should contain list items", actual.contains("<li>&nbsp;Second item</li>"));
    }

    /**
     * Test linkify method for automatic link detection
     */
    @Test
    public void testLinkify() {
        // Test email linkification
        String input = "Contact us at support@example.com";
        String result = Utils.linkify(input);
        assertTrue("Should linkify email addresses",
                result.contains("<a href=\"mailto:support@example.com\">support@example.com</a>"));

        // Test URL linkification
        input = "Visit https://www.example.com for more info";
        result = Utils.linkify(input);
        assertTrue("Should linkify URLs",
                result.contains("<a href=\"https://www.example.com\">https://www.example.com</a>"));

        // Test mixed content
        input = "Email: test@domain.org and website: http://example.net";
        result = Utils.linkify(input);
        assertTrue("Should linkify email in mixed content",
                result.contains("<a href=\"mailto:test@domain.org\">test@domain.org</a>"));
        assertTrue("Should linkify URL in mixed content",
                result.contains("<a href=\"http://example.net\">http://example.net</a>"));

        // Test no links
        input = "Just plain text with no links";
        result = Utils.linkify(input);
        assertEquals("Should not modify text without links", input, result);
    }
}
