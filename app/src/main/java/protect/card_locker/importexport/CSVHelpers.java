package protect.card_locker.importexport;

import org.apache.commons.csv.CSVRecord;

import protect.card_locker.FormatException;

public class CSVHelpers {
    /**
     * Extract a string from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, defaultValue is returned.
     */
    static String extractString(String key, CSVRecord record, String defaultValue) {
        if (record.isMapped(key)) {
            return record.get(key);
        }

        return defaultValue;
    }

    /**
     * Extract an integer from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * int, a FormatException is thrown.
     */
    static Integer extractInt(String key, CSVRecord record)
            throws FormatException {
        if (!record.isMapped(key)) {
            throw new FormatException("Field not used but expected: " + key);
        }

        String value = record.get(key);
        if (value.isEmpty()) {
            throw new FormatException("Field is empty: " + key);
        }

        try {
            return Integer.parseInt(record.get(key));
        } catch (NumberFormatException e) {
            throw new FormatException("Failed to parse field: " + key, e);
        }
    }

    /**
     * Extract a long from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * int, a FormatException is thrown.
     */
    static Long extractLong(String key, CSVRecord record)
            throws FormatException {
        if (!record.isMapped(key)) {
            throw new FormatException("Field not used but expected: " + key);
        }

        String value = record.get(key);
        if (value.isEmpty()) {
            throw new FormatException("Field is empty: " + key);
        }

        try {
            return Long.parseLong(record.get(key));
        } catch (NumberFormatException e) {
            throw new FormatException("Failed to parse field: " + key, e);
        }
    }
}
