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

    fun lock() {
        if (!isLocked) activity.startLockTask()
    }

    fun unlock() {
        if (isLocked) activity.stopLockTask()
    }

    /** Gives up device owner, so the app can be uninstalled normally again. */
    @Suppress("DEPRECATION") // still the only way for an app to drop its own device owner status
    fun releaseDeviceOwner() {
        dpm.setLockTaskPackages(admin, emptyArray())
        dpm.clearDeviceOwnerApp(activity.packageName)
    }
}
