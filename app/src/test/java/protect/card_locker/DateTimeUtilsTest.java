package protect.card_locker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class DateTimeUtilsTest {

    @Test
    public void testLongToInstant() {
        // Given
        long epochMillis = 1609459200000L; // 2021-01-01 00:00:00 UTC

        // When
        Instant result = DateTimeUtils.longToInstant(epochMillis);

        // Then
        assertNotNull(result);
        assertEquals(epochMillis, result.toEpochMilli());
    }

    @Test
    public void testLongToInstant_withNull() {
        // Given
        Long nullMillis = null;

        // When
        Instant result = DateTimeUtils.longToInstant(nullMillis);

        // Then
        assertNull(result);
    }

    @Test
    public void testInstantToZonedDateTime() {
        // Given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        // When
        ZonedDateTime result = DateTimeUtils.instantToZonedDateTime(instant);

        // Then
        assertNotNull(result);
        assertEquals(instant, result.toInstant());
        assertEquals(ZoneId.systemDefault(), result.getZone());
    }

    @Test
    public void testInstantToZonedDateTime_withNull() {
        // When
        ZonedDateTime result = DateTimeUtils.instantToZonedDateTime(null);

        // Then
        assertNull(result);
    }

    @Test
    public void testIsNotYetValid_withFutureDate() {
        // Given - A date in the future (tomorrow)
        Instant futureDate = Instant.now().plus(1, ChronoUnit.DAYS);

        // When
        boolean result = DateTimeUtils.isNotYetValid(futureDate);

        // Then
        assertTrue("Future date should be 'not yet valid'", result);
    }

    @Test
    public void testIsNotYetValid_withPastDate() {
        // Given - A date in the past (yesterday)
        Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);

        // When
        boolean result = DateTimeUtils.isNotYetValid(pastDate);

        // Then
        assertFalse("Past date should not be 'not yet valid'", result);
    }

    @Test
    public void testIsNotYetValid_withTodayStart() {
        // Given - Start of today
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ZonedDateTime startOfToday = today.atStartOfDay(ZoneId.systemDefault());
        Instant todayStart = startOfToday.toInstant();

        // When
        boolean result = DateTimeUtils.isNotYetValid(todayStart);

        // Then
        assertFalse("Start of today should not be 'not yet valid'", result);
    }

    @Test
    public void testIsNotYetValid_withNull() {
        // When
        boolean result = DateTimeUtils.isNotYetValid(null);

        // Then
        assertFalse("Null date should not be 'not yet valid'", result);
    }

    @Test
    public void testHasExpired_withPastDate() {
        // Given - A date in the past (yesterday)
        Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);

        // When
        boolean result = DateTimeUtils.hasExpired(pastDate);

        // Then
        assertTrue("Past date should be expired", result);
    }

    @Test
    public void testHasExpired_withFutureDate() {
        // Given - A date in the future (tomorrow)
        Instant futureDate = Instant.now().plus(1, ChronoUnit.DAYS);

        // When
        boolean result = DateTimeUtils.hasExpired(futureDate);

        // Then
        assertFalse("Future date should not be expired", result);
    }

    @Test
    public void testHasExpired_withTodayStart() {
        // Given - Start of today
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ZonedDateTime startOfToday = today.atStartOfDay(ZoneId.systemDefault());
        Instant todayStart = startOfToday.toInstant();

        // When
        boolean result = DateTimeUtils.hasExpired(todayStart);

        // Then
        assertTrue("Start of today should be considered expired", result);
    }

    @Test
    public void testHasExpired_withNull() {
        // When
        boolean result = DateTimeUtils.hasExpired(null);

        // Then
        assertFalse("Null expiry should never expire", result);
    }

    @Test
    public void testFormatMedium_withValidInstant() {
        // Given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        // When
        String result = DateTimeUtils.formatMedium(instant);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Verify it follows the expected pattern by parsing it back
        ZonedDateTime zdt = Instant.parse("2024-01-15T10:30:00Z")
                .atZone(ZoneId.systemDefault());
        String expected = zdt.format(DateTimeFormatter.ofLocalizedDateTime(
                java.time.format.FormatStyle.MEDIUM,
                java.time.format.FormatStyle.SHORT
        ));
        assertEquals(expected, result);
    }

    @Test
    public void testFormatMedium_withNull() {
        // When
        String result = DateTimeUtils.formatMedium(null);

        // Then
        assertNull(result);
    }

    @Test
    public void testFormatLong_withValidInstant() {
        // Given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        // When
        String result = DateTimeUtils.formatLong(instant);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Verify it follows the expected pattern by parsing it back
        ZonedDateTime zdt = Instant.parse("2024-01-15T10:30:00Z")
                .atZone(ZoneId.systemDefault());
        String expected = zdt.format(DateTimeFormatter.ofLocalizedDateTime(
                java.time.format.FormatStyle.LONG,
                java.time.format.FormatStyle.SHORT
        ));
        assertEquals(expected, result);
    }

    @Test
    public void testFormatLong_withNull() {
        // When
        String result = DateTimeUtils.formatLong(null);

        // Then
        assertNull(result);
    }

    @Test
    public void testDateTimeFormattersInitialization() {
        // Verify that the formatters are properly initialized
        assertNotNull(DateTimeUtils.longDateShortTimeFormatter);
        assertNotNull(DateTimeUtils.mediumDateShortTimeFormatter);

        // Test that formatters work correctly
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());

        String longFormat = zdt.format(DateTimeUtils.longDateShortTimeFormatter);
        String mediumFormat = zdt.format(DateTimeUtils.mediumDateShortTimeFormatter);

        assertNotNull(longFormat);
        assertNotNull(mediumFormat);
        assertFalse(longFormat.isEmpty());
        assertFalse(mediumFormat.isEmpty());
    }

    @Test
    public void testIsNotYetValid_RelativeToNow() {
        Instant now = Instant.now();

        // Test with timestamps relative to current moment
        Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
        Instant oneMinuteLater = now.plus(1, ChronoUnit.MINUTES);
        Instant oneHourLater = now.plus(1, ChronoUnit.HOURS);

        // One minute ago should be valid (not "not yet valid")
        assertFalse(DateTimeUtils.isNotYetValid(oneMinuteAgo));

        // Future times should be "not yet valid"
        assertTrue(DateTimeUtils.isNotYetValid(oneMinuteLater));
        assertTrue(DateTimeUtils.isNotYetValid(oneHourLater));
    }

    @Test
    public void testConsistencyBetweenMethods() {
        // Test that the methods work consistently together
        long epochMillis = 1609459200000L; // 2021-01-01 00:00:00 UTC

        Instant instant = DateTimeUtils.longToInstant(epochMillis);
        ZonedDateTime zonedDateTime = DateTimeUtils.instantToZonedDateTime(instant);

        assertNotNull(instant);
        assertNotNull(zonedDateTime);
        assertEquals(epochMillis, instant.toEpochMilli());
        assertEquals(instant, zonedDateTime.toInstant());
    }
}