package protect.card_locker;

import android.database.Cursor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Class for exporting the database into CSV (Comma Separate Values)
 * format.
 */
public class CsvDatabaseExporter implements DatabaseExporter
{
    public void exportData(DBHelper db, OutputStreamWriter output) throws IOException, InterruptedException
    {
        CSVPrinter printer = new CSVPrinter(output, CSVFormat.RFC4180);

        // Print the header
        printer.printRecord(DBHelper.LoyaltyCardDbIds.ID,
                DBHelper.LoyaltyCardDbIds.STORE,
                DBHelper.LoyaltyCardDbIds.NOTE,
                DBHelper.LoyaltyCardDbIds.CARD_ID,
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE);

        Cursor cursor = db.getLoyaltyCardCursor();

        while(cursor.moveToNext())
        {
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cursor);

            printer.printRecord(card.id,
                    card.store,
                    card.note,
                    card.cardId,
                    card.barcodeType);

            if(Thread.currentThread().isInterrupted())
            {
                throw new InterruptedException();
            }
        }

        cursor.close();

        printer.close();
    }
}
