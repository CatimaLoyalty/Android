package protect.card_locker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import protect.card_locker.AboutContent
import protect.card_locker.AboutEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun About(
    navigateUp: () -> Unit,
    open: (AboutEntry) -> Unit,
) {
    val content = AboutContent(LocalContext.current)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content.pageTitle) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(content.items) { item ->
                ListItem(item, open)
            }
        }
    }
}

@Composable
private fun ListItem(
    item: AboutEntry,
    open: (AboutEntry) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open(item) }
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            )
    ) {
        Column {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(item.description)
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun PreviewAbout() {
    CatimaTheme {
        About({}, {})
    }
}
