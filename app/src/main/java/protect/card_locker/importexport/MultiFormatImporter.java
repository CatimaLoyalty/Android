package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import protect.card_locker.Utils;

public class MultiFormatImporter {
    private static final String TAG = "Catima";
    private static final String TEMP_ZIP_NAME = MultiFormatImporter.class.getSimpleName() + ".zip";

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

        String error;
        if (importer != null) {
            File inputFile;
            try {
                inputFile = Utils.copyToTempFile(context, input, TEMP_ZIP_NAME);
                database.beginTransaction();
                try {
                    importer.importData(context, database, inputFile, password);
                    database.setTransactionSuccessful();
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
                    if (!inputFile.delete()) {
                        Log.w(TAG, "Failed to delete temporary ZIP file (should not be a problem) " + inputFile);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy ZIP file", e);
                error = e.toString();
            }
        } else {
            error = "Unsupported data format imported: " + format.name();
            Log.e(TAG, error);
        }

        return new ImportExportResult(ImportExportResultType.GenericFailure, error);
    }
}
