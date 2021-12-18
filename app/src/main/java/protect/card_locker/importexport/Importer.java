package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

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
    void importData(Context context, SQLiteDatabase database, InputStream input, char[] password) throws IOException, FormatException, InterruptedException, JSONException, ParseException;
}
