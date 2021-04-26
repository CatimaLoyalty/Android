package protect.card_locker.importexport;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;

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
    void importData(DBHelper db, InputStream input) throws IOException, FormatException, InterruptedException, JSONException, ParseException;
}
