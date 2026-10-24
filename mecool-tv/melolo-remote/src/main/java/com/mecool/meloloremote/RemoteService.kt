package com.mecool.meloloremote

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Lets the TV remote drive Melolo while it is in front. Other apps get their keys untouched.
 *
 * - Player screen: Up/Down go to the previous/next episode, Menu opens the episode list,
 *   OK shows the pointer.
 * - Other Melolo screens: the D-pad moves an on-screen pointer and OK clicks what is under it.
 *   Holding the pointer against an edge scrolls.
 * - Holding OK swaps between the two modes on the current screen.
 *
 * The Mecool reports no touchscreen, so Android rejects injected touch gestures.
 * Instead we press views directly with accessibility actions (click / scroll).
 * That also means the video itself can't be paused: Melolo only pauses on a real touch.
 *
 * Some remotes send no key repeats while a button is held, so long presses and
 * pointer gliding are timed here instead of relying on KeyEvent.repeatCount.
 */
class RemoteService : AccessibilityService() {

    private lateinit var cursor: CursorOverlay
    private var inMelolo = false
    private var inPlayer = false
    /** Long-press OK flips the default mode until the next screen change. */
    private var modeFlipped = false
    private var okLongPressed = false

    private val handler = Handler(Looper.getMainLooper())
    private val okLongPress = Runnable {
        okLongPressed = true
        modeFlipped = !modeFlipped
        updateCursor()
    }

    /** Direction of the arrow key currently held in pointer mode, (0, 0) when none. */
    private var heldDx = 0
    private var heldDy = 0
    private var heldTicks = 0
    private val glide = object : Runnable {
        override fun run() {
            heldTicks++
            movePointer()
            handler.postDelayed(this, GLIDE_INTERVAL_MS)
        }
    }

    private val pointerMode get() = inMelolo && (inPlayer == modeFlipped)

    override fun onServiceConnected() {
        cursor = CursorOverlay(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in IGNORED_PACKAGES) return

        val wasPlayer = inPlayer
        inMelolo = pkg == MELOLO_PACKAGE
        if (inMelolo) {
            val cls = event.className?.toString().orEmpty()
            // Only activities and dialogs tell us which screen we are on;
            // other window events (toasts, popups) keep the current mode.
            if (cls.endsWith("Activity")) inPlayer = cls.endsWith(PLAYER_ACTIVITY)
            else if (cls.contains("Dialog")) inPlayer = false
        } else {
            inPlayer = false
        }
        if (inPlayer != wasPlayer || !inMelolo) modeFlipped = false
        updateCursor()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!inMelolo) return false
        val down = event.action == KeyEvent.ACTION_DOWN

