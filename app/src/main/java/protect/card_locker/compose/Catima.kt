package protect.card_locker.compose

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
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
import protect.card_locker.ImageLocationType
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.preferences.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatimaTopAppBar(title: String,
                    onBackPressedDispatcher: OnBackPressedDispatcher?,
                    overflowMenuActions : Map<OverflowMenuEntry, Map<OverflowMenuParameter, Any>>? = null) {
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
        },
        actions = {
            if(!overflowMenuActions.isNullOrEmpty()){
                var showMenu by remember { mutableStateOf(false) }
                var showIcon by remember { mutableStateOf(true) }
                IconButton(onClick = {showMenu = !showMenu}){
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Overflow menu icon",
                        modifier = Modifier.visible(showIcon)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }){
                    if(overflowMenuActions.contains(OverflowMenuEntry.IMAGE_GALLERY) && overflowMenuActions[OverflowMenuEntry.IMAGE_GALLERY] != null){
                        val imageGalleryParameters = overflowMenuActions[OverflowMenuEntry.IMAGE_GALLERY]
                        if(imageGalleryParameters != null){
                            val cardId = imageGalleryParameters[OverflowMenuParameter.LOYALTY_CARD_ID] as? Int
                            val imageLocationType = imageGalleryParameters[OverflowMenuParameter.IMAGE_LOCATION_TYPE] as? ImageLocationType
                            if(cardId != null && imageLocationType != null) {
                                showIcon = true
                                DropdownMenuItem(onClick = {openInImageGallery(context, cardId, imageLocationType)},
                                    text = { Text(stringResource(R.string.open_in_gallery)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

fun openInImageGallery(context: Context, cardId: Int, imageLocationType: ImageLocationType){
    val imagePath = Utils.getCardImageFileName(cardId, imageLocationType)
    val viewInGalleryIntent = Intent(Intent.ACTION_VIEW)
    viewInGalleryIntent.setDataAndType(FileProvider.getUriForFile(context, context.packageName, context.getFileStreamPath(imagePath)), "image/*")
    viewInGalleryIntent.setFlags(FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(viewInGalleryIntent)
}