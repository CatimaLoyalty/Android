package protect.card_locker.ui.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import protect.card_locker.R

/**
 * Loads a given URL into web browser based on current context
 * @param url URL to load
 */
fun Context.load(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (exception: ActivityNotFoundException) {
        Toast.makeText(this, R.string.failedToOpenUrl, Toast.LENGTH_LONG).show()
        Log.e("Context", "No activity found to handle intent", exception)
    }
}
