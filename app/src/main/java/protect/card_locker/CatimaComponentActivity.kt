package protect.card_locker

import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import protect.card_locker.preferences.Settings

open class CatimaComponentActivity() : ComponentActivity() {
    fun fixedEdgeToEdge() {
        // Fix edge-to-edge
        // When overriding onCreate this does not correctly get applied, which is why it is its own function

        // We explicitly need to set the systemBarStyle ourselves, to prevent issues where Android
        // for example renders white icons on top of a white statusbar (or black on black)
        val settings = Settings(this)
        val systemBarStyle = when (settings.theme) {
            AppCompatDelegate.MODE_NIGHT_NO ->
                SystemBarStyle.light(
                    scrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                )
            AppCompatDelegate.MODE_NIGHT_YES ->
                SystemBarStyle.dark(
                    scrim = Color.TRANSPARENT,
                )
            else ->
                SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT
                )
        }

        enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle
        )
    }
}