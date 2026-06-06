package com.example.myapplication

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

class RemoteKeyboardService : InputMethodService() {

    companion object {
        private var instance: RemoteKeyboardService? = null

        fun sendText(text: String): Boolean {
            val ime = instance ?: return false
            val ic = ime.currentInputConnection ?: return false
            
            // Iterate through characters to handle spaces specially for browser compatibility
            for (char in text) {
                if (char == ' ') {
                    // Send space as a hardware-like key event to ensure it's accepted in browser fields (like Email)
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE))
                } else {
                    ic.commitText(char.toString(), 1)
                }
            }
            
            // Immediate cursor fix for RTL
            ic.setSelection(1000, 1000)
            
            // Delayed cursor fix to override browser auto-reset (fixes reversed words)
            applyCursorFix(50)
            return true
        }

        fun replaceText(text: String): Boolean {
            val ime = instance ?: return false
            val ic = ime.currentInputConnection ?: return false
            ic.beginBatchEdit()
            ic.setSelection(0, 1000)
            ic.commitText(text, 1)
            ic.setSelection(1000, 1000)
            ic.endBatchEdit()
            
            applyCursorFix(50)
            return true
        }

        private fun applyCursorFix(delay: Long) {
            val ime = instance ?: return
            Handler(Looper.getMainLooper()).postDelayed({
                val ic = ime.currentInputConnection ?: return@postDelayed
                ic.setSelection(1000, 1000)
                // محاكاة مفتاح النهاية لضمان بقاء الموشر في جهة اليسار
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END))
            }, delay)
        }

        fun sendKey(keyCode: Int): Boolean {
            val ime = instance ?: return false
            val ic = ime.currentInputConnection ?: return false
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            if (keyCode != KeyEvent.KEYCODE_DEL) {
                applyCursorFix(50)
            }
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onEvaluateFullscreenMode(): Boolean = false
    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onCreateInputView(): View {
        // إنشاء شريط يظهر للنظام أن الكيبورد نشط (يحل مشكلة اختفاء المؤشر في Chrome)
        val frame = FrameLayout(this)
        val density = resources.displayMetrics.density
        frame.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (40 * density).toInt())
        frame.setBackgroundColor(Color.parseColor("#EE333333"))
        
        val tv = TextView(this)
        tv.text = "Remote Keyboard Active"
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        frame.addView(tv)
        
        return frame
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }
}
