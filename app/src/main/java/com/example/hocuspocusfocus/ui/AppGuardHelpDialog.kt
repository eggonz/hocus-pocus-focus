package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/** The steps to turn on the app guard, with buttons to the settings pages where they're done. */
@Composable
fun AppGuardHelpDialog(
    mayBeRestricted: Boolean,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_guard_help_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.app_guard_help_turn_on))
                if (mayBeRestricted) {
                    Text(stringResource(R.string.app_guard_help_restricted))
                    TextButton(onClick = onOpenAppInfo) { Text(stringResource(R.string.app_info)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenAccessibilitySettings) { Text(stringResource(R.string.open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun AppGuardHelpPreview() {
    HocusPocusFocusTheme {
        AppGuardHelpDialog(mayBeRestricted = false, onOpenAppInfo = {}, onOpenAccessibilitySettings = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun AppGuardHelpRestrictedPreview() {
    HocusPocusFocusTheme {
        AppGuardHelpDialog(mayBeRestricted = true, onOpenAppInfo = {}, onOpenAccessibilitySettings = {}, onDismiss = {})
    }
}
