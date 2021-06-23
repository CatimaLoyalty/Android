package protect.card_locker.importexport;

import android.content.Context;
import android.database.Cursor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStreamWriter;

import protect.card_locker.DBHelper;
import protect.card_locker.Group;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.Utils;

/**
 * Class for exporting the database into CSV (Comma Separate Values)
 * format.
 */
public class CsvExporter implements Exporter
{
    public void exportData(Context context, DBHelper db, OutputStreamWriter output) throws IOException, InterruptedException
    {
        CSVPrinter printer = new CSVPrinter(output, CSVFormat.RFC4180);

        // Print the version
        printer.printRecord("2");

        printer.println();

        // Print the header for groups
        printer.printRecord(DBHelper.LoyaltyCardDbGroups.ID);

        Cursor groupCursor = db.getGroupCursor();

        while(groupCursor.moveToNext())
        {
            Group group = Group.toGroup(groupCursor);

            printer.printRecord(group._id);

            if(Thread.currentThread().isInterrupted())
            {
                throw new InterruptedException();
            }
        }

        groupCursor.close();

        // Print an empty line
        printer.println();

        // Print the header for cards
        printer.printRecord(DBHelper.LoyaltyCardDbIds.ID,
                DBHelper.LoyaltyCardDbIds.STORE,
                DBHelper.LoyaltyCardDbIds.NOTE,
                DBHelper.LoyaltyCardDbIds.EXPIRY,
                DBHelper.LoyaltyCardDbIds.BALANCE,
                DBHelper.LoyaltyCardDbIds.BALANCE_TYPE,
                DBHelper.LoyaltyCardDbIds.CARD_ID,
                DBHelper.LoyaltyCardDbIds.BARCODE_ID,
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE,
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR,
                DBHelper.LoyaltyCardDbIds.STAR_STATUS,
                CSVHelpers.IMAGE_FRONT,
                CSVHelpers.IMAGE_BACK);

        Cursor cardCursor = db.getLoyaltyCardCursor();

        while(cardCursor.moveToNext())
        {
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cardCursor);

            printer.printRecord(card.id,
                    card.store,
                    card.note,
                    card.expiry != null ? card.expiry.getTime() : "",
                    card.balance,
                    card.balanceType,
                    card.cardId,
                    card.barcodeId,
                    card.barcodeType,
                    card.headerColor,
                    card.starStatus,
                    Utils.bitmapToBase64(Utils.retrieveCardImage(context, card.id, true)),
                    Utils.bitmapToBase64(Utils.retrieveCardImage(context, card.id, false)));

            if(Thread.currentThread().isInterrupted())
            {
                throw new InterruptedException();
            }
        }

        cardCursor.close();

        // Print an empty line
        printer.println();

        // Print the header for card group mappings
        printer.printRecord(DBHelper.LoyaltyCardDbIdsGroups.cardID,
                DBHelper.LoyaltyCardDbIdsGroups.groupID);

        Cursor cardCursor2 = db.getLoyaltyCardCursor();

        while(cardCursor2.moveToNext())
        {
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cardCursor2);

            for (Group group : db.getLoyaltyCardGroups(card.id)) {
                printer.printRecord(card.id, group._id);
            }

            if(Thread.currentThread().isInterrupted())
            {
                throw new InterruptedException();
            }
        }

        cardCursor2.close();

        printer.close();
    }
}
