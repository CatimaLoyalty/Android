package protect.card_locker.cardview;

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

final class LoyaltyCardImageZoomHandler implements View.OnTouchListener {
    private static final float MAX_SCALE = 5f;

    private final ImageView imageView;
    private final Matrix imageMatrix = new Matrix();
    private final float[] imageMatrixValues = new float[9];
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private boolean enabled = false;
    private float fitCenterScale = 1f;
    private float imageScale = 1f;
    private float lastTouchX;
    private float lastTouchY;

    LoyaltyCardImageZoomHandler(ImageView imageView) {
        this.imageView = imageView;
        this.scaleDetector = new ScaleGestureDetector(
                imageView.getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        scale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                        return true;
                    }
                }
        );
        this.gestureDetector = new GestureDetector(
                imageView.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        reset();
                        return true;
                    }
                }
        );

        imageView.setOnTouchListener(this);
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (enabled) {
            imageView.setScaleType(ImageView.ScaleType.MATRIX);
            reset();
        } else {
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageMatrix(null);
        }
    }

    void reset() {
        imageScale = 1f;

        if (!enabled) {
            return;
        }

        if (imageView.getWidth() == 0 || imageView.getHeight() == 0) {
            imageView.post(this::reset);
            return;
        }

        Drawable drawable = imageView.getDrawable();
        if (drawable == null) {
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }

        float viewWidth = imageView.getWidth();
        float viewHeight = imageView.getHeight();
        fitCenterScale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight);
        float dx = (viewWidth - drawableWidth * fitCenterScale) / 2f;
        float dy = (viewHeight - drawableHeight * fitCenterScale) / 2f;

        imageMatrix.reset();
        imageMatrix.postScale(fitCenterScale, fitCenterScale);
        imageMatrix.postTranslate(dx, dy);
        imageView.setImageMatrix(imageMatrix);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (!enabled) {
            return false;
        }

        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && imageScale > 1f) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    if (dx != 0f || dy != 0f) {
                        imageMatrix.postTranslate(dx, dy);
                        clampTranslation();
                        imageView.setImageMatrix(imageMatrix);
                    }
                }
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }

        return true;
    }

    private void scale(float scaleFactor, float focusX, float focusY) {
        float oldImageScale = imageScale;
        imageScale = Math.max(1f, Math.min(imageScale * scaleFactor, MAX_SCALE));
        float matrixScaleFactor = imageScale / oldImageScale;

        imageMatrix.postScale(matrixScaleFactor, matrixScaleFactor, focusX, focusY);
        clampTranslation();
        imageView.setImageMatrix(imageMatrix);
    }

    private void clampTranslation() {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) {
            return;
        }

        imageMatrix.getValues(imageMatrixValues);
        float scale = fitCenterScale * imageScale;
        float scaledWidth = drawable.getIntrinsicWidth() * scale;
        float scaledHeight = drawable.getIntrinsicHeight() * scale;

        imageMatrixValues[Matrix.MTRANS_X] = clampAxis(
                imageMatrixValues[Matrix.MTRANS_X],
                imageView.getWidth(),
                scaledWidth
        );
        imageMatrixValues[Matrix.MTRANS_Y] = clampAxis(
                imageMatrixValues[Matrix.MTRANS_Y],
                imageView.getHeight(),
                scaledHeight
        );
        imageMatrix.setValues(imageMatrixValues);
    }

    private float clampAxis(float translation, float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return (viewSize - contentSize) / 2f;
        }

        float min = viewSize - contentSize;
        return Math.max(min, Math.min(translation, 0f));
    }
}
