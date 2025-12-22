package protect.card_locker;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateTimeUtils {
    static public Instant longToInstant(Long value) {
        if(value == null) return null;
        return Instant.ofEpochMilli(value);
    }

    static public @Nullable ZonedDateTime instantToZonedDateTime(@Nullable Instant value) {
        if(value == null) return null;
        ZoneId systemZone = ZoneId.systemDefault();
        return value.atZone(systemZone);
    }

    /**
     * Returns an Instant representing the start of the current day (00:00:00)
     * in the system's default timezone.
     */
    private static Instant getStartOfTodayAsInstant() {
        // Get the current date in the device's timezone (e.g., "2025-09-28")
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        // Get the start of that day (midnight) in the same timezone
        ZonedDateTime startOfToday = today.atStartOfDay(ZoneId.systemDefault());

        // Convert to an Instant for universal comparison
        return startOfToday.toInstant();
    }

    /**
     * Checks if an item is not yet valid based on exact date AND time.
     * Different from original behavior - now considers exact timestamps.
     *
     * New behavior: Item becomes valid only at the exact validFrom instant
     * - If validFrom = "2024-01-15 14:30:00" and current time is "2024-01-15 14:29:59" → NOT YET VALID (returns true)
     * - If validFrom = "2024-01-15 14:30:00" and current time is "2024-01-15 14:30:00" → VALID (returns false)
     */
    public static boolean isNotYetValid(Instant validFrom) {
        if (validFrom == null) {
            return false;
        }
        return validFrom.isAfter(Instant.now());
    }

    /**
     * Checks if an item has expired, considering exact date AND time.
     * @param expiry The Instant the item expires. If null, it never expires.
     * @return true if the expiry date/time is in the past.
     */
    public static boolean hasExpired(Instant expiry) {
        if (expiry == null) {
            return false; // Never expires
        }
        return expiry.isBefore(Instant.now());
    }
    static public DateTimeFormatter longDateShortTimeFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);

    static public DateTimeFormatter mediumDateShortTimeFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);

    static public String formatMedium(Instant value) {
        if(value == null) return null;
        ZonedDateTime zoneDate = instantToZonedDateTime(value);
        if(zoneDate == null) return null;
        return zoneDate.format(mediumDateShortTimeFormatter);
    }

    static public String formatLong(Instant value) {
        if(value == null) return null;
        ZonedDateTime zoneDate = instantToZonedDateTime(value);
        if(zoneDate == null) return null;
        return zoneDate.format(longDateShortTimeFormatter);
    }
}