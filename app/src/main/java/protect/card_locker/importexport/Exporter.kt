package protect.card_locker.importexport

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.IOException
import java.io.OutputStream

/**
 * Interface for a class which can export the contents of the database
 * in a given format.
 */
interface Exporter {
    /**
     * Export the database to the output stream in a given format.
     *
     * @throws IOException, InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    fun exportData(
        context: Context,
        database: SQLiteDatabase,
        output: OutputStream,
        password: CharArray
    )
}
