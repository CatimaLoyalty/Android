package protect.card_locker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CatimaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    color: String = colorFromSettings(),
    content: @Composable () -> Unit
) {
    val colorScheme = catimaColorScheme(darkTheme, color)
    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
        )
    )
    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(48.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        }
    )
}

@Composable
private fun PreviewContent(darkTheme: Boolean, color: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("dark: $darkTheme")
        Text("theme: $color")

        Text("This is a text on surface color")
        Text(
            text = "This is a text on surface with primary color",
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "This is a text on surface with secondary color",
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "This is a text on surface with tertiary color",
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = "This is a text on surface with error color",
            color = MaterialTheme.colorScheme.error
        )

        Card(modifier = Modifier.padding(top = 16.dp)) {
            Text("Text inside default container", modifier = Modifier.padding(16.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Text inside primary container", modifier = Modifier.padding(16.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Text inside secondary container", modifier = Modifier.padding(16.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Text inside tertiary container", modifier = Modifier.padding(16.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Text inside tertiary container", modifier = Modifier.padding(16.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inversePrimary),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Text inside inversePrimary container", modifier = Modifier.padding(16.dp))
        }

        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Text inside inverse surface ")
                Text(
                    text = "This is a text on inverse surface with primary color",
                    color = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
    }
}

@Composable
@Preview()
fun PreviewDefaultLight() {
    CatimaTheme(darkTheme = false) {
        PreviewContent(
            darkTheme = false,
            color = "catima_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewDefaultDark() {
    CatimaTheme(darkTheme = true) {
        PreviewContent(
            darkTheme = true,
            color = "catima_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewPinkLight() {
    CatimaTheme(
        darkTheme = false,
        color = "pink_theme"
    ) {
        PreviewContent(
            darkTheme = false,
            color = "pink_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewPinkDark() {
    CatimaTheme(
        darkTheme = true,
        color = "pink_theme"
    ) {
        PreviewContent(
            darkTheme = true,
            color = "pink_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewMagentaLight() {
    CatimaTheme(
        darkTheme = false,
        color = "magenta_theme"
    ) {
        PreviewContent(
            darkTheme = false,
            color = "magenta_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewMagentaDark() {
    CatimaTheme(
        darkTheme = true,
        color = "magenta_theme"
    ) {
        PreviewContent(
            darkTheme = true,
            color = "magenta_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewVioletLight() {
    CatimaTheme(
        darkTheme = false,
        color = "violet_theme"
    ) {
        PreviewContent(
            darkTheme = false,
            color = "violet_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewVioletDark() {
    CatimaTheme(
        darkTheme = true,
        color = "violet_theme"
    ) {
        PreviewContent(
            darkTheme = true,
            color = "violet_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewBlueLight() {
    CatimaTheme(
        darkTheme = false,
        color = "blue_theme"
    ) {
        PreviewContent(
            darkTheme = false,
            color = "blue_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewBlueDark() {
    CatimaTheme(
        darkTheme = true,
        color = "blue_theme"
    ) {
        PreviewContent(
            darkTheme = true,
            color = "blue_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewSkyBlueLight() {
    CatimaTheme(
        darkTheme = false,
        color = "sky_blue_theme"
    ) {
        PreviewContent(
            darkTheme = false,
            color = "sky_blue_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewSkyBlueDark() {
    CatimaTheme(
        darkTheme = true,
        color = "sky_blue_theme"
    ) {
        PreviewContent(
            darkTheme = true,
            color = "sky_blue_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewGreenLight() {
    CatimaTheme(
        darkTheme = false,
        color = "green_theme"
    ) {
        PreviewContent(
            darkTheme = false,
            color = "green_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewGreenDark() {
    CatimaTheme(
        darkTheme = true,
        color = "green_theme"
    ) {
        PreviewContent(
            darkTheme = true,
            color = "green_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewBrownLight() {
    CatimaTheme(
        darkTheme = false,
        color = "brown_theme"
    ) {
        PreviewContent(
            darkTheme = false,
            color = "brown_theme"
        )
    }
}

@Composable
@Preview()
fun PreviewBrownDark() {
    CatimaTheme(
        darkTheme = true,
        color = "brown_theme"
    ) {
        PreviewContent(
            darkTheme = true,
            color = "brown_theme"
        )
    }
}