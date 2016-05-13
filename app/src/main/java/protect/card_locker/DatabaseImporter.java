package protect.card_locker;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Interface for a class which can import the contents of a stream
 * into the database.
 */
public interface DatabaseImporter
{
    /**
     * Import data from the input stream in a given format into
     * the database.
     * @throws IOException
     * @throws FormatException
     */
    void importData(DBHelper db, InputStreamReader input) throws IOException, FormatException, InterruptedException;
}
