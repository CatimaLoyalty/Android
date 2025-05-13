package protect.card_locker.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CatimaAboutSection(title: String, message: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row {
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(text = message)
            }
            Text(modifier = Modifier.align(Alignment.CenterVertically),
                text = ">",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}