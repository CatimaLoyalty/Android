package protect.card_locker

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.yalantis.ucrop.UCropActivity

class UCropWrapper : UCropActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.applyWindowInsets(findViewById(android.R.id.content))
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val darkMode = Utils.isDarkModeEnabled(this)
        // setup status bar to look like the rest of the app
        setupStatusBar(darkMode)
        // find and check views that we wish to color modify
        // for when we update ucrop or switch to another cropper
        checkViews(darkMode)
        // change toolbar font
        changeToolbarFont()
    }

    private fun setupStatusBar(darkMode: Boolean) {
        if (window == null) {
            return
        }

        if (Build.VERSION.SDK_INT >= 23) {
            val decorView = window.decorView
            val wic = WindowInsetsControllerCompat(window, decorView)
            wic.isAppearanceLightStatusBars = !darkMode
        } else if (!darkMode) {
            window.statusBarColor = ColorUtils.compositeColors(
                Color.argb(127, 0, 0, 0),
                window.statusBarColor
            )
        }
    }

    private fun checkViews(darkMode: Boolean) {
        var view = findViewById<View?>(com.yalantis.ucrop.R.id.wrapper_controls)
        if (view !is FrameLayout) {
            return
        }

        val controls = view
        view = findViewById(com.yalantis.ucrop.R.id.wrapper_states)
        if (view !is LinearLayout) {
            return
        }
        val states = view
        controls.children.firstOrNull { it is AppCompatImageView }?.let {
            // everything gathered and are as expected, now perform color patching
            Utils.patchColors(this)
            val colorSurface = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurface,
                ContextCompat.getColor(
                    this,
                    R.color.md_theme_light_surface
                )
            )
            val colorOnSurface = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                ContextCompat.getColor(
                    this,
                    R.color.md_theme_light_onSurface
                )
            )

            val controlsBackgroundImageDrawable = it.background
            controlsBackgroundImageDrawable.mutate()
            controlsBackgroundImageDrawable.setTint(
                if (darkMode) {
                    colorOnSurface
                } else {
                    colorSurface
                }
            )
            it.background = controlsBackgroundImageDrawable
            states.setBackgroundColor(
                if (darkMode) {
                    colorSurface
                } else {
                    colorOnSurface
                }
            )
        }
    }

    private fun changeToolbarFont() {
        val toolbar = findViewById<View?>(com.yalantis.ucrop.R.id.toolbar_title)
        if (toolbar !is MaterialTextView) {
            return
        }

        val style = intent.getIntExtra(UCROP_TOOLBAR_TYPEFACE_STYLE, -1)
        if (style != -1) {
            toolbar.setTypeface(Typeface.defaultFromStyle(style))
        }
    }

    internal companion object {
        const val UCROP_TOOLBAR_TYPEFACE_STYLE: String = "ucop_toolbar_typeface_style"
    }
}
