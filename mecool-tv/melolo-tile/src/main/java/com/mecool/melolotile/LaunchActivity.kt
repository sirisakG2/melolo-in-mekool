package com.mecool.melolotile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * Google TV home tile that opens the installed Melolo app full screen in landscape.
 *
 * Melolo is locked to portrait, so in a normal (fullscreen) task Android shows it as a narrow
 * strip. A freeform window covering the whole TV avoids that. Apps can't ask for freeform on
 * this box, so [FreeformLauncher] does it through the box's own adb. If that fails, Melolo
 * opens normally.
 */
class LaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = packageManager.getLaunchIntentForPackage(MELOLO_PACKAGE)
        if (intent == null) {
            Toast.makeText(this, R.string.not_installed, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val bounds = windowManager.maximumWindowMetrics.bounds
        val launcher = FreeformLauncher(filesDir, MELOLO_PACKAGE, intent.component!!.className)
        Thread {
            val result = runCatching { launcher.open(bounds.width(), bounds.height()) }
            runOnUiThread {
                result.onSuccess { opened ->
                    if (!opened) startActivity(intent)
                }.onFailure { e ->
                    Log.w(TAG, "freeform launch failed", e)
                    Toast.makeText(this, getString(R.string.landscape_failed, e.message ?: e.javaClass.simpleName), Toast.LENGTH_LONG).show()
                    startActivity(intent)
                }
                finish()
            }
        }.start()
    }

    private companion object {
        const val TAG = "MeloloTile"
        const val MELOLO_PACKAGE = "com.worldance.drama"
    }
}
