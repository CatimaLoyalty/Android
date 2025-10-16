package protect.card_locker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.LocalFileHeader
import java.io.FileNotFoundException
import java.io.IOException

class PkpassesParser(context: Context, uri: Uri?) {
    private var mContext = context
    private val pkPassParsers: ArrayList<PkpassParser> = ArrayList()

    init {
        mContext = context

        Log.i(TAG, "Received Pkpasses file")
        if (uri == null) {
            Log.e(TAG, "Uri did not contain any data")
            throw IOException(context.getString(R.string.errorReadingFile))
        }

        try {
            mContext.contentResolver.openInputStream(uri).use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var localFileHeader: LocalFileHeader?

                    while (true) {
                        // Retrieve the next file
                        localFileHeader = zipInputStream.nextEntry

                        // If no next file, exit loop
                        if (localFileHeader == null) {
                            break
                        }

                        // Ignore directories
                        if (localFileHeader.isDirectory) continue

                        // Ignore non-pkpass files
                        if (!localFileHeader.fileName.endsWith(".pkpass")) continue

                        // Extract .pkpass (.zip) inside .pkpasses to cache directory
                        val tempFileName = "pkpassparser_" + System.currentTimeMillis() + "_" + localFileHeader.fileName
                        val tempFile = Utils.copyToTempFile(mContext, zipInputStream, tempFileName)

                        // Parse temporary file
                        pkPassParsers.add(
                            PkpassParser(mContext, tempFile.toUri())
                        )

                        // Delete temporary file
                        tempFile.delete()
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            throw IOException(mContext.getString(R.string.errorReadingFile))
        } catch (e: Exception) {
            throw e
        }
    }

    fun getPkpassParsers(): ArrayList<PkpassParser> {
        return pkPassParsers
    }

    companion object {
        private const val TAG = "Catima"
    }
}
