package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/** The alarm sound, the lock strength (with device-owner removal) and the app version. */
@Composable
fun SettingsScreen(
    alarmSoundTitle: String,
    isDeviceOwner: Boolean,
    versionName: String,
    onPickAlarmSound: () -> Unit,
    onRemoveDeviceOwner: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmRemoval by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.safeDrawingPadding().padding(vertical = 12.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = DisplayFontFamily,
                    )
                }
                Spacer(Modifier.height(16.dp))

                SettingRow(
                    title = stringResource(R.string.alarm_sound),
                    modifier = Modifier.clickable(onClick = onPickAlarmSound),
                ) {
                    Text(alarmSoundTitle, color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingRow(title = stringResource(R.string.lock_mode)) {
                    Text(
                        stringResource(if (isDeviceOwner) R.string.full_lock else R.string.soft_lock_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isDeviceOwner) {
                        TextButton(
                            onClick = { confirmRemoval = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text(stringResource(R.string.remove_device_owner))
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text(stringResource(R.string.remove_device_owner)) },
            text = { Text(stringResource(R.string.remove_device_owner_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemoval = false
                    onRemoveDeviceOwner()
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoval = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SettingRow(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    HocusPocusFocusTheme {
        SettingsScreen(
            alarmSoundTitle = "Default (Morning Glory)",
            isDeviceOwner = true,
            versionName = "0.1.1",
            onPickAlarmSound = {},
            onRemoveDeviceOwner = {},
            onBack = {},
        )
    }
}
