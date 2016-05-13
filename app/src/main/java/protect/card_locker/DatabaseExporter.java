package protect.card_locker;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Interface for a class which can export the contents of the database
 * in a given format.
 */
public interface DatabaseExporter
{
    /**
     * Export the database to the output stream in a given format.
     * @throws IOException
     */
    void exportData(DBHelper db, OutputStreamWriter output) throws IOException, InterruptedException;
}
