package com.example.hocuspocusfocus

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName

/**
 * Locks the phone to this app via lock task mode.
 *
 * As device owner this is a full kiosk lock: no Home, Recents, notification shade, status bar
 * info or power menu, and no exit gesture. Otherwise [lock] falls back to screen pinning, which
 * asks the user to confirm and can be exited by holding Back + Recents.
 *
 * A session's allowed apps are opened into this app's task (see [MainActivity]), which both modes
 * allow; as device owner they're also whitelisted, so apps that insist on their own task open too.
 * Without device owner, a session with allowed apps isn't pinned while the [AppGuardService] is on,
 * since pinning would block those apps; the guard keeps the phone to them instead. Sessions without
 * allowed apps can skip pinning (and its "App is pinned" notice) the same way, if the user asks.
 */
class FocusLock(private val activity: Activity) {

    private val dpm = activity.getSystemService(DevicePolicyManager::class.java)
    private val activityManager = activity.getSystemService(ActivityManager::class.java)
    private val admin = ComponentName(activity, FocusAdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(activity.packageName)

    private val isLocked: Boolean
        get() = activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    /** Allows this app into full lock task mode, with every system UI feature turned off. */
    fun applyPolicies() {
        if (!isDeviceOwner) return
        dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
    }

    /**
     * Locks the phone to this app and the [allowedApps] package names, with the app guard instead of screen pinning
     * when there are allowed apps or [alwaysUseAppGuard].
     */
    fun lock(allowedApps: List<String>, alwaysUseAppGuard: Boolean) {
        if (isDeviceOwner) {
            dpm.setLockTaskPackages(admin, arrayOf(activity.packageName) + allowedApps)
        } else if ((allowedApps.isNotEmpty() || alwaysUseAppGuard) && AppGuardService.isOn(activity)) {
            return
        }
        if (!isLocked) activity.startLockTask()
    }

    fun unlock() {
        if (isLocked) activity.stopLockTask()
        applyPolicies()
    }

    /** Gives up device owner, so the app can be uninstalled normally again. */
    @Suppress("DEPRECATION") // still the only way for an app to drop its own device owner status
    fun releaseDeviceOwner() {
        dpm.setLockTaskPackages(admin, emptyArray())
        dpm.clearDeviceOwnerApp(activity.packageName)
    }
}
