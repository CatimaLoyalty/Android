package protect.card_locker.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {
    public static final int PERMISSIONS_EXTERNAL_STORAGE = 1;

    public static boolean isNeedRequestStoragePermission(Activity activity) {
        return (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
    }

    public static void requestStoragePermission(Activity activity) {
        if (isNeedRequestStoragePermission(activity) && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_EXTERNAL_STORAGE);
        }
    }
}
