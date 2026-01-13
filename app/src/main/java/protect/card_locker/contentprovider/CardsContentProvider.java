package protect.card_locker.contentprovider;

import static protect.card_locker.DBHelper.LoyaltyCardDbIds;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import protect.card_locker.BuildConfig;
import protect.card_locker.DBHelper;
import protect.card_locker.preferences.Settings;

public class CardsContentProvider extends ContentProvider {
    private static final String TAG = "Catima";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".contentprovider.cards";

    public static class Version {
        public static final String MAJOR_COLUMN = "major";
        public static final String MINOR_COLUMN = "minor";
        public static final int MAJOR = 1;
        public static final int MINOR = 1;
    }

    private static final int URI_VERSION = 0;
    private static final int URI_CARDS = 1;
    private static final int URI_GROUPS = 2;
    private static final int URI_CARD_GROUPS = 3;

    private static final String[] CARDS_DEFAULT_PROJECTION = new String[]{
            LoyaltyCardDbIds.ID,
            LoyaltyCardDbIds.STORE,
            LoyaltyCardDbIds.VALID_FROM,
            LoyaltyCardDbIds.EXPIRY,
            LoyaltyCardDbIds.BALANCE,
            LoyaltyCardDbIds.BALANCE_TYPE,
            LoyaltyCardDbIds.NOTE,
            LoyaltyCardDbIds.HEADER_COLOR,
            LoyaltyCardDbIds.CARD_ID,
            LoyaltyCardDbIds.BARCODE_ID,
            LoyaltyCardDbIds.BARCODE_TYPE,
            LoyaltyCardDbIds.BARCODE_ENCODING,
            LoyaltyCardDbIds.STAR_STATUS,
            LoyaltyCardDbIds.LAST_USED,
            LoyaltyCardDbIds.ARCHIVE_STATUS,
    };

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH) {{
        addURI(AUTHORITY, "version", URI_VERSION);
        addURI(AUTHORITY, "cards", URI_CARDS);
        addURI(AUTHORITY, "groups", URI_GROUPS);
        addURI(AUTHORITY, "card_groups", URI_CARD_GROUPS);
    }};

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull final Uri uri,
                        @Nullable final String[] projection,
                        @Nullable final String selection,
                        @Nullable final String[] selectionArgs,
                        @Nullable final String sortOrder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Disable the content provider on SDK < 23 since it grants dangerous
            // permissions at install-time
            Log.w(TAG, "Content provider read is only available for SDK >= 23");
            return null;
        }

        final Settings settings = new Settings(getContext());
        if (!settings.getAllowContentProviderRead()) {
            Log.w(TAG, "Content provider read is disabled");
            return null;
        }

        final String table;
        String[] updatedProjection = projection;

        switch (uriMatcher.match(uri)) {
            case URI_VERSION:
                return queryVersion();
            case URI_CARDS:
                table = DBHelper.LoyaltyCardDbIds.TABLE;
                // Restrict columns to the default projection (omit internal columns such as zoom level)
                if (projection == null) {
                    updatedProjection = CARDS_DEFAULT_PROJECTION;
                } else {
                    final Set<String> defaultProjection = new HashSet<>(Arrays.asList(CARDS_DEFAULT_PROJECTION));
                    updatedProjection = Arrays.stream(projection).filter(defaultProjection::contains).toArray(String[]::new);
                }
                break;
            case URI_GROUPS:
                table = DBHelper.LoyaltyCardDbGroups.TABLE;
                break;
            case URI_CARD_GROUPS:
                table = DBHelper.LoyaltyCardDbIdsGroups.TABLE;
                break;
            default:
                Log.w(TAG, "Unrecognized URI " + uri);
                return null;
        }

        final DBHelper dbHelper = new DBHelper(getContext());
        final SQLiteDatabase database = dbHelper.getReadableDatabase();

        return database.query(
                table,
                updatedProjection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor queryVersion() {
        final String[] columns = new String[]{Version.MAJOR_COLUMN, Version.MINOR_COLUMN};
        final MatrixCursor matrixCursor = new MatrixCursor(columns);
        matrixCursor.addRow(new Object[]{Version.MAJOR, Version.MINOR});

        return matrixCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull final Uri uri) {
        // MIME types are not relevant (for now at least)
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull final Uri uri,
                      @Nullable final ContentValues values) {
        // This content provider is read-only for now, so we always return null
        return null;
    }

    @Override
    public int delete(@NonNull final Uri uri,
                      @Nullable final String selection,
                      @Nullable final String[] selectionArgs) {
        // This content provider is read-only for now, so we always return 0
        return 0;
    }

    @Override
    public int update(@NonNull final Uri uri,
                      @Nullable final ContentValues values,
                      @Nullable final String selection,
                      @Nullable final String[] selectionArgs) {
        // This content provider is read-only for now, so we always return 0
        return 0;
    }
}
