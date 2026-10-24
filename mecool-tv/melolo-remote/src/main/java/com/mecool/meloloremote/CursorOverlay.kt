package com.mecool.meloloremote

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/** A pointer drawn above Melolo. It never takes touches, so taps land on the app below it. */
class CursorOverlay(service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val sizePx = (28 * service.resources.displayMetrics.density).toInt()
    private val view = PointerView(service)
    private var shown = false

    val screenWidth: Int
    val screenHeight: Int

    /** Pointer centre in screen pixels. */
    var x: Float
        private set
    var y: Float
        private set

    private val params = WindowManager.LayoutParams(
        sizePx, sizePx,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            val size = Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(size)
            screenWidth = size.x
            screenHeight = size.y
        }
        x = screenWidth / 2f
        y = screenHeight / 2f
    }

    fun setVisible(visible: Boolean) {
        if (visible == shown) return
        shown = visible
        if (visible) {
            place()
            windowManager.addView(view, params)
        } else {
            windowManager.removeView(view)
        }
    }

    /** Moves the pointer, clamped to the screen. Returns false if it was already at that edge. */
    fun moveBy(dx: Int, dy: Int): Boolean {
        val newX = (x + dx).coerceIn(0f, screenWidth - 1f)
        val newY = (y + dy).coerceIn(0f, screenHeight - 1f)
        if (newX == x && newY == y) return false
        x = newX
        y = newY
        if (shown) {
            place()
            windowManager.updateViewLayout(view, params)
        }
        return true
    }

    private fun place() {
        params.x = x.toInt() - sizePx / 2
        params.y = y.toInt() - sizePx / 2
    }

    private class PointerView(context: Context) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFD400.toInt() }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        override fun onDraw(canvas: Canvas) {
            val r = width / 2f - 2f
            canvas.drawCircle(width / 2f, height / 2f, r, fill)
            canvas.drawCircle(width / 2f, height / 2f, r, ring)
        }
    }
}
