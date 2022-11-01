package protect.card_locker

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.util.Log
import android.widget.Toast

class OpenWebLinkHandler {
    fun openBrowser(activity: AppCompatActivity, url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.failedToOpenUrl, Toast.LENGTH_LONG).show()
            Log.e(TAG, "No activity found to handle intent", e)
        }
    }

    companion object {
        private const val TAG = "Catima"
    }
}