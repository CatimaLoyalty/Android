package protect.card_locker.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import protect.card_locker.R
import protect.card_locker.preferences.Settings

@Composable
@ReadOnlyComposable
fun colorFromSettings(): String {
    val settings = Settings(LocalContext.current)
    return settings.color
}

@Composable
@ReadOnlyComposable
fun catimaColorScheme(
    darkTheme: Boolean,
    color: String,
): ColorScheme {
    return when (color) {
        stringResource(R.string.settings_key_pink_theme) -> pinkColorScheme(darkTheme)
        stringResource(R.string.settings_key_magenta_theme) -> magentaColorScheme(darkTheme)
        stringResource(R.string.settings_key_violet_theme) -> violetColorScheme(darkTheme)
        stringResource(R.string.settings_key_blue_theme) -> blueColorScheme(darkTheme)
        stringResource(R.string.settings_key_sky_blue_theme) -> skyblueColorScheme(darkTheme)
        stringResource(R.string.settings_key_green_theme) -> greenColorScheme(darkTheme)
        stringResource(R.string.settings_key_brown_theme) -> brownColorScheme(darkTheme)
        else -> defaultColorScheme(darkTheme)
    }
}

fun defaultColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
         darkColorScheme(
            primary = Color(0xFFFFB3AE),
            onPrimary = Color(0xFF67020E),
            primaryContainer = Color(0xFF881E22),
            onPrimaryContainer = Color(0xFFFFDAD6),
            inversePrimary = Color(0xFFA83536),
            secondary = Color(0xFFE7BDBA),
            onSecondary = Color(0xFF442928),
            secondaryContainer = Color(0xFF5D3F3D),
            onSecondaryContainer = Color(0xFFFFDAD7),
            tertiary = Color(0xFFE3C28C),
            onTertiary = Color(0xFF402D05),
            tertiaryContainer = Color(0xFF594319),
            onTertiaryContainer = Color(0xFFFFDEA7),
            background = Color(0xFF201A1A),
            onBackground = Color(0xFFECDFDE),
            surface = Color(0xFF201A1A),
            onSurface = Color(0xFFECDFDE),
            surfaceVariant = Color(0xFF524342),
            onSurfaceVariant = Color(0xFFD8C2C0),
            inverseSurface = Color(0xFFECDFDE),
            inverseOnSurface = Color(0xFF201A1A),
            error = Color(0xFFFFB4A9),
            onError = Color(0xFF680003),
            errorContainer = Color(0xFF680003),
            onErrorContainer = Color(0xFFFFDAD4),
            outline = Color(0xFFA08C8B),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFA83536),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDAD6),
            onPrimaryContainer = Color(0xFF410004),
            inversePrimary = Color(0xFFFFB3AE),
            secondary = Color(0xFF775654),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFDAD7),
            onSecondaryContainer = Color(0xFF2C1514),
            tertiary = Color(0xFF735A2E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFDEA7),
            onTertiaryContainer = Color(0xFF281900),
            background = Color(0xFFFCFCFC),
            onBackground = Color(0xFF201A1A),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF201A1A),
            surfaceVariant = Color(0xFFF4DDDB),
            onSurfaceVariant = Color(0xFF524342),
            inverseSurface = Color(0xFF362F2E),
            inverseOnSurface = Color(0xFFFBEEEC),
            error = Color(0xFFBA1B1B),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD4),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF857372),
        )
    }
}

