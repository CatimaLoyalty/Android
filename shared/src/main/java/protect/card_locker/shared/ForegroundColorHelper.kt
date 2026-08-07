package protect.card_locker.shared

import androidx.core.graphics.ColorUtils

class ForegroundColorHelper {
    companion object {
        const val LUMINANCE_MIDPOINT: Double = 0.5

        fun needsDarkForeground(backgroundColor: Int): Boolean {
            return ColorUtils.calculateLuminance(backgroundColor) > LUMINANCE_MIDPOINT
        }
    }
}