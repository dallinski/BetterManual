package com.obsidium.bettermanual

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.sony.scalar.hardware.CameraEx

class Preferences(context: Context?) {
    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    var sceneMode: String?
        get() = mPrefs.getString(KEY_SCENE_MODE, CameraEx.ParametersModifier.SCENE_MODE_MANUAL_EXPOSURE)
        set(mode) {
            mPrefs.edit().putString(KEY_SCENE_MODE, mode).apply()
        }
    var driveMode: String?
        get() = mPrefs.getString(KEY_DRIVE_MODE, CameraEx.ParametersModifier.DRIVE_MODE_BURST)
        set(mode) {
            mPrefs.edit().putString(KEY_DRIVE_MODE, mode).apply()
        }
    var burstDriveSpeed: String?
        get() = mPrefs.getString(KEY_BURST_DRIVE_SPEED, CameraEx.ParametersModifier.BURST_DRIVE_SPEED_HIGH)
        set(speed) {
            mPrefs.edit().putString(KEY_BURST_DRIVE_SPEED, speed).apply()
        }
    var minShutterSpeed: Int
        get() = mPrefs.getInt(KEY_MIN_SHUTTER_SPEED, -1)
        set(speed) {
            mPrefs.edit().putInt(KEY_MIN_SHUTTER_SPEED, speed).apply()
        }

    fun getViewFlags(defaultValue: Int): Int {
        return mPrefs.getInt(KEY_VIEW_FLAGS, defaultValue)
    }

    fun setViewFlags(flags: Int) {
        mPrefs.edit().putInt(KEY_VIEW_FLAGS, flags).apply()
    }

    companion object {
        private const val KEY_SCENE_MODE = "sceneMode"
        private const val KEY_DRIVE_MODE = "driveMode"
        private const val KEY_BURST_DRIVE_SPEED = "burstDriveSpeed"
        private const val KEY_MIN_SHUTTER_SPEED = "minShutterSpeed"
        private const val KEY_VIEW_FLAGS = "viewFlags"
    }

}