fun pinkColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
         darkColorScheme(
             primary = Color(0xFFFFB2C0),
             onPrimary = Color(0xFF670024),
             primaryContainer = Color(0xFF900036),
             onPrimaryContainer = Color(0xFFFFD9DF),
             inversePrimary = Color(0xFFBC0049),
             secondary = Color(0xFFE5BDC2),
             onSecondary = Color(0xFF43292D),
             secondaryContainer = Color(0xFF5C3F43),
             onSecondaryContainer = Color(0xFFFFD9DE),
             tertiary = Color(0xFFEBBF90),
             onTertiary = Color(0xFF452B08),
             tertiaryContainer = Color(0xFF5F411C),
             onTertiaryContainer = Color(0xFFFFDDB8),
             background = Color(0xFF201A1B),
             onBackground = Color(0xFFECE0E0),
             surface = Color(0xFF201A1B),
             onSurface = Color(0xFFECE0E0),
             surfaceVariant = Color(0xFF524345),
             onSurfaceVariant = Color(0xFFD6C1C3),
             inverseSurface = Color(0xFFECE0E0),
             inverseOnSurface = Color(0xFF201A1B),
             error = Color(0xFFFFB4A9),
             onError = Color(0xFF680003),
             errorContainer = Color(0xFF930006),
             onErrorContainer = Color(0xFFFFDAD4),
             outline = Color(0xFF9F8C8E),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFBC0049),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD9DF),
            onPrimaryContainer = Color(0xFF400013),
            inversePrimary = Color(0xFFFFB2C0),
            secondary = Color(0xFF76565B),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFD9DE),
            onSecondaryContainer = Color(0xFF2B1519),
            tertiary = Color(0xFF795831),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFDDB8),
            onTertiaryContainer = Color(0xFF2C1700),
            background = Color(0xFFFCFCFC),
            onBackground = Color(0xFF201A1B),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF201A1B),
            surfaceVariant = Color(0xFFF4DDDF),
            onSurfaceVariant = Color(0xFF524345),
            inverseSurface = Color(0xFF362F30),
            inverseOnSurface = Color(0xFFFAEEEE),
            error = Color(0xFFBA1B1B),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD4),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF847375),
        )
    }
}

fun magentaColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
         darkColorScheme(
             primary = Color(0xFFFBAAFF),
             onPrimary = Color(0xFF570068),
             primaryContainer = Color(0xFF7B0091),
             onPrimaryContainer = Color(0xFFFFD5FF),
             inversePrimary = Color(0xFF9A25AE),
             secondary = Color(0xFFD7BFD5),
             onSecondary = Color(0xFF3B2B3B),
             secondaryContainer = Color(0xFF534153),
             onSecondaryContainer = Color(0xFFF5DBF2),
             tertiary = Color(0xFFF6B8AE),
             onTertiary = Color(0xFF4C251F),
             tertiaryContainer = Color(0xFF663B34),
             onTertiaryContainer = Color(0xFFFFDAD2),
             background = Color(0xFF1E1A1D),
             onBackground = Color(0xFFE9E0E5),
             surface = Color(0xFF1E1A1D),
             onSurface = Color(0xFFE9E0E5),
             surfaceVariant = Color(0xFF4D444C),
             onSurfaceVariant = Color(0xFFD0C3CC),
             inverseSurface = Color(0xFFE9E0E5),
             inverseOnSurface = Color(0xFF1E1A1D),
             error = Color(0xFFFFB4A9),
             onError = Color(0xFF680003),
             errorContainer = Color(0xFF930006),
             onErrorContainer = Color(0xFFFFDAD4),
             outline = Color(0xFF998E96),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF9A25AE),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD5FF),
            onPrimaryContainer = Color(0xFF350040),
            inversePrimary = Color(0xFFFBAAFF),
            secondary = Color(0xFF6B586B),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF5DBF2),
            onSecondaryContainer = Color(0xFF251626),
            tertiary = Color(0xFF82524A),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFDAD2),
            onTertiaryContainer = Color(0xFF32110C),
            background = Color(0xFFFCFCFC),
            onBackground = Color(0xFF1E1A1D),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF1E1A1D),
            surfaceVariant = Color(0xFFECDEE8),
            onSurfaceVariant = Color(0xFF4D444C),
            inverseSurface = Color(0xFF332F32),
            inverseOnSurface = Color(0xFFF7EEF3),
            error = Color(0xFFBA1B1B),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD4),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF7E747C),
        )
    }
}

