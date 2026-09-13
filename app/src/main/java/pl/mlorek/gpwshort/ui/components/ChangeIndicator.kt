package pl.mlorek.gpwshort.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.mlorek.gpwshort.R
import pl.mlorek.gpwshort.domain.model.ChangeDirection
import pl.mlorek.gpwshort.ui.theme.ChangeColors
import pl.mlorek.gpwshort.util.formatSignedPercentagePoints

@Composable
fun ChangeIndicator(
    direction: ChangeDirection,
    changePercentagePoints: Double?,
    modifier: Modifier = Modifier,
) {
    val color = when (direction) {
        ChangeDirection.UP -> ChangeColors.Up
        ChangeDirection.DOWN -> ChangeColors.Down
        ChangeDirection.NONE -> ChangeColors.Neutral
    }
    val icon = when (direction) {
        ChangeDirection.UP -> Icons.Default.ArrowUpward
        ChangeDirection.DOWN -> Icons.Default.ArrowDownward
        ChangeDirection.NONE -> Icons.Default.Remove
    }
    val contentDescription = when (direction) {
        ChangeDirection.UP -> stringResource(R.string.common_change_up)
        ChangeDirection.DOWN -> stringResource(R.string.common_change_down)
        ChangeDirection.NONE -> stringResource(R.string.common_change_none)
    }

    Row(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.width(16.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = changePercentagePoints?.let(::formatSignedPercentagePoints) ?: "—",
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
