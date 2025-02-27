package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class LoyaltyCard {
    public int id;
    public String store;
    public String note;
    @Nullable
    public Date validFrom;
    @Nullable
    public Date expiry;
    public BigDecimal balance;
    @Nullable
    public Currency balanceType;
    public String cardId;
    @Nullable
    public String barcodeId;
    @Nullable
    public CatimaBarcode barcodeType;
    @Nullable
    public Integer headerColor;
    public int starStatus;
    public long lastUsed;
    public int zoomLevel;
    public int zoomLevelWidth;
    public int archiveStatus;


    @Nullable
    private Bitmap imageThumbnail;
    @Nullable
    private String imageThumbnailPath;
    @Nullable
    private Bitmap imageFront;
    @Nullable
    private String imageFrontPath;
    @Nullable
    private Bitmap imageBack;
    @Nullable
    private String imageBackPath;

    public static final String BUNDLE_LOYALTY_CARD_ID = "loyaltyCardId";
    public static final String BUNDLE_LOYALTY_CARD_STORE = "loyaltyCardStore";
    public static final String BUNDLE_LOYALTY_CARD_NOTE = "loyaltyCardNote";
    public static final String BUNDLE_LOYALTY_CARD_VALID_FROM = "loyaltyCardValidFrom";
    public static final String BUNDLE_LOYALTY_CARD_EXPIRY = "loyaltyCardExpiry";
    public static final String BUNDLE_LOYALTY_CARD_BALANCE = "loyaltyCardBalance";
    public static final String BUNDLE_LOYALTY_CARD_BALANCE_TYPE = "loyaltyCardBalanceType";
    public static final String BUNDLE_LOYALTY_CARD_CARD_ID = "loyaltyCardCardId";
    public static final String BUNDLE_LOYALTY_CARD_BARCODE_ID = "loyaltyCardBarcodeId";
    public static final String BUNDLE_LOYALTY_CARD_BARCODE_TYPE = "loyaltyCardBarcodeType";
    public static final String BUNDLE_LOYALTY_CARD_HEADER_COLOR = "loyaltyCardHeaderColor";
    public static final String BUNDLE_LOYALTY_CARD_STAR_STATUS = "loyaltyCardStarStatus";
    public static final String BUNDLE_LOYALTY_CARD_LAST_USED = "loyaltyCardLastUsed";
    public static final String BUNDLE_LOYALTY_CARD_ZOOM_LEVEL = "loyaltyCardZoomLevel";
    public static final String BUNDLE_LOYALTY_CARD_ZOOM_LEVEL_WIDTH = "loyaltyCardZoomLevelWidth";
    public static final String BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS = "loyaltyCardArchiveStatus";
    public static final String BUNDLE_LOYALTY_CARD_IMAGE_THUMBNAIL = "loyaltyCardImageThumbnail";
    public static final String BUNDLE_LOYALTY_CARD_IMAGE_FRONT = "loyaltyCardImageFront";
    public static final String BUNDLE_LOYALTY_CARD_IMAGE_BACK = "loyaltyCardImageBack";

    private static final String TEMP_IMAGE_THUMBNAIL_FILE_NAME = "loyaltyCardTempImageThumbnailFileName";
    private static final String TEMP_IMAGE_FRONT_FILE_NAME = "loyaltyCardTempImageFrontFileName";
    private static final String TEMP_IMAGE_BACK_FILE_NAME = "loyaltyCardTempImageBackFileName";

    /**
     * Create a loyalty card object with default values
     */
    public LoyaltyCard() {
        setId(-1);
        setStore("");
        setNote("");
        setValidFrom(null);
        setExpiry(null);
        setBalance(new BigDecimal("0"));
        setBalanceType(null);
        setCardId("");
        setBarcodeId(null);
        setBarcodeType(null);
        setHeaderColor(null);
        setStarStatus(0);
        setLastUsed(Utils.getUnixTime());
        setZoomLevel(100);
        setZoomLevelWidth(100);
        setArchiveStatus(0);
        setImageThumbnail(null, null);
        setImageFront(null, null);
        setImageBack(null, null);
    }

    /**
     * Create a new loyalty card
     *
     * @param id
     * @param store
     * @param note
     * @param validFrom
     * @param expiry
     * @param balance
     * @param balanceType
     * @param cardId
     * @param barcodeId
     * @param barcodeType
     * @param headerColor
     * @param starStatus
     * @param lastUsed
     * @param zoomLevel
     * @param zoomLevelWidth
     * @param archiveStatus
     */
    public LoyaltyCard(final int id, final String store, final String note, @Nullable final Date validFrom,
                       @Nullable final Date expiry, final BigDecimal balance, @Nullable final Currency balanceType,
                       final String cardId, @Nullable final String barcodeId, @Nullable final CatimaBarcode barcodeType,
                       @Nullable final Integer headerColor, final int starStatus,
                       final long lastUsed, final int zoomLevel, final int zoomLevelWidth, final int archiveStatus,
                       @Nullable Bitmap imageThumbnail, @Nullable String imageThumbnailPath,
                       @Nullable Bitmap imageFront, @Nullable String imageFrontPath,
                       @Nullable Bitmap imageBack, @Nullable String imageBackPath) {
        setId(id);
        setStore(store);
        setNote(note);
        setValidFrom(validFrom);
        setExpiry(expiry);
        setBalance(balance);
        setBalanceType(balanceType);
        setCardId(cardId);
        setBarcodeId(barcodeId);
        setBarcodeType(barcodeType);
        setHeaderColor(headerColor);
        setStarStatus(starStatus);
        setLastUsed(lastUsed);
        setZoomLevel(zoomLevel);
        setZoomLevelWidth(zoomLevelWidth);
        setArchiveStatus(archiveStatus);
        setImageThumbnail(imageThumbnail, imageThumbnailPath);
        setImageFront(imageFront, imageFrontPath);
        setImageBack(imageBack, imageBackPath);
    }

    @Nullable
    public Bitmap getImageThumbnail(Context context) {
        if (imageThumbnailPath != null) {
            if (imageThumbnailPath.equals(TEMP_IMAGE_THUMBNAIL_FILE_NAME)) {
                imageThumbnail = Utils.loadTempImage(context, imageThumbnailPath);
            } else {
                imageThumbnail = Utils.retrieveCardImage(context, imageThumbnailPath);
            }
            imageThumbnailPath = null;
        }

        if (imageThumbnail == null) {
            return null;
        }

        return imageThumbnail.copy(imageThumbnail.getConfig(), imageThumbnail.isMutable());
    }

    @Nullable
    public Bitmap getImageFront(Context context) {
        if (imageFrontPath != null) {
            if (imageFrontPath.equals(TEMP_IMAGE_FRONT_FILE_NAME)) {
                imageFront = Utils.loadTempImage(context, imageFrontPath);
            } else {
                imageFront = Utils.retrieveCardImage(context, imageFrontPath);
            }
            imageFrontPath = null;
        }

        if (imageFront == null) {
            return null;
        }

        return imageFront.copy(imageFront.getConfig(), imageFront.isMutable());
    }

    @Nullable
    public Bitmap getImageBack(Context context) {
        if (imageBackPath != null) {
            if (imageBackPath.equals(TEMP_IMAGE_BACK_FILE_NAME)) {
                imageBack = Utils.loadTempImage(context, imageBackPath);
            } else {
                imageBack = Utils.retrieveCardImage(context, imageBackPath);
            }
            imageBackPath = null;
        }

        if (imageBack == null) {
            return null;
        }

        return imageBack.copy(imageBack.getConfig(), imageBack.isMutable());
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setStore(@NonNull String store) {
        this.store = store;
    }

    public void setNote(@NonNull String note) {
        this.note = note;
    }

    public void setValidFrom(@Nullable Date validFrom) {
        this.validFrom = validFrom;
    }

    public void setExpiry(@Nullable Date expiry) {
        this.expiry = expiry;
    }

    public void setBalance(@NonNull BigDecimal balance) {
        this.balance = balance;
    }

    public void setBalanceType(@Nullable Currency balanceType) {
        this.balanceType = balanceType;
    }

    public void setCardId(@NonNull String cardId) {
        this.cardId = cardId;
    }

    public void setBarcodeId(@Nullable String barcodeId) {
        this.barcodeId = barcodeId;
    }

    public void setBarcodeType(@Nullable CatimaBarcode barcodeType) {
        this.barcodeType = barcodeType;
    }

    public void setHeaderColor(@Nullable Integer headerColor) {
        this.headerColor = headerColor;
    }

    public void setStarStatus(int starStatus) {
        if (starStatus != 0 && starStatus != 1) {
            throw new IllegalArgumentException("starStatus must be 0 or 1");
        }

        this.starStatus = starStatus;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    public void setZoomLevel(int zoomLevel) {
        if (zoomLevel < 0 || zoomLevel > 100) {
            throw new IllegalArgumentException("zoomLevel must be in range 0-100");
        }

        this.zoomLevel = zoomLevel;
    }

    public void setZoomLevelWidth(int zoomLevelWidth) {
        if (zoomLevelWidth < 0 || zoomLevelWidth > 100) {
            throw new IllegalArgumentException("zoomLevelWidth must be in range 0-100");
        }

        this.zoomLevelWidth = zoomLevelWidth;
    }

    public void setArchiveStatus(int archiveStatus) {
        if (archiveStatus != 0 && archiveStatus != 1) {
            throw new IllegalArgumentException("archiveStatus must be 0 or 1");
        }

        this.archiveStatus = archiveStatus;
    }

    public void setImageThumbnail(@Nullable Bitmap imageThumbnail, @Nullable String imageThumbnailPath) {
        if (imageThumbnail != null && imageThumbnailPath != null) {
            throw new IllegalArgumentException("Cannot set both thumbnail and path");
        }

        this.imageThumbnailPath = imageThumbnailPath;
        this.imageThumbnail = imageThumbnail != null ? imageThumbnail.copy(imageThumbnail.getConfig(), imageThumbnail.isMutable()) : null;
    }

    public void setImageFront(@Nullable Bitmap imageFront, @Nullable String imageFrontPath) {
        if (imageFront != null && imageFrontPath != null) {
            throw new IllegalArgumentException("Cannot set both thumbnail and path");
        }

        this.imageFrontPath = imageFrontPath;
        this.imageFront = imageFront != null ? imageFront.copy(imageFront.getConfig(), imageFront.isMutable()) : null;
    }

    public void setImageBack(@Nullable Bitmap imageBack, @Nullable String imageBackPath) {
        if (imageBack != null && imageBackPath != null) {
            throw new IllegalArgumentException("Cannot set both thumbnail and path");
        }

        this.imageBackPath = imageBackPath;
        this.imageBack = imageBack != null ? imageBack.copy(imageBack.getConfig(), imageBack.isMutable()) : null;
    }

    @Nullable
    public Bitmap getImageForImageLocationType(Context context, ImageLocationType imageLocationType) {
        if (imageLocationType == ImageLocationType.icon) {
            return getImageThumbnail(context);
        } else if (imageLocationType == ImageLocationType.front) {
            return getImageFront(context);
        } else if (imageLocationType == ImageLocationType.back) {
            return getImageBack(context);
        }

        throw new IllegalArgumentException("Unknown image location type");
    }

    public void updateFromBundle(@NonNull Bundle bundle, boolean requireFull) {
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_ID)) {
            setId(bundle.getInt(BUNDLE_LOYALTY_CARD_ID));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_ID);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_STORE)) {
            setStore(Objects.requireNonNull(bundle.getString(BUNDLE_LOYALTY_CARD_STORE)));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_STORE);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_NOTE)) {
            setNote(Objects.requireNonNull(bundle.getString(BUNDLE_LOYALTY_CARD_NOTE)));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_NOTE);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_VALID_FROM)) {
            long tmpValidFrom = bundle.getLong(BUNDLE_LOYALTY_CARD_VALID_FROM);
            setValidFrom(tmpValidFrom > 0 ? new Date(tmpValidFrom) : null);
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_VALID_FROM);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_EXPIRY)) {
            long tmpExpiry = bundle.getLong(BUNDLE_LOYALTY_CARD_EXPIRY);
            setExpiry(tmpExpiry > 0 ? new Date(tmpExpiry) : null);
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_EXPIRY);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BALANCE)) {
            setBalance(new BigDecimal(bundle.getString(BUNDLE_LOYALTY_CARD_BALANCE)));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_BALANCE);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BALANCE_TYPE)) {
            String tmpBalanceType = bundle.getString(BUNDLE_LOYALTY_CARD_BALANCE_TYPE);
            setBalanceType(tmpBalanceType != null ? Currency.getInstance(tmpBalanceType) : null);
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_BALANCE_TYPE);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_CARD_ID)) {
            setCardId(Objects.requireNonNull(bundle.getString(BUNDLE_LOYALTY_CARD_CARD_ID)));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_CARD_ID);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BARCODE_ID)) {
            setBarcodeId(bundle.getString(BUNDLE_LOYALTY_CARD_BARCODE_ID));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_BARCODE_ID);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BARCODE_TYPE)) {
            String tmpBarcodeType = bundle.getString(BUNDLE_LOYALTY_CARD_BARCODE_TYPE);
            setBarcodeType(tmpBarcodeType != null ? CatimaBarcode.fromName(tmpBarcodeType) : null);
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_BARCODE_TYPE);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_HEADER_COLOR)) {
            int tmpHeaderColor = bundle.getInt(BUNDLE_LOYALTY_CARD_HEADER_COLOR);
            setHeaderColor(tmpHeaderColor != -1 ? tmpHeaderColor : null);
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_HEADER_COLOR);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_STAR_STATUS)) {
            setStarStatus(bundle.getInt(BUNDLE_LOYALTY_CARD_STAR_STATUS));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_STAR_STATUS);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_LAST_USED)) {
            setLastUsed(bundle.getLong(BUNDLE_LOYALTY_CARD_LAST_USED));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_LAST_USED);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL)) {
            setZoomLevel(bundle.getInt(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_ZOOM_LEVEL);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL_WIDTH)) {
            setZoomLevelWidth(bundle.getInt(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL_WIDTH));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_ZOOM_LEVEL_WIDTH);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS)) {
            setArchiveStatus(bundle.getInt(BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_IMAGE_THUMBNAIL)) {
            setImageThumbnail(null, bundle.getString(BUNDLE_LOYALTY_CARD_IMAGE_THUMBNAIL));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_IMAGE_THUMBNAIL);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_IMAGE_FRONT)) {
            setImageFront(null, bundle.getString(BUNDLE_LOYALTY_CARD_IMAGE_FRONT));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_IMAGE_FRONT);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_IMAGE_BACK)) {
            setImageBack(null, bundle.getString(BUNDLE_LOYALTY_CARD_IMAGE_BACK));
        } else if (requireFull) {
            throw new IllegalArgumentException("Missing key " + BUNDLE_LOYALTY_CARD_IMAGE_BACK);
        }
    }

    public Bundle toBundle(Context context, List<String> exportLimit) {
        boolean exportIsLimited = !exportLimit.isEmpty();

        Bundle bundle = new Bundle();

        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_ID)) {
            bundle.putInt(BUNDLE_LOYALTY_CARD_ID, id);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_STORE)) {
            bundle.putString(BUNDLE_LOYALTY_CARD_STORE, store);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_NOTE)) {
            bundle.putString(BUNDLE_LOYALTY_CARD_NOTE, note);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_VALID_FROM)) {
            bundle.putLong(BUNDLE_LOYALTY_CARD_VALID_FROM, validFrom != null ? validFrom.getTime() : -1);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_EXPIRY)) {
            bundle.putLong(BUNDLE_LOYALTY_CARD_EXPIRY, expiry != null ? expiry.getTime() : -1);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_BALANCE)) {
            bundle.putString(BUNDLE_LOYALTY_CARD_BALANCE, balance.toString());
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_BALANCE_TYPE)) {
            bundle.putString(BUNDLE_LOYALTY_CARD_BALANCE_TYPE, balanceType != null ? balanceType.toString() : null);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_CARD_ID)) {
            bundle.putString(BUNDLE_LOYALTY_CARD_CARD_ID, cardId);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_BARCODE_ID)) {
            bundle.putString(BUNDLE_LOYALTY_CARD_BARCODE_ID, barcodeId);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_BARCODE_TYPE)) {
            bundle.putString(BUNDLE_LOYALTY_CARD_BARCODE_TYPE, barcodeType != null ? barcodeType.name() : null);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_HEADER_COLOR)) {
            bundle.putInt(BUNDLE_LOYALTY_CARD_HEADER_COLOR, headerColor != null ? headerColor : -1);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_STAR_STATUS)) {
            bundle.putInt(BUNDLE_LOYALTY_CARD_STAR_STATUS, starStatus);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_LAST_USED)) {
            bundle.putLong(BUNDLE_LOYALTY_CARD_LAST_USED, lastUsed);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL)) {
            bundle.putInt(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL, zoomLevel);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL_WIDTH)) {
            bundle.putInt(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL_WIDTH, zoomLevelWidth);
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS)) {
            bundle.putInt(BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS, archiveStatus);
        }
        // There is an (undocumented) size limit to bundles of around 2MB(?), when going over it you will experience a random crash
        // So, instead of storing the bitmaps directly, we write the bitmap to a temp file and store the path
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_IMAGE_THUMBNAIL)) {
            Bitmap thumbnail = getImageThumbnail(context);
            if (thumbnail != null) {
                Utils.saveTempImage(context, thumbnail, TEMP_IMAGE_THUMBNAIL_FILE_NAME, Bitmap.CompressFormat.PNG);
                bundle.putString(BUNDLE_LOYALTY_CARD_IMAGE_THUMBNAIL, TEMP_IMAGE_THUMBNAIL_FILE_NAME);
            } else {
                bundle.putString(BUNDLE_LOYALTY_CARD_IMAGE_THUMBNAIL, null);
            }
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_IMAGE_FRONT)) {
            Bitmap front = getImageFront(context);
            if (front != null) {
                Utils.saveTempImage(context, front, TEMP_IMAGE_FRONT_FILE_NAME, Bitmap.CompressFormat.PNG);
                bundle.putString(BUNDLE_LOYALTY_CARD_IMAGE_FRONT, TEMP_IMAGE_FRONT_FILE_NAME);
            } else {
                bundle.putString(BUNDLE_LOYALTY_CARD_IMAGE_FRONT, null);
            }
        }
        if (!exportIsLimited || exportLimit.contains(BUNDLE_LOYALTY_CARD_IMAGE_BACK)) {
            Bitmap back = getImageBack(context);
            if (back != null) {
                Utils.saveTempImage(context, back, TEMP_IMAGE_BACK_FILE_NAME, Bitmap.CompressFormat.PNG);
                bundle.putString(BUNDLE_LOYALTY_CARD_IMAGE_BACK, TEMP_IMAGE_BACK_FILE_NAME);
            } else {
                bundle.putString(BUNDLE_LOYALTY_CARD_IMAGE_BACK, null);
            }
        }

        return bundle;
    }

    public static LoyaltyCard fromCursor(Context context, Cursor cursor) {
        // id
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        // store
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        // note
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
        // validFrom
        long validFromLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.VALID_FROM));
        Date validFrom = validFromLong > 0 ? new Date(validFromLong) : null;
        // expiry
        long expiryLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY));
        Date expiry = expiryLong > 0 ? new Date(expiryLong) : null;
        // balance
        BigDecimal balance = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
        // balanceType
        int balanceTypeColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE);
        Currency balanceType = !cursor.isNull(balanceTypeColumn) ? Currency.getInstance(cursor.getString(balanceTypeColumn)) : null;
        // cardId
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        // barcodeId
        int barcodeIdColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID);
        String barcodeId = !cursor.isNull(barcodeIdColumn) ? cursor.getString(barcodeIdColumn) : null;
        // barcodeType
        int barcodeTypeColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE);
        CatimaBarcode barcodeType = !cursor.isNull(barcodeTypeColumn) ? CatimaBarcode.fromName(cursor.getString(barcodeTypeColumn)) : null;
        // headerColor
        int headerColorColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR);
        Integer headerColor = !cursor.isNull(headerColorColumn) ? cursor.getInt(headerColorColumn) : null;
        // starStatus
        int starStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS));
        // lastUsed
        long lastUsed = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.LAST_USED));
        // zoomLevel
        int zoomLevel = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ZOOM_LEVEL));
        // zoomLevelWidth
        int zoomLevelWidth = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ZOOM_LEVEL_WIDTH));
        // archiveStatus
        int archiveStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS));

        return new LoyaltyCard(
                id,
                store,
                note,
                validFrom,
                expiry,
                balance,
                balanceType,
                cardId,
                barcodeId,
                barcodeType,
                headerColor,
                starStatus,
                lastUsed,
                zoomLevel,
                zoomLevelWidth,
                archiveStatus,
                null,
                Utils.getCardImageFileName(id, ImageLocationType.icon),
                null,
                Utils.getCardImageFileName(id, ImageLocationType.front),
                null,
                Utils.getCardImageFileName(id, ImageLocationType.back)
        );
    }

    public static boolean isDuplicate(Context context, final LoyaltyCard a, final LoyaltyCard b) {
        // Note: Bitmap comparing is slow, be careful when calling this method
        // Skip lastUsed & zoomLevel*
        return a.id == b.id && // non-nullable int
                a.store.equals(b.store) && // non-nullable String
                a.note.equals(b.note) && // non-nullable String
                Utils.equals(a.validFrom, b.validFrom) && // nullable Date
                Utils.equals(a.expiry, b.expiry) && // nullable Date
                a.balance.equals(b.balance) && // non-nullable BigDecimal
                Utils.equals(a.balanceType, b.balanceType) && // nullable Currency
                a.cardId.equals(b.cardId) && // non-nullable String
                Utils.equals(a.barcodeId, b.barcodeId) && // nullable String
                Utils.equals(a.barcodeType == null ? null : a.barcodeType.format(),
                        b.barcodeType == null ? null : b.barcodeType.format()) && // nullable CatimaBarcode with no overridden .equals(), so we need to check .format()
                Utils.equals(a.headerColor, b.headerColor) && // nullable Integer
                a.starStatus == b.starStatus && // non-nullable int
                a.archiveStatus == b.archiveStatus && // non-nullable int
                nullableBitmapsEqual(a.getImageThumbnail(context), b.getImageThumbnail(context)) && // nullable Bitmap
                nullableBitmapsEqual(a.getImageFront(context), b.getImageFront(context)) && // nullable Bitmap
                nullableBitmapsEqual(a.getImageBack(context), b.getImageBack(context)); // nullable Bitmap
    }

    public static boolean nullableBitmapsEqual(@Nullable Bitmap a, @Nullable Bitmap b) {
        if (a == null && b == null) {
            return true;
        }

        if (a != null && b != null) {
            return a.sameAs(b);
        }

        // One is null and the other isn't, so it's not equal
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "LoyaltyCard{%n  id=%s,%n  store=%s,%n  note=%s,%n  validFrom=%s,%n  expiry=%s,%n"
                        + "  balance=%s,%n  balanceType=%s,%n  cardId=%s,%n  barcodeId=%s,%n  barcodeType=%s,%n"
                        + "  headerColor=%s,%n  starStatus=%s,%n  lastUsed=%s,%n  zoomLevel=%s,%n  zoomLevelWidth=%s,%n  archiveStatus=%s%n"
                        + "  imageThumbnail=%s,%n  imageThumbnailPath=%s,%n  imageFront=%s,%n  imageFrontPath=%s,%n  imageBack=%s,%n  imageBackPath=%s,%n}",
                this.id,
                this.store,
                this.note,
                this.validFrom,
                this.expiry,
                this.balance,
                this.balanceType,
                this.cardId,
                this.barcodeId,
                this.barcodeType != null ? this.barcodeType.format() : null,
                this.headerColor,
                this.starStatus,
                this.lastUsed,
                this.zoomLevel,
                this.zoomLevelWidth,
                this.archiveStatus,
                this.imageThumbnail,
                this.imageThumbnailPath,
                this.imageFront,
                this.imageFrontPath,
                this.imageBack,
                this.imageBackPath
        );
    }
}