fun violetColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
         darkColorScheme(
             primary = Color(0xFFD4BAFF),
             onPrimary = Color(0xFF3E008E),
             primaryContainer = Color(0xFF5727A7),
             onPrimaryContainer = Color(0xFFECDCFF),
             inversePrimary = Color(0xFF6F43BF),
             secondary = Color(0xFFCDC2DB),
             onSecondary = Color(0xFF342D41),
             secondaryContainer = Color(0xFF4B4358),
             onSecondaryContainer = Color(0xFFE9DEF7),
             tertiary = Color(0xFFF0B8C5),
             onTertiary = Color(0xFF4A2530),
             tertiaryContainer = Color(0xFF643A46),
             onTertiaryContainer = Color(0xFFFFD9E2),
             background = Color(0xFF1D1B1F),
             onBackground = Color(0xFFE6E1E5),
             surface = Color(0xFF1D1B1F),
             onSurface = Color(0xFFE6E1E5),
             surfaceVariant = Color(0xFF49454E),
             onSurfaceVariant = Color(0xFFCBC4CF),
             inverseSurface = Color(0xFFE6E1E5),
             inverseOnSurface = Color(0xFF1D1B1F),
             error = Color(0xFFFFB4A9),
             onError = Color(0xFF930006),
             errorContainer = Color(0xFF680003),
             onErrorContainer = Color(0xFFFFDAD4),
             outline = Color(0xFF948E99),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6F43BF),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFECDCFF),
            onPrimaryContainer = Color(0xFF25005B),
            inversePrimary = Color(0xFFD4BAFF),
            secondary = Color(0xFF635B70),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE9DEF7),
            onSecondaryContainer = Color(0xFF1F182B),
            tertiary = Color(0xFF7F525E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD9E2),
            onTertiaryContainer = Color(0xFF32101B),
            background = Color(0xFFFFFBFD),
            onBackground = Color(0xFF1D1B1F),
            surface = Color(0xFFFFFBFD),
            onSurface = Color(0xFF1D1B1F),
            surfaceVariant = Color(0xFFE7E0EB),
            onSurfaceVariant = Color(0xFF49454E),
            inverseSurface = Color(0xFF323033),
            inverseOnSurface = Color(0xFFF5EFF4),
            error = Color(0xFFBA1B1B),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD4),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF7A757F),
        )
    }
}

fun blueColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFB9C3FF),
            onPrimary = Color(0xFF08218A),
            primaryContainer = Color(0xFF293CA0),
            onPrimaryContainer = Color(0xFFDDE0FF),
            inversePrimary = Color(0xFF4355B9),
            secondary = Color(0xFFC4C5DD),
            onSecondary = Color(0xFF2D2F42),
            secondaryContainer = Color(0xFF43465A),
            onSecondaryContainer = Color(0xFFE0E1FA),
            tertiary = Color(0xFFE5BAD7),
            onTertiary = Color(0xFF45263E),
            tertiaryContainer = Color(0xFF5D3C55),
            onTertiaryContainer = Color(0xFFFFD7F3),
            background = Color(0xFF1B1B1F),
            onBackground = Color(0xFFE4E1E6),
            surface = Color(0xFF1B1B1F),
            onSurface = Color(0xFFE4E1E6),
            surfaceVariant = Color(0xFF46464F),
            onSurfaceVariant = Color(0xFFC6C5D0),
            inverseSurface = Color(0xFFE4E1E6),
            inverseOnSurface = Color(0xFF1B1B1F),
            error = Color(0xFFFFB4A9),
            onError = Color(0xFF680003),
            errorContainer = Color(0xFF930006),
            onErrorContainer = Color(0xFFFFDAD4),
            outline = Color(0xFF90909A),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF4355B9),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDDE0FF),
            onPrimaryContainer = Color(0xFF000D61),
            inversePrimary = Color(0xFFB9C3FF),
            secondary = Color(0xFF5B5D71),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE0E1FA),
            onSecondaryContainer = Color(0xFF171A2C),
            tertiary = Color(0xFF77536D),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD7F3),
            onTertiaryContainer = Color(0xFF2D1228),
            background = Color(0xFFFEFBFF),
            onBackground = Color(0xFF1B1B1F),
            surface = Color(0xFFFEFBFF),
            onSurface = Color(0xFF1B1B1F),
            surfaceVariant = Color(0xFFE3E1EC),
            onSurfaceVariant = Color(0xFF46464F),
            inverseSurface = Color(0xFF303034),
            inverseOnSurface = Color(0xFFF3F0F5),
            error = Color(0xFFBA1B1B),
            errorContainer = Color(0xFFFFDAD4),
            onError = Color(0xFFFFFFFF),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF767680),
        )
    }
}

