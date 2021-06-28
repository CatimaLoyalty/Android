package protect.card_locker.importexport;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;

import protect.card_locker.DBHelper;
import protect.card_locker.DataFormat;

public class MultiFormatExporter
{
    private static final String TAG = "Catima";

    /**
     * Attempts to export data to the output stream in the
     * given format, if possible.
     *
     * The output stream is closed on success.
     *
     * @return true if the database was successfully exported,
     * false otherwise. If false, partial data may have been
     * written to the output stream, and it should be discarded.
     */
    public static boolean exportData(Context context, DBHelper db, OutputStreamWriter output, DataFormat format)
    {
        Exporter exporter = null;

        switch(format)
        {
            case Catima:
                exporter = new CsvExporter();
                break;
            default:
                Log.e(TAG, "Failed to export data, unknown format " + format.name());
                break;
        }

        if(exporter != null)
        {
            try
            {
                exporter.exportData(context, db, output);
                return true;
            }
            catch(IOException e)
            {
                Log.e(TAG, "Failed to export data", e);
            }
            catch(InterruptedException e)
            {
                Log.e(TAG, "Failed to export data", e);
            }

            return false;
        }
        else
        {
            Log.e(TAG, "Unsupported data format exported: " + format.name());
            return false;
        }
    }
}
