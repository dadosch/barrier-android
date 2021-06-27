package com.barrier.android

import android.accessibilityservice.AccessibilityService
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class KeyboardMouseService : AccessibilityService() {
    private var cursorView: View? = null
    private var cursorLayout: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null
    private lateinit var maniLoopHandler: Handler
    private val displayHeight = Resources.getSystem().displayMetrics.heightPixels
    private val displayWidth = Resources.getSystem().displayMetrics.widthPixels
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    // display metrics with this method can be ambiguous, as Resources#heightPixels/widthPixels doesn't supports rotation, etc.

    override fun onServiceConnected() {
        super.onServiceConnected()
        log("onServiceConnected: Accessibility service is connected...")
        maniLoopHandler = Handler(mainLooper)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        cursorLayout = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = this.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
        }
        cursorView = LayoutInflater.from(this).inflate(R.layout.layout_cursor, null)
        windowManager?.addView(cursorView, cursorLayout)
        /*sendNotificationToStartAccessibility()*/
        randomlyMoveCursor()
    }

    private var cursorXpos = 0
    private var cursorYpos = 0
    private fun randomlyMoveCursor() {
        serviceScope.launch {
            while (isActive) {
                delay(5)
                moveCursor(cursorXpos++, cursorYpos++)
                if (cursorXpos >= displayWidth) {
                    cursorXpos = 0
                    cursorYpos = 0
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        log("onAccessibilityEvent, Event: $event")
    }

    override fun onInterrupt() {
        log("onInterrupt, accessibility service interrupted...")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy, accessibility service destroyed...")
        releaseResources()
    }

    private fun releaseResources() {
        serviceScope.cancel("Service destroyed")
        windowManager?.removeViewImmediate(cursorView)
    }

    private fun moveCursor(x: Int, y: Int) {
        maniLoopHandler.post {  // posting on UI thread, because background thread cannot update UI
            cursorLayout?.let {
                it.x = x
                it.y = y
                windowManager?.updateViewLayout(cursorView, it)
            }
            log("onCursorMove, cursor moved. x: $x, y: $y")
        }
    }

    companion object {
        fun Any.log(msg: String) {
            Log.e(this.javaClass.simpleName, msg)
        }
    }
}