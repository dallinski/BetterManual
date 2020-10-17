package com.obsidium.bettermanual

import android.util.Pair

object CameraUtil {
    /* These have been determined experimentally, values actually do not need to be exact... */
    val MIN_SHUTTER_VALUES = intArrayOf(
            1, 276, 347, 437, 551, 693, 873, 1101, 1385, 1743, 2202, 2771, 3486, 4404, 5540, 6972, 8808,
            11081, 13945, 17617, 22162, 27888, 35232, 44321, 55777, 70465, 93451, 116628, 145553, 181652,
            226703, 282927, 354560, 446220, 563719, 709135, 892421, 1127429, 1418242, 1784846,
            2770094, 3462657, 4328353, 5410477, 6763136, 845956, 10567481, 13209387, 16511770,
            20639744, 25799712, 32249672
    )
    @JvmField
    val SHUTTER_SPEEDS = arrayOf(
            intArrayOf(1, 4000), intArrayOf(1, 3200), intArrayOf(1, 2500), intArrayOf(1, 2000),
            intArrayOf(1, 1600), intArrayOf(1, 1250), intArrayOf(1, 1000), intArrayOf(1, 800),
            intArrayOf(1, 640), intArrayOf(1, 500), intArrayOf(1, 400), intArrayOf(1, 320),
            intArrayOf(1, 250), intArrayOf(1, 200), intArrayOf(1, 160), intArrayOf(1, 125),
            intArrayOf(1, 100), intArrayOf(1, 80), intArrayOf(1, 60), intArrayOf(1, 50),
            intArrayOf(1, 40), intArrayOf(1, 30), intArrayOf(1, 25), intArrayOf(1, 20),
            intArrayOf(1, 15), intArrayOf(1, 13), intArrayOf(1, 10), intArrayOf(1, 8),
            intArrayOf(1, 6), intArrayOf(1, 5), intArrayOf(1, 4), intArrayOf(1, 3),
            intArrayOf(10, 25), intArrayOf(1, 2), intArrayOf(10, 16), intArrayOf(4, 5),
            intArrayOf(1, 1), intArrayOf(13, 10), intArrayOf(16, 10), intArrayOf(2, 1),
            intArrayOf(25, 10), intArrayOf(16, 5), intArrayOf(4, 1), intArrayOf(5, 1),
            intArrayOf(6, 1), intArrayOf(8, 1), intArrayOf(10, 1), intArrayOf(13, 1),
            intArrayOf(15, 1), intArrayOf(20, 1), intArrayOf(25, 1), intArrayOf(30, 1))

    @JvmStatic
    fun getShutterValueIndex(speed: Pair<Int, Int>): Int {
        return getShutterValueIndex(speed.first, speed.second)
    }

    fun getShutterValueIndex(n: Int, d: Int): Int {
        for (i in SHUTTER_SPEEDS.indices) {
            if (SHUTTER_SPEEDS[i][0] == n &&
                    SHUTTER_SPEEDS[i][1] == d) {
                return i
            }
        }
        return -1
    }

    @JvmStatic
    fun formatShutterSpeed(n: Int, d: Int): String {
        return if (n == 1 && d != 2 && d != 1) String.format("%d/%d", n, d) else if (d == 1) {
            if (n == 65535) "BULB" else String.format("%d\"", n)
        } else String.format("%.1f\"", n.toFloat() / d.toFloat())
    }
}