fun skyblueColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF8BCEFF),
            onPrimary = Color(0xFF003450),
            primaryContainer = Color(0xFF004B71),
            onPrimaryContainer = Color(0xFFC8E6FF),
            inversePrimary = Color(0xFF006494),
            secondary = Color(0xFFB7C8D8),
            onSecondary = Color(0xFF22323F),
            secondaryContainer = Color(0xFF384956),
            onSecondaryContainer = Color(0xFFD3E4F5),
            tertiary = Color(0xFFCFBFE8),
            onTertiary = Color(0xFF362B4B),
            tertiaryContainer = Color(0xFF4D4162),
            onTertiaryContainer = Color(0xFFECDCFF),
            background = Color(0xFF1A1C1E),
            onBackground = Color(0xFFE2E2E5),
            surface = Color(0xFF1A1C1E),
            onSurface = Color(0xFFE2E2E5),
            surfaceVariant = Color(0xFF41474D),
            onSurfaceVariant = Color(0xFFC1C7CE),
            inverseSurface = Color(0xFFE2E2E5),
            inverseOnSurface = Color(0xFF1A1C1E),
            error = Color(0xFFFFB4A9),
            onError = Color(0xFF680003),
            errorContainer = Color(0xFF930006),
            onErrorContainer = Color(0xFFFFDAD4),
            outline = Color(0xFF8B9198),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF006494),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFC8E6FF),
            onPrimaryContainer = Color(0xFF001E31),
            inversePrimary = Color(0xFF8BCEFF),
            secondary = Color(0xFF50606E),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD3E4F5),
            onSecondaryContainer = Color(0xFF0C1D29),
            tertiary = Color(0xFF65597B),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFECDCFF),
            onTertiaryContainer = Color(0xFF201634),
            background = Color(0xFFFCFCFF),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFFCFCFF),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFDEE3EA),
            onSurfaceVariant = Color(0xFF41474D),
            inverseSurface = Color(0xFF2F3032),
            inverseOnSurface = Color(0xFFF0F0F3),
            error = Color(0xFFBA1B1B),
            errorContainer = Color(0xFFFFDAD4),
            onError = Color(0xFFFFFFFF),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF72787E),
        )
    }
}

