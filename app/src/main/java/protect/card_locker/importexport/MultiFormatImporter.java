package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.lingala.zip4j.exception.ZipException;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import protect.card_locker.DBHelper;

public class MultiFormatImporter {
    private static final String TAG = "Catima";

    /**
     * Attempts to import data from the input stream of the
     * given format into the database.
     * <p>
     * The input stream is not closed, and doing so is the
     * responsibility of the caller.
     *
     * @return ImportExportResult.Success if the database was successfully imported,
     * or another result otherwise. If no Success, no data was written to
     * the database.
     */
    public static ImportExportResult importData(Context context, SQLiteDatabase database, InputStream input, DataFormat format, char[] password) {
        Importer importer = null;

        switch (format) {
            case Catima:
                importer = new CatimaImporter();
                break;
            case Fidme:
                importer = new FidmeImporter();
                break;
            case Stocard:
                importer = new StocardImporter();
                break;
            case VoucherVault:
                importer = new VoucherVaultImporter();
                break;
        }

        String error = null;
        Set<String> newImageFiles = new HashSet<>();
        if (importer != null) {
            database.beginTransaction();
            try {
                int maxLoyaltyCardId = DBHelper.getMaxLoyaltyCardId(database);
                Log.d(TAG, "Current max loyalty card id: " + maxLoyaltyCardId);
                importer.importData(context, database, input, password, newImageFiles, maxLoyaltyCardId);
                database.setTransactionSuccessful();
                for (String fileName : newImageFiles) {
                    Log.d(TAG, "New image file: " + fileName);
                }
                return new ImportExportResult(ImportExportResultType.Success);
            } catch (ZipException e) {
                if (e.getType().equals(ZipException.Type.WRONG_PASSWORD)) {
                    return new ImportExportResult(ImportExportResultType.BadPassword);
                } else {
                    Log.e(TAG, "Failed to import data", e);
                    error = e.toString();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to import data", e);
                error = e.toString();
            } finally {
                database.endTransaction();
            }
        } else {
            error = "Unsupported data format imported: " + format.name();
            Log.e(TAG, error);
        }

        for (String fileName : newImageFiles) {
            context.deleteFile(fileName);
        }

        return new ImportExportResult(ImportExportResultType.GenericFailure, error);
    }
}
