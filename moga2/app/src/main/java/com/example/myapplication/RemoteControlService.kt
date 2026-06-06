package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Deprecated Service. Use MyAccessibilityService instead.
 * Logic removed to prevent accidental clipboard pasting.
 */
class RemoteControlService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.w("RemoteControlService", "Old Service Connected! Please disable this in settings and use MyAccessibilityService.")
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