fun greenColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF78DC77),
            onPrimary = Color(0xFF003907),
            primaryContainer = Color(0xFF00530F),
            onPrimaryContainer = Color(0xFF93F990),
            inversePrimary = Color(0xFF006E17),
            secondary = Color(0xFFB9CCB3),
            onSecondary = Color(0xFF253423),
            secondaryContainer = Color(0xFF3B4B38),
            onSecondaryContainer = Color(0xFFD5E8CE),
            tertiary = Color(0xFFA1CFD5),
            onTertiary = Color(0xFF00363B),
            tertiaryContainer = Color(0xFF1E4D52),
            onTertiaryContainer = Color(0xFFBCEBF0),
            background = Color(0xFF1A1C19),
            onBackground = Color(0xFFE2E3DD),
            surface = Color(0xFF1A1C19),
            onSurface = Color(0xFFE2E3DD),
            surfaceVariant = Color(0xFF424840),
            onSurfaceVariant = Color(0xFFC2C8BD),
            inverseSurface = Color(0xFFE2E3DD),
            inverseOnSurface = Color(0xFF1A1C19),
            error = Color(0xFFFFB4A9),
            onError = Color(0xFF680003),
            errorContainer = Color(0xFF930006),
            onErrorContainer = Color(0xFFFFDAD4),
            outline = Color(0xFF8C9288),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF006E17),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF93F990),
            onPrimaryContainer = Color(0xFF002203),
            inversePrimary = Color(0xFF78DC77),
            secondary = Color(0xFF52634F),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD5E8CE),
            onSecondaryContainer = Color(0xFF101F0F),
            tertiary = Color(0xFF38656A),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFBCEBF0),
            onTertiaryContainer = Color(0xFF001F23),
            background = Color(0xFFFCFDF6),
            onBackground = Color(0xFF1A1C19),
            surface = Color(0xFFFCFDF6),
            onSurface = Color(0xFF1A1C19),
            surfaceVariant = Color(0xFFDEE5D8),
            onSurfaceVariant = Color(0xFF424840),
            inverseSurface = Color(0xFF2F312D),
            inverseOnSurface = Color(0xFFF0F1EB),
            error = Color(0xFFBA1B1B),
            errorContainer = Color(0xFFFFDAD4),
            onError = Color(0xFFFFFFFF),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF73796F),
        )
    }
}

fun brownColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFFFB598),
            onPrimary = Color(0xFF5C1A00),
            primaryContainer = Color(0xFF7B2E0D),
            onPrimaryContainer = Color(0xFFFFDBCD),
            inversePrimary = Color(0xFF9A4523),
            secondary = Color(0xFFE7BEB0),
            onSecondary = Color(0xFF442A20),
            secondaryContainer = Color(0xFF5D4035),
            onSecondaryContainer = Color(0xFFFFDBCD),
            tertiary = Color(0xFFD5C78E),
            onTertiary = Color(0xFF383005),
            tertiaryContainer = Color(0xFF50461A),
            onTertiaryContainer = Color(0xFFF1E2A7),
            background = Color(0xFF201A18),
            onBackground = Color(0xFFEDE0DC),
            surface = Color(0xFF201A18),
            onSurface = Color(0xFFEDE0DC),
            surfaceVariant = Color(0xFF52433E),
            onSurfaceVariant = Color(0xFFD8C2BB),
            inverseSurface = Color(0xFFEDE0DC),
            inverseOnSurface = Color(0xFF201A18),
            error = Color(0xFFFFB4A9),
            onError = Color(0xFF680003),
            errorContainer = Color(0xFF930006),
            onErrorContainer = Color(0xFFFFDAD4),
            outline = Color(0xFFA08C86),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF9A4523),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDBCD),
            onPrimaryContainer = Color(0xFF380C00),
            inversePrimary = Color(0xFFFFB598),
            secondary = Color(0xFF77574C),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFDBCD),
            onSecondaryContainer = Color(0xFF2C160D),
            tertiary = Color(0xFF695E2F),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFF1E2A7),
            onTertiaryContainer = Color(0xFF221B00),
            background = Color(0xFFFCFCFC),
            onBackground = Color(0xFF201A18),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF201A18),
            surfaceVariant = Color(0xFFF5DED6),
            onSurfaceVariant = Color(0xFF52433E),
            inverseSurface = Color(0xFF362F2D),
            inverseOnSurface = Color(0xFFFCEEEA),
            error = Color(0xFFBA1B1B),
            errorContainer = Color(0xFFFFDAD4),
            onError = Color(0xFFFFFFFF),
            onErrorContainer = Color(0xFF410001),
            outline = Color(0xFF85736D),
        )
    }
}