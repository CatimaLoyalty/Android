package protect.card_locker.importexport

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONException
import protect.card_locker.FormatException
import java.io.File
import java.io.IOException
import java.text.ParseException

/**
 * Interface for a class which can import the contents of a stream
 * into the database.
 */
interface Importer {
    /**
     * Import data from the input stream in a given format into
     * the database.
     *
     * @throws IOException
     * @throws FormatException
     * @throws InterruptedException
     * @throws JSONException
     * @throws ParseException
     */
    @Throws(
        IOException::class,
        FormatException::class,
        InterruptedException::class,
        JSONException::class,
        ParseException::class
    )
    fun importData(
        context: Context,
        database: SQLiteDatabase,
        inputFile: File,
        password: CharArray
    )
}
