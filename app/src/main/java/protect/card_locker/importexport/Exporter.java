package protect.card_locker.importexport;

import android.content.Context;

import java.io.IOException;
import java.io.OutputStreamWriter;

import protect.card_locker.DBHelper;

/**
 * Interface for a class which can export the contents of the database
 * in a given format.
 */
public interface Exporter
{
    /**
     * Export the database to the output stream in a given format.
     * @throws IOException
     */
    void exportData(Context context, DBHelper db, OutputStreamWriter output) throws IOException, InterruptedException;
}
