package protect.card_locker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArrayMap;

public class AboutOpenLinkHandler {

    private static final String TAG = "Catima";

    private final ArrayMap<Integer, String> viewIdToUrlMap = new ArrayMap<Integer, String>();

    AboutOpenLinkHandler() {
        viewIdToUrlMap.put(R.id.version_history, "https://catima.app/changelog/");
        viewIdToUrlMap.put(R.id.translate, "https://hosted.weblate.org/engage/catima/");
        viewIdToUrlMap.put(R.id.license, "https://github.com/CatimaLoyalty/Android/blob/master/LICENSE");
        viewIdToUrlMap.put(R.id.repo, "https://github.com/CatimaLoyalty/Android/");
        viewIdToUrlMap.put(R.id.privacy, "https://catima.app/privacy-policy/");
        viewIdToUrlMap.put(R.id.report_error, "https://github.com/CatimaLoyalty/Android/issues");
        viewIdToUrlMap.put(R.id.rate, "https://play.google.com/store/apps/details?id=me.hackerchick.catima");
    }

    public void open(AppCompatActivity activity, View view) {
        String url = viewIdToUrlMap.get(view.getId());
        if (url == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.failedToOpenUrl, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle intent", e);
        }
    }
}
