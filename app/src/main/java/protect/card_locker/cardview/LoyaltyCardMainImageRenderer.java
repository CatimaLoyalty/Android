package protect.card_locker.cardview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.nio.charset.Charset;

import protect.card_locker.*;
import protect.card_locker.async.TaskHandler;

final class LoyaltyCardMainImageRenderer {
    private final Context context;
    private final TaskHandler tasks;
    private final BarcodeImageWriterResultCallback barcodeCallback;

    LoyaltyCardMainImageRenderer(
            Context context,
            TaskHandler tasks,
            BarcodeImageWriterResultCallback barcodeCallback
    ) {
        this.context = context;
        this.tasks = tasks;
        this.barcodeCallback = barcodeCallback;
    }

    void renderCurrent(
            LoyaltyCardImageType imageType,
            Bitmap frontImageBitmap,
            Bitmap backImageBitmap,
            CatimaBarcode format,
            Charset barcodeEncoding,
            String cardIdString,
            String barcodeIdString,
            ImageView barcodeRenderTarget,
            TextView mainImageDescription,
            MaterialCardView mainCardView,
            boolean isFullscreen,
            boolean waitForResize
    ) {
        if (imageType == LoyaltyCardImageType.NONE) {
            // With no renderable media left, show the raw card ID instead of an empty card area.
            barcodeRenderTarget.setVisibility(View.GONE);
            mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            mainImageDescription.setTextColor(
                    MaterialColors.getColor(
                            mainImageDescription,
                            com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
            );
            mainImageDescription.setText(cardIdString);
            return;
        }

        if (imageType == LoyaltyCardImageType.BARCODE) {
            barcodeRenderTarget.setBackgroundColor(Color.WHITE);
            mainCardView.setCardBackgroundColor(Color.WHITE);
            mainImageDescription.setTextColor(context.getResources().getColor(R.color.md_theme_light_onSurfaceVariant));

            if (waitForResize) {
                redrawBarcodeAfterResize(
                        barcodeRenderTarget,
                        barcodeIdString,
                        cardIdString,
                        format,
                        barcodeEncoding,
                        !isFullscreen,
                        isFullscreen
                );
            } else {
                drawBarcode(
                        barcodeRenderTarget,
                        barcodeIdString,
                        cardIdString,
                        format,
                        barcodeEncoding,
                        !isFullscreen,
                        isFullscreen
                );
            }

            mainImageDescription.setText(cardIdString);
            barcodeRenderTarget.setContentDescription(
                    context.getString(R.string.barcodeImageDescriptionWithType, format.prettyName())
            );
        } else if (imageType == LoyaltyCardImageType.IMAGE_FRONT) {
            barcodeRenderTarget.setImageBitmap(frontImageBitmap);
            barcodeRenderTarget.setBackgroundColor(Color.TRANSPARENT);
            mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            mainImageDescription.setTextColor(
                    MaterialColors.getColor(
                            mainImageDescription,
                            com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
            );
            mainImageDescription.setText(context.getString(R.string.frontImageDescription));
            barcodeRenderTarget.setContentDescription(context.getString(R.string.frontImageDescription));
        } else if (imageType == LoyaltyCardImageType.IMAGE_BACK) {
            barcodeRenderTarget.setImageBitmap(backImageBitmap);
            barcodeRenderTarget.setBackgroundColor(Color.TRANSPARENT);
            mainCardView.setCardBackgroundColor(Color.TRANSPARENT);
            mainImageDescription.setTextColor(
                    MaterialColors.getColor(
                            mainImageDescription,
                            com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
            );
            mainImageDescription.setText(context.getString(R.string.backImageDescription));
            barcodeRenderTarget.setContentDescription(context.getString(R.string.backImageDescription));
        } else {
            throw new IllegalArgumentException("Unknown image type: " + imageType);
        }

        barcodeRenderTarget.setVisibility(View.VISIBLE);
    }

    private void redrawBarcodeAfterResize(
            ImageView barcodeRenderTarget,
            String barcodeIdString,
            String cardIdString,
            CatimaBarcode format,
            Charset barcodeEncoding,
            boolean addPadding,
            boolean isFullscreen
    ) {
        if (format == null) {
            return;
        }

        // Barcode dimensions depend on the final ImageView size, so wait for layout before rendering.
        barcodeRenderTarget.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        barcodeRenderTarget.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        drawBarcode(
                                barcodeRenderTarget,
                                barcodeIdString,
                                cardIdString,
                                format,
                                barcodeEncoding,
                                addPadding,
                                isFullscreen
                        );
                    }
                });
    }

    private void drawBarcode(
            ImageView barcodeRenderTarget,
            String barcodeIdString,
            String cardIdString,
            CatimaBarcode format,
            Charset barcodeEncoding,
            boolean addPadding,
            boolean isFullscreen
    ) {
        // Barcodes are regenerated eagerly because zoom/fullscreen changes affect the output bitmap.
        tasks.flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false);
        if (format == null) {
            return;
        }

        BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(
                context.getApplicationContext(),
                barcodeRenderTarget,
                barcodeIdString != null ? barcodeIdString : cardIdString,
                format,
                barcodeEncoding,
                null,
                false,
                barcodeCallback,
                addPadding,
                isFullscreen
        );
        tasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
    }
}
