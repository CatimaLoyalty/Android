package protect.card_locker.compose

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import protect.card_locker.R
import protect.card_locker.preferences.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatimaTopAppBar(title: String, onBackPressedDispatcher: OnBackPressedDispatcher?) {
    // Use pure black in OLED theme
    val context = LocalContext.current
    val settings = Settings(context)
    val isDarkMode = when (settings.theme) {
        AppCompatDelegate.MODE_NIGHT_NO -> false
        AppCompatDelegate.MODE_NIGHT_YES -> true
        else -> isSystemInDarkTheme()
    }

    val appBarColors = if (isDarkMode && settings.oledDark) {
        TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Black)
    } else {
        TopAppBarDefaults.topAppBarColors()
    }

    TopAppBar(
        modifier = Modifier.testTag("topbar_catima"),
        title = { Text(text = title) },
        colors = appBarColors,
        navigationIcon = {
            if (onBackPressedDispatcher != null) {
                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatimaTopAppBarWithOverflowMenuToImageGallery(title: String, onBackPressedDispatcher: OnBackPressedDispatcher?, imagePath: String) {
    // Use pure black in OLED theme
    val context = LocalContext.current
    val settings = Settings(context)
    val isDarkMode = when (settings.theme) {
        AppCompatDelegate.MODE_NIGHT_NO -> false
        AppCompatDelegate.MODE_NIGHT_YES -> true
        else -> isSystemInDarkTheme()
    }

    val appBarColors = if (isDarkMode && settings.oledDark) {
        TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Black)
    } else {
        TopAppBarDefaults.topAppBarColors()
    }
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.testTag("topbar_with_overflow_menu_catima"),
        title = { Text(text = title) },
        colors = appBarColors,
        navigationIcon = {
            if (onBackPressedDispatcher != null) {
                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            IconButton(
                content = {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Overflow menu icon")},
                onClick = {showMenu = !showMenu})
            DropdownMenu(expanded = showMenu,
                onDismissRequest = { showMenu = false }){
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.open_in_gallery)) },
                    onClick = {
                        val viewInGalleryIntent = Intent(Intent.ACTION_VIEW)
                        viewInGalleryIntent.setDataAndType(FileProvider.getUriForFile(context, context.packageName, context.getFileStreamPath(imagePath)), "image/*")
                        viewInGalleryIntent.setFlags(FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(viewInGalleryIntent)
                    }
                )
            }
        }
    )
}