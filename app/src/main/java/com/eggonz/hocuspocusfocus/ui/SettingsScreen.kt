package com.eggonz.hocuspocusfocus.ui

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
import androidx.compose.material3.Switch
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
import com.eggonz.hocuspocusfocus.R
import com.eggonz.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.eggonz.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/**
 * Sharing the app, the alarm sound, the "always use the app guard" toggle (soft lock only), the lock strength (with
 * device-owner removal) and the app version.
 */
@Composable
fun SettingsScreen(
    alarmSoundTitle: String,
    isDeviceOwner: Boolean,
    alwaysUseAppGuard: Boolean,
    isAppGuardOn: Boolean,
    versionName: String,
    onPickAlarmSound: () -> Unit,
    onAlwaysUseAppGuardChange: (Boolean) -> Unit,
    onTurnOnAppGuard: () -> Unit,
    onRemoveDeviceOwner: () -> Unit,
    onShareApp: () -> Unit,
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
                    title = stringResource(R.string.share_app),
                    modifier = Modifier.clickable(onClick = onShareApp),
                    action = { Icon(painterResource(R.drawable.ic_pool_share), contentDescription = null) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingRow(
                    title = stringResource(R.string.alarm_sound),
                    modifier = Modifier.clickable(onClick = onPickAlarmSound),
                    action = { Icon(painterResource(R.drawable.ic_pool_clock_alarm), contentDescription = null) },
                ) {
                    Text(alarmSoundTitle, color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // As device owner the lock is full either way, so there's nothing to choose
                if (!isDeviceOwner) {
                    SettingRow(
                        title = stringResource(R.string.always_use_app_guard),
                        modifier = Modifier.clickable { onAlwaysUseAppGuardChange(!alwaysUseAppGuard) },
                        action = { Switch(checked = alwaysUseAppGuard, onCheckedChange = onAlwaysUseAppGuardChange) },
                    ) {
                        Text(
                            stringResource(R.string.always_use_app_guard_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (alwaysUseAppGuard && !isAppGuardOn) {
                            TextButton(onClick = onTurnOnAppGuard) { Text(stringResource(R.string.turn_on)) }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

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
private fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (content != null) {
                Spacer(Modifier.height(4.dp))
                content()
            }
        }
        if (action != null) {
            Spacer(Modifier.width(16.dp))
            action()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    HocusPocusFocusTheme {
        SettingsScreen(
            alarmSoundTitle = "Default (Spaceline)",
            isDeviceOwner = true,
            alwaysUseAppGuard = false,
            isAppGuardOn = false,
            versionName = "0.1.1",
            onPickAlarmSound = {},
            onAlwaysUseAppGuardChange = {},
            onTurnOnAppGuard = {},
            onRemoveDeviceOwner = {},
            onShareApp = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsSoftLockPreview() {
    HocusPocusFocusTheme {
        SettingsScreen(
            alarmSoundTitle = "Default (Spaceline)",
            isDeviceOwner = false,
            alwaysUseAppGuard = true,
            isAppGuardOn = false,
            versionName = "0.1.1",
            onPickAlarmSound = {},
            onAlwaysUseAppGuardChange = {},
            onTurnOnAppGuard = {},
            onRemoveDeviceOwner = {},
            onShareApp = {},
            onBack = {},
        )
    }
}
