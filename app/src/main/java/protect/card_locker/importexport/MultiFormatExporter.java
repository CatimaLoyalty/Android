package protect.card_locker.importexport;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

import protect.card_locker.DBHelper;

public class MultiFormatExporter {
    private static final String TAG = "Catima";

    /**
     * Attempts to export data to the output stream in the
     * given format, if possible.
     * <p>
     * The output stream is closed on success.
     *
     * @return ImportExportResult.Success if the database was successfully exported,
     * another ImportExportResult otherwise. If not Success, partial data may have been
     * written to the output stream, and it should be discarded.
     */
    public static ImportExportResult exportData(Context context, DBHelper db, OutputStream output, DataFormat format, char[] password) {
        Exporter exporter = null;

        switch (format) {
            case Catima:
                exporter = new CatimaExporter();
                break;
            default:
                Log.e(TAG, "Failed to export data, unknown format " + format.name());
                break;
        }

        if (exporter != null) {
            try {
                exporter.exportData(context, db, output, password);
                return ImportExportResult.Success;
            } catch (IOException e) {
                Log.e(TAG, "Failed to export data", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to export data", e);
            }

            return ImportExportResult.GenericFailure;
        } else {
            Log.e(TAG, "Unsupported data format exported: " + format.name());
            return ImportExportResult.GenericFailure;
        }
    }
}
