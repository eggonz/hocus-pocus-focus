package com.example.hocuspocusfocus

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.InCallService
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * The app guard: during a session, brings this app back whenever an app the session doesn't allow
 * comes to the front. Without device owner it stands in for screen pinning in routine sessions (and, if
 * the user asks, in all of them), since pinning allows a single task and so blocks apps that always
 * open in their own.
 */
class AppGuardService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val shownPackage = event.packageName?.toString() ?: return
        val allowedApps = FocusViewModel.savedSessionAllowedApps(this, System.currentTimeMillis()) ?: return
        if (shownPackage in allowedApps || shownPackage in alwaysAllowedPackages()) return
        // MainActivity is singleTask, so this brings its task back to the front
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onInterrupt() = Unit

    // What a session shouldn't block: the system UI (shade, lock screen, system dialogs), permission prompts,
    // keyboards, and phone calls
    private fun alwaysAllowedPackages(): Set<String> = buildSet {
        add(packageName)
        addAll(SYSTEM_PACKAGES)
        addAll(getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName })
        addAll(
            packageManager.queryIntentServices(Intent(InCallService.SERVICE_INTERFACE), 0)
                .map { it.serviceInfo.packageName },
        )
    }

    companion object {
        // The permission prompt's package isn't public API, so both known names are listed
        private val SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )

        /** Whether the user has turned the app guard on in the accessibility settings. */
        fun isOn(context: Context): Boolean {
            // Reads the setting the toggle writes, since AccessibilityManager only lists services once they're bound,
            // which happens a moment after the toggle (or an app update), so a check on resume would miss it
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val appGuard = ComponentName(context, AppGuardService::class.java)
            return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == appGuard }
        }
    }
}
