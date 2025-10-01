package protect.card_locker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

object OpenWebLinkHandler {
    fun openURL(activity: Activity, url: String) {
        if (url.isBlank()) {
            Toast.makeText(activity, "Invalid URL", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = url.trim().toUri()
                // Ensure it opens in browser, not your own app
                addCategory(Intent.CATEGORY_BROWSABLE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Check if there is any activity that can handle this intent
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "No app found to open URL", Toast.LENGTH_SHORT).show()
            }

        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, "No application found to open link", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(activity, "Failed to open link", Toast.LENGTH_SHORT).show()
        }
    }
}
