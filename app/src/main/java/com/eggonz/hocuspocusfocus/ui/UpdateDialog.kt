package com.eggonz.hocuspocusfocus.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.eggonz.hocuspocusfocus.R
import com.eggonz.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/** Tells the user a newer version has been released, with a button to download it. */
@Composable
fun UpdateDialog(
    newVersion: String,
    currentVersion: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_title)) },
        text = { Text(stringResource(R.string.update_message, newVersion, currentVersion)) },
        confirmButton = {
            TextButton(onClick = onDownload) { Text(stringResource(R.string.update_download)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
        },
    )
}

@Preview
@Composable
private fun UpdateDialogPreview() {
    HocusPocusFocusTheme {
        UpdateDialog(newVersion = "0.6.0", currentVersion = "0.5.1", onDownload = {}, onDismiss = {})
    }
}
