package com.salaryflow.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salaryflow.app.R
import com.salaryflow.app.update.UpdateRelease
import java.util.Locale

@Composable
fun AvailableUpdateDialog(
    release: UpdateRelease,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val size = formatUpdateSize(release.sizeBytes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.update_available_title, release.version.toString()))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.update_available_message, size))
                if (release.notes.isNotBlank()) {
                    Text(
                        text = release.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(stringResource(R.string.update_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}

@Composable
fun DownloadingUpdateDialog(progress: Int) {
    val safeProgress = progress.coerceIn(0, 100)
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.update_downloading_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.update_downloading_message, safeProgress))
                LinearProgressIndicator(
                    progress = { safeProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
fun ReadyToInstallDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_ready_title)) },
        text = { Text(stringResource(R.string.update_ready_message)) },
        confirmButton = {
            TextButton(onClick = onInstall) {
                Text(stringResource(R.string.update_install_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}

@Composable
fun UpdateResultDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val (title, message) = when (state) {
        UpdateUiState.UpToDate -> {
            stringResource(R.string.update_latest_title) to
                stringResource(R.string.update_latest_message)
        }

        is UpdateUiState.Failed -> {
            val messageRes = when (state.reason) {
                UpdateFailure.CHECK -> R.string.update_check_failed
                UpdateFailure.DOWNLOAD -> R.string.update_download_failed
                UpdateFailure.INSTALL -> R.string.update_install_failed
            }
            stringResource(R.string.update_failed_title) to stringResource(messageRes)
        }

        else -> return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.update_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

private fun formatUpdateSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes >= 1024L * 1024L -> String.format(
        Locale.getDefault(),
        "%.1f MB",
        bytes.toDouble() / (1024.0 * 1024.0),
    )

    else -> String.format(
        Locale.getDefault(),
        "%.0f KB",
        bytes.toDouble() / 1024.0,
    )
}
