package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {
    companion object { var instance: MyAccessibilityService? = null }

    override fun onServiceConnected() { 
        super.onServiceConnected()
        instance = this 
    }

    // ميزة الكتابة الاحتياطية للمواقع إذا لم يكن الكيبورد مفعلاً
    fun sendTextFallback(text: String) {
        Handler(Looper.getMainLooper()).post {
            val root = rootInActiveWindow ?: return@post
            val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findAnyEditable(root)
            if (node != null) {
                val bundle = Bundle()
                bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            }
        }
    }

    private fun findAnyEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findAnyEditable(child)
            if (found != null) return found
        }
        return null
    }

    fun dispatchGestureClick(x: Int, y: Int) {
        Handler(Looper.getMainLooper()).post {
            val path = Path()
            path.moveTo(x.toFloat(), y.toFloat())
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
