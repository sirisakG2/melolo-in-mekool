package com.mecool.melolotile

import android.util.Log
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File

/**
 * Opens an app in a freeform window covering the whole screen, using the box's own adbd
 * on localhost:5555 (the same commands as ../landscape.sh).
 *
 * The first connection makes the TV ask "Allow debugging?"; the key is kept in [keyDir]
 * so later connections are accepted silently.
 */
class FreeformLauncher(
    private val keyDir: File,
    private val packageName: String,
    private val activityName: String,
) {
    /**
     * Returns true if the app is now in front in a full-screen freeform window, or false if
     * such a window already existed and the caller should simply start the app to bring it back.
     */
    fun open(width: Int, height: Int): Boolean = connect().use { adb ->
        fun sh(cmd: String): String {
            val response = adb.shell(cmd)
            Log.d(TAG, "$ ${cmd} -> ${response.exitCode}")
            return response.allOutput
        }

        when (taskMode(sh("am stack list"))) {
            TaskMode.FREEFORM -> return false
            // A fullscreen task would be reused as-is, so close it first.
            TaskMode.FULLSCREEN -> sh("am force-stop $packageName")
            TaskMode.NONE -> Unit
        }
        sh("am start --windowingMode $WINDOWING_MODE_FREEFORM -n $packageName/$activityName")

        repeat(20) {
            val taskId = taskId(sh("am stack list"))
            if (taskId != null) {
                sh("am task resize $taskId 0 0 $width $height")
                return true
            }
            Thread.sleep(250)
        }
        error("app did not start")
    }

    private fun connect(): Dadb {
        val private = File(keyDir, "adbkey")
        val public = File(keyDir, "adbkey.pub")
        if (!private.exists() || !public.exists()) AdbKeyPair.generate(private, public)
        return Dadb.create("localhost", ADB_PORT, AdbKeyPair.read(private, public))
    }

    private enum class TaskMode { NONE, FULLSCREEN, FREEFORM }

    /**
     * `am stack list` prints each root task's configuration line (with its windowing mode)
     * followed by its tasks, e.g. `taskId=9909: com.worldance.drama/...`.
     */
    private fun taskMode(stackList: String): TaskMode {
        var freeform = false
        for (line in stackList.lines()) {
            if ("mWindowingMode=" in line) freeform = "mWindowingMode=freeform" in line
            if (isOurTask(line)) return if (freeform) TaskMode.FREEFORM else TaskMode.FULLSCREEN
        }
        return TaskMode.NONE
    }

    private fun taskId(stackList: String): String? =
        stackList.lines().firstOrNull(::isOurTask)
            ?.let { Regex("taskId=(\\d+)").find(it)?.groupValues?.get(1) }

    private fun isOurTask(line: String) = line.trimStart().startsWith("taskId=") && ": $packageName/" in line

    private companion object {
        const val TAG = "MeloloTile"
        const val ADB_PORT = 5555
        /** WindowConfiguration.WINDOWING_MODE_FREEFORM */
        const val WINDOWING_MODE_FREEFORM = 5
    }
}
