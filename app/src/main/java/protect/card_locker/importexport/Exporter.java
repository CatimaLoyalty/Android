package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for a class which can export the contents of the database
 * in a given format.
 */
public interface Exporter {
    /**
     * Export the database to the output stream in a given format.
     *
     * @throws IOException
     */
    void exportData(Context context, SQLiteDatabase database, OutputStream output, char[] password) throws IOException, InterruptedException;
}