        if (event.keyCode in OK_KEYS) {
            if (down) {
                if (event.repeatCount == 0) {
                    okLongPressed = false
                    handler.postDelayed(okLongPress, LONG_PRESS_MS)
                }
            } else {
                handler.removeCallbacks(okLongPress)
                if (!okLongPressed) {
                    if (pointerMode) {
                        clickAt(cursor.x.toInt(), cursor.y.toInt())
                    } else {
                        // Nothing in the player reacts to a click, so bring up the pointer instead.
                        modeFlipped = !modeFlipped
                        updateCursor()
                    }
                }
                okLongPressed = false
            }
            return true
        }
        return if (pointerMode) handlePointerKey(event, down) else handlePlayerKey(event, down)
    }

    private fun handlePlayerKey(event: KeyEvent, down: Boolean): Boolean {
        val firstPress = down && event.repeatCount == 0
        val cx = width / 2
        val cy = height / 2
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> if (firstPress) scrollAt(cx, cy, forward = false)
            KeyEvent.KEYCODE_DPAD_DOWN -> if (firstPress) scrollAt(cx, cy, forward = true)
            KeyEvent.KEYCODE_MENU -> if (firstPress) clickAt(cx, (height * EPISODE_LIST_Y).toInt())
            else -> return false
        }
        return true
    }

    private fun handlePointerKey(event: KeyEvent, down: Boolean): Boolean {
        val (dx, dy) = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> 0 to -1
            KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
            KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0
            KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
            else -> return false
        }
        if (down) {
            if (event.repeatCount == 0) {
                stopGlide()
                heldDx = dx
                heldDy = dy
                movePointer()
                handler.postDelayed(glide, GLIDE_DELAY_MS)
            }
        } else if (dx == heldDx && dy == heldDy) {
            stopGlide()
        }
        return true
    }

    /** One pointer step in the held direction; speeds up the longer the key is held. */
    private fun movePointer() {
        val step = minOf(STEP_PX + heldTicks * GLIDE_ACCEL_PX, MAX_STEP_PX)
        val atEdge = !cursor.moveBy(heldDx * step, heldDy * step)
        if (atEdge && heldTicks % EDGE_SCROLL_EVERY == 0) {
            scrollAt(cursor.x.toInt(), cursor.y.toInt(), forward = heldDx > 0 || heldDy > 0, horizontal = heldDx != 0)
        }
    }

    private fun stopGlide() {
        handler.removeCallbacks(glide)
        heldDx = 0
        heldDy = 0
        heldTicks = 0
    }

    private fun updateCursor() {
        if (!pointerMode) stopGlide()
        cursor.setVisible(pointerMode)
    }

    private val width get() = cursor.screenWidth
    private val height get() = cursor.screenHeight

    /** Clicks the innermost clickable view under (x, y). */
    private fun clickAt(x: Int, y: Int) {
        val target = deepestAt(x, y) { it.isClickable }
        val ok = target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        Log.d(TAG, "click ($x,$y) -> ${describe(target)} ok=$ok")
    }

    /**
     * Scrolls the innermost scrollable view under (x, y); falls back to any scrollable
     * view on screen. For the player's episode pager, forward = next episode.
     */
    private fun scrollAt(x: Int, y: Int, forward: Boolean, horizontal: Boolean = false) {
        val root = rootInActiveWindow ?: return
        val target = deepestAt(x, y) { it.isScrollable && looksHorizontal(it) == horizontal }
            ?: deepestAt(x, y) { it.isScrollable }
            ?: findFirst(root) { it.isScrollable }
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val ok = target?.performAction(action) ?: false
        Log.d(TAG, "scroll forward=$forward ($x,$y) -> ${describe(target)} ok=$ok")
    }

    private fun looksHorizontal(node: AccessibilityNodeInfo): Boolean {
        val r = Rect().also { node.getBoundsInScreen(it) }
        return r.width() > r.height() * 2
    }

    /**
     * Innermost matching node containing (x, y). Overlapping siblings are all searched
     * (pagers stack their pages); among equally deep matches the last-drawn one wins.
     */
    private fun deepestAt(x: Int, y: Int, match: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestDepth = -1
        walkAt(x, y) { node, depth ->
            if (depth >= bestDepth && match(node)) {
                best = node
                bestDepth = depth
            }
        }
        return best
    }

    /** Visits every visible node containing (x, y), parents before children. */
    private fun walkAt(x: Int, y: Int, visit: (AccessibilityNodeInfo, Int) -> Unit) {
        val r = Rect()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            visit(node, depth)
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                child.getBoundsInScreen(r)
                if (child.isVisibleToUser && r.contains(x, y)) walk(child, depth + 1)
            }
        }
        rootInActiveWindow?.let { walk(it, 0) }
    }

    private fun findFirst(node: AccessibilityNodeInfo, match: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (match(node)) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findFirst(it, match) }
            if (found != null) return found
        }
        return null
    }

    private fun describe(node: AccessibilityNodeInfo?): String {
        if (node == null) return "none"
        val r = Rect().also { node.getBoundsInScreen(it) }
        return "${node.className?.toString()?.substringAfterLast('.')} ${node.viewIdResourceName?.substringAfterLast('/')} $r"
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::cursor.isInitialized) cursor.setVisible(false)
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MeloloRemote"
        const val MELOLO_PACKAGE = "com.worldance.drama"
        const val PLAYER_ACTIVITY = "ShortSeriesActivity"
        val IGNORED_PACKAGES = setOf("com.android.systemui", "android")
        val OK_KEYS = setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
        /** "All episodes" bar near the bottom of the player (y 968..1048 of 1080). */
        const val EPISODE_LIST_Y = 0.933f
        const val LONG_PRESS_MS = 700L
        const val STEP_PX = 30
        const val MAX_STEP_PX = 60
        const val GLIDE_DELAY_MS = 300L
        const val GLIDE_INTERVAL_MS = 40L
        const val GLIDE_ACCEL_PX = 4
        /** Ticks between edge scrolls while held (~400 ms). */
        const val EDGE_SCROLL_EVERY = 10
    }
}
