package com.example.myapplication

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import okhttp3.*
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var serviceHandler: Handler? = null

    private var lastFullText: String = ""

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread("CaptureThread")
        thread.start()
        serviceHandler = Handler(thread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("DATA")
        if (data != null) {
            startForegroundService()
            startProjection(resultCode, data)
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "remote_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Remote", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Remote Control").setSmallIcon(android.R.drawable.ic_menu_camera).build()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val metrics = resources.displayMetrics
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)

        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay("Remote", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val bitmap = Bitmap.createBitmap(image.width + plane.rowStride / plane.pixelStride - image.width, image.height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, out)
                webSocket?.send(out.toByteArray().toByteString())
                bitmap.recycle()
            } catch (e: Exception) { Log.e("Screen", "Img Error: ${e.message}") }
            finally { image.close() }
        }, serviceHandler)

        connectWebSocket()
    }

    private fun connectWebSocket() {
        val request = Request.Builder().url("wss://5p-production.up.railway.app").build()
        lastFullText = ""
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when(json.optString("type")) {
                        "touch" -> {
                            val metrics = resources.displayMetrics
                            val x = (json.optDouble("x") * metrics.widthPixels).toInt()
                            val y = (json.optDouble("y") * metrics.heightPixels).toInt()
                            MyAccessibilityService.instance?.dispatchGestureClick(x, y)
                        }
                        "text" -> {
                            val txt = json.optString("value")
                            syncInput(txt)
                        }
                        "key" -> {
                            val key = json.optString("value")
                            when (key) {
                                "Enter" -> RemoteKeyboardService.sendKey(KeyEvent.KEYCODE_ENTER)
                                "Backspace" -> RemoteKeyboardService.sendKey(KeyEvent.KEYCODE_DEL)
                                "Delete" -> RemoteKeyboardService.sendKey(KeyEvent.KEYCODE_FORWARD_DEL)
                                " " -> RemoteKeyboardService.sendKey(KeyEvent.KEYCODE_SPACE)
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("WS", "Err: ${e.message}") }
            }
        })
    }

    private fun syncInput(newValue: String) {
        if (newValue == lastFullText) return
        
        if (newValue.isEmpty()) {
            RemoteKeyboardService.replaceText("")
            lastFullText = ""
            return
        }

        if (newValue.startsWith(lastFullText)) {
            val diff = newValue.substring(lastFullText.length)
            if (diff.isNotEmpty()) {
                RemoteKeyboardService.sendText(diff)
            }
        } else if (lastFullText.startsWith(newValue)) {
            val count = lastFullText.length - newValue.length
            for (i in 0 until count) {
                RemoteKeyboardService.sendKey(KeyEvent.KEYCODE_DEL)
            }
        } else {
            // Robust fallback for one-character delta sync (common in browsers)
            if (newValue.length == 1 && lastFullText.isNotEmpty()) {
                RemoteKeyboardService.sendText(newValue)
                lastFullText += newValue
                return
            }
            RemoteKeyboardService.replaceText(newValue)
        }

        lastFullText = newValue
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        mediaProjection?.stop()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
