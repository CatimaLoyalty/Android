package protect.card_locker;

import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;

public class MultiFormatImporter
{
    private static final String TAG = "LoyaltyCardLocker";

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
    public static boolean importData(DBHelper db, InputStreamReader input, DataFormat format)
    {
        DatabaseImporter importer = null;

        switch(format)
        {
            case CSV:
                importer = new CsvDatabaseImporter();
                break;
        }

        if(importer != null)
        {
            try
            {
                importer.importData(db, input);
                return true;
            }
            catch(IOException e)
            {
                Log.e(TAG, "Failed to input data", e);
            }
            catch(FormatException e)
            {
                Log.e(TAG, "Failed to input data", e);
            }
            catch(InterruptedException e)
            {
                Log.e(TAG, "Failed to input data", e);
            }

            return false;
        }
        else
        {
            Log.e(TAG, "Unsupported data format imported: " + format.name());
            return false;
        }
    }
}
