package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartStop: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnKeyboard: Button
    private lateinit var tvStatus: TextView
    private var isStreaming = false

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
            }
            ContextCompat.startForegroundService(this, intent)
            isStreaming = true
            btnStartStop.text = "Stop Streaming"
            tvStatus.text = "Streaming Live..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartStop = findViewById(R.id.btnStartStop)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnKeyboard = findViewById(R.id.btnKeyboard)
        tvStatus = findViewById(R.id.tvStatus)

        btnStartStop.setOnClickListener {
            if (isStreaming) {
                stopService(Intent(this, ScreenCaptureService::class.java))
                isStreaming = false
                btnStartStop.text = "Start Streaming"
                tvStatus.text = "Not Streaming"
            } else {
                val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                projectionLauncher.launch(mpManager.createScreenCaptureIntent())
            }
        }

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnKeyboard.setOnClickListener {
            if (hasSecureSettingsPermission()) {
                try {
                    val componentName = ComponentName(this, RemoteKeyboardService::class.java).flattenToString()
                    
                    // Enable the keyboard
                    val enabledImeList = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
                    if (!enabledImeList.contains(componentName)) {
                        Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS, "$enabledImeList:$componentName")
                    }
                    
                    // Set it as default
                    Settings.Secure.putString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD, componentName)
                    Toast.makeText(this, "Keyboard auto-enabled via ADB", Toast.LENGTH_SHORT).show()
                    onResume() // Refresh UI
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to auto-enable: ${e.message}", Toast.LENGTH_LONG).show()
                    showImePicker()
                }
            } else {
                if (!isKeyboardEnabled()) {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    Toast.makeText(this, "Grant ADB permission for auto-enable", Toast.LENGTH_LONG).show()
                } else {
                    showImePicker()
                }
            }
        }
    }

    private fun showImePicker() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    private fun hasSecureSettingsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        
        val isOldServiceRunning = isAccessibilityServiceEnabled(RemoteControlService::class.java)
        if (isOldServiceRunning) {
            tvStatus.text = "CRITICAL: OLD SERVICE ACTIVE. DISABLE IN SETTINGS."
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        } else {
            tvStatus.text = if (isStreaming) "Streaming Live..." else "Not Streaming"
            tvStatus.setTextColor(getColor(android.R.color.black))
        }

        if (MyAccessibilityService.instance != null) {
            btnAccessibility.text = "Accessibility: ON"
            btnAccessibility.setBackgroundColor(getColor(android.R.color.holo_green_light))
        } else {
            btnAccessibility.text = "Enable Accessibility"
            btnAccessibility.setBackgroundColor(getColor(android.R.color.holo_red_light))
        }

        if (isKeyboardSelected()) {
            btnKeyboard.text = "Keyboard: SELECTED"
            btnKeyboard.setBackgroundColor(getColor(android.R.color.holo_green_light))
        } else if (isKeyboardEnabled()) {
            btnKeyboard.text = "Keyboard: ENABLED (Tap to Select)"
            btnKeyboard.setBackgroundColor(getColor(android.R.color.holo_orange_light))
        } else {
            btnKeyboard.text = "Enable Keyboard"
            btnKeyboard.setBackgroundColor(getColor(android.R.color.holo_red_light))
        }
    }

    private fun isKeyboardEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun isKeyboardSelected(): Boolean {
        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return currentIme?.contains(packageName) == true
    }

    private fun isAccessibilityServiceEnabled(service: Class<*>): Boolean {
        val expectedComponentName = ComponentName(this, service)
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedComponentName.flattenToString()) == true
    }
}
