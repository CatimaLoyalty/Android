package protect.card_locker.importexport;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.InternalZipConstants;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import protect.card_locker.DBHelper;
import protect.card_locker.Group;
import protect.card_locker.ImageType;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.Utils;

/**
 * Class for exporting the database into CSV (Comma Separate Values)
 * format.
 */
public class CatimaExporter implements Exporter
{
    public void exportData(Context context, DBHelper db, OutputStream output) throws IOException, InterruptedException
    {
        // Necessary vars
        int readLen;
        byte[] readBuffer = new byte[InternalZipConstants.BUFF_SIZE];

        // Create zip output stream
        ZipOutputStream zipOutputStream = new ZipOutputStream(output);

        // Generate CSV
        ByteArrayOutputStream catimaOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter catimaOutputStreamWriter = new OutputStreamWriter(catimaOutputStream, StandardCharsets.UTF_8);
        writeCSV(db, catimaOutputStreamWriter);

        // Add CSV to zip file
        ZipParameters csvZipParameters = new ZipParameters();
        csvZipParameters.setFileNameInZip("catima.csv");
        zipOutputStream.putNextEntry(csvZipParameters);
        InputStream csvInputStream = new ByteArrayInputStream(catimaOutputStream.toByteArray());
        while ((readLen = csvInputStream.read(readBuffer)) != -1) {
            zipOutputStream.write(readBuffer, 0, readLen);
        }
        zipOutputStream.closeEntry();

        // Loop over all cards again
        Cursor cardCursor = db.getLoyaltyCardCursor();
        while(cardCursor.moveToNext())
        {
            // For each card
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cardCursor);

            // For each image
            for (ImageType imageType : ImageType.values()) {
                // If it exists, add to the .zip file
                Bitmap image = Utils.retrieveCardImage(context, card.id, imageType);
                if (image != null) {
                    ZipParameters imageZipParameters = new ZipParameters();
                    imageZipParameters.setFileNameInZip(Utils.getCardImageFileName(card.id, imageType));
                    zipOutputStream.putNextEntry(imageZipParameters);
                    InputStream imageInputStream = new ByteArrayInputStream(Utils.bitmapToByteArray(image));
                    while ((readLen = imageInputStream.read(readBuffer)) != -1) {
                        zipOutputStream.write(readBuffer, 0, readLen);
                    }
                    zipOutputStream.closeEntry();
                }
            }
        }

        zipOutputStream.close();
    }

    private void writeCSV(DBHelper db, OutputStreamWriter output) throws IOException, InterruptedException {
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
                DBHelper.LoyaltyCardDbIds.STAR_STATUS);

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
                    card.starStatus);

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
