package protect.card_locker.importexport;

import android.content.Context;

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
public interface Importer {
    /**
     * Import data from the input stream in a given format into
     * the database.
     *
     * @throws IOException
     * @throws FormatException
     */
    void importData(Context context, DBHelper db, InputStream input, char[] password) throws IOException, FormatException, InterruptedException, JSONException, ParseException;
}
