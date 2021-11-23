package protect.card_locker.importexport;

import android.content.Context;
import android.util.Log;

import net.lingala.zip4j.exception.ZipException;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;

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
    public static ImportExportResult importData(Context context, DBHelper db, InputStream input, DataFormat format, char[] password) {
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

        if (importer != null) {
            try {
                importer.importData(context, db, input, password);
                return ImportExportResult.Success;
            } catch (ZipException e) {
                return ImportExportResult.BadPassword;
            } catch (IOException | FormatException | InterruptedException | JSONException | ParseException | NullPointerException e) {
                Log.e(TAG, "Failed to import data", e);
            }

        } else {
            Log.e(TAG, "Unsupported data format imported: " + format.name());
        }

        return ImportExportResult.GenericFailure;
    }
}
