package pl.mlorek.gpwshort.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.mlorek.gpwshort.R
import pl.mlorek.gpwshort.util.ErrorType

@Composable
fun messageFor(errorType: ErrorType): String = when (errorType) {
    ErrorType.NETWORK -> stringResource(R.string.common_error_network)
    ErrorType.PARSING -> stringResource(R.string.common_error_parsing)
    ErrorType.UNKNOWN -> stringResource(R.string.common_error_generic)
}
