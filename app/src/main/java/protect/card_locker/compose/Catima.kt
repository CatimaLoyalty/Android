package protect.card_locker.compose

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import protect.card_locker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatimaTopAppBar(title: String, onBackPressedDispatcher: OnBackPressedDispatcher?) {
    TopAppBar(
        title = {
            Text(text = title)
        },
        navigationIcon = { if (onBackPressedDispatcher != null) {
            IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        } else null }
    )
}