package protect.card_locker;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {
    /**
     * Check if storage read permission is needed.
     *
     * This is only necessary on Android 6.0 (Marshmallow) and below. See
     * https://github.com/CatimaLoyalty/Android/issues/979 for more info.
     *
     * @param activity
     * @return
     */
    private static boolean needsStorageReadPermission(Activity activity) {
        // Testing showed this permission wasn't needed for anything Catima did past Marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return false;
        }

        return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if camera permission is needed.
     *
     * @param activity
     * @return
     */
    public static boolean needsCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Call onRequestPermissionsResult after storage read permission was granted.
     * Mocks a successful grant if a grant is not necessary.
     *
     * @param activity
     * @param requestCode
     */
    public static void requestStorageReadPermission(Activity activity, int requestCode) {
        String[] permissions = new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE };
        int[] mockedResults = new int[]{ PackageManager.PERMISSION_GRANTED };

        if (needsStorageReadPermission(activity)) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        } else {
            activity.onRequestPermissionsResult(requestCode, permissions, mockedResults);
        }
    }

    /**
     * Call onRequestPermissionsResult after camera permission was granted.
     * Mocks a successful grant if a grant is not necessary.
     *
     * @param activity
     * @param requestCode
     */
    public static void requestCameraPermission(Activity activity, int requestCode) {
        String[] permissions = new String[]{ Manifest.permission.CAMERA };
        int[] mockedResults = new int[]{ PackageManager.PERMISSION_GRANTED };

        if (needsCameraPermission(activity)) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        } else {
            activity.onRequestPermissionsResult(requestCode, permissions, mockedResults);
        }
    }
}