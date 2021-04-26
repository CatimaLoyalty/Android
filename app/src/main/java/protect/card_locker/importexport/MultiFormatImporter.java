package protect.card_locker.importexport;

import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import protect.card_locker.DBHelper;
import protect.card_locker.DataFormat;
import protect.card_locker.FormatException;

public class MultiFormatImporter
{
    private static final String TAG = "Catima";

    /**
     * Attempts to import data from the input stream of the
     * given format into the database.
     *
     * The input stream is not closed, and doing so is the
     * responsibility of the caller.
     *
     * @return true if the database was successfully imported,
     * false otherwise. If false, no data was written to
     * the database.
     */
    public static boolean importData(DBHelper db, InputStream input, DataFormat format)
    {
        DatabaseImporter importer = null;

        switch(format)
        {
            case Catima:
                importer = new CsvDatabaseImporter();
                break;
            case Fidme:
                importer = new FidmeImporter();
                break;
            case VoucherVault:
                importer = new VoucherVaultImporter();
                break;
        }

        if (importer != null)
        {
            try
            {
                importer.importData(db, input);
                return true;
            }
            catch(IOException | FormatException | InterruptedException | JSONException | ParseException e)
            {
                Log.e(TAG, "Failed to import data", e);
            }

        }
        else
        {
            Log.e(TAG, "Unsupported data format imported: " + format.name());
        }
        return false;
    }
}
