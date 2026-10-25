package com.example.hocuspocusfocus

import android.app.admin.DeviceAdminReceiver

/** The device admin component that `adb shell dpm set-device-owner` points at; it needs no callbacks. */
class FocusAdminReceiver : DeviceAdminReceiver()
