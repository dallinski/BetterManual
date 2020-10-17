package com.github.ma1co.pmcademo.app

import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import com.github.ma1co.openmemories.framework.DeviceInfo
import com.github.ma1co.pmcademo.app.AppNotificationManager.Companion.instance
import com.sony.scalar.hardware.avio.DisplayManager
import com.sony.scalar.sysutil.ScalarInput
import com.sony.scalar.sysutil.didep.Gpelibrary
import com.sony.scalar.sysutil.didep.Gpelibrary.GS_FRAMEBUFFER_TYPE

open class BaseActivity : Activity() {
    var displayManager: DisplayManager? = null
        private set

    override fun onResume() {
        Logger.info("Resume " + componentName.className)
        super.onResume()
        setColorDepth(true)
        notifyAppInfo()
        displayManager = DisplayManager()
        displayManager!!.setDisplayStatusListener { event -> if (event == DisplayManager.EVENT_SWITCH_DEVICE) onDisplayChanged(displayManager!!.activeDevice) }
    }

    override fun onPause() {
        Logger.info("Pause " + componentName.className)
        super.onPause()
        setColorDepth(false)
        displayManager!!.releaseDisplayStatusListener()
        displayManager!!.finish()
        displayManager = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (event.scanCode) {
            ScalarInput.ISV_KEY_UP -> onUpKeyDown()
            ScalarInput.ISV_KEY_DOWN -> onDownKeyDown()
            ScalarInput.ISV_KEY_LEFT -> onLeftKeyDown()
            ScalarInput.ISV_KEY_RIGHT -> onRightKeyDown()
            ScalarInput.ISV_KEY_ENTER -> onEnterKeyDown()
            ScalarInput.ISV_KEY_FN -> onFnKeyDown()
            ScalarInput.ISV_KEY_AEL -> onAelKeyDown()
            ScalarInput.ISV_KEY_MENU, ScalarInput.ISV_KEY_SK1 -> onMenuKeyDown()
            ScalarInput.ISV_KEY_S1_1 -> onFocusKeyDown()
            ScalarInput.ISV_KEY_S1_2 -> true
            ScalarInput.ISV_KEY_S2 -> onShutterKeyDown()
            ScalarInput.ISV_KEY_PLAY -> onPlayKeyDown()
            ScalarInput.ISV_KEY_STASTOP -> onMovieKeyDown()
            ScalarInput.ISV_KEY_CUSTOM1 -> onC1KeyDown()
            ScalarInput.ISV_KEY_DELETE, ScalarInput.ISV_KEY_SK2 -> onDeleteKeyDown()
            ScalarInput.ISV_KEY_LENS_ATTACH -> onLensAttached()
            ScalarInput.ISV_DIAL_1_CLOCKWISE, ScalarInput.ISV_DIAL_1_COUNTERCW -> onUpperDialChanged(getDialStatus(ScalarInput.ISV_DIAL_1_STATUS) / 22)
            ScalarInput.ISV_DIAL_2_CLOCKWISE, ScalarInput.ISV_DIAL_2_COUNTERCW -> onLowerDialChanged(getDialStatus(ScalarInput.ISV_DIAL_2_STATUS) / 22)
            ScalarInput.ISV_KEY_MODE_DIAL -> onModeDialChanged(getDialStatus(ScalarInput.ISV_KEY_MODE_DIAL))
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (event.scanCode) {
            ScalarInput.ISV_KEY_UP -> onUpKeyUp()
            ScalarInput.ISV_KEY_DOWN -> onDownKeyUp()
            ScalarInput.ISV_KEY_LEFT -> onLeftKeyUp()
            ScalarInput.ISV_KEY_RIGHT -> onRightKeyUp()
            ScalarInput.ISV_KEY_ENTER -> onEnterKeyUp()
            ScalarInput.ISV_KEY_FN -> onFnKeyUp()
            ScalarInput.ISV_KEY_AEL -> onAelKeyUp()
            ScalarInput.ISV_KEY_MENU, ScalarInput.ISV_KEY_SK1 -> onMenuKeyUp()
            ScalarInput.ISV_KEY_S1_1 -> onFocusKeyUp()
            ScalarInput.ISV_KEY_S1_2 -> true
            ScalarInput.ISV_KEY_S2 -> onShutterKeyUp()
            ScalarInput.ISV_KEY_PLAY -> onPlayKeyUp()
            ScalarInput.ISV_KEY_STASTOP -> onMovieKeyUp()
            ScalarInput.ISV_KEY_CUSTOM1 -> onC1KeyUp()
            ScalarInput.ISV_KEY_DELETE, ScalarInput.ISV_KEY_SK2 -> onDeleteKeyUp()
            ScalarInput.ISV_KEY_LENS_ATTACH -> onLensDetached()
            ScalarInput.ISV_DIAL_1_CLOCKWISE, ScalarInput.ISV_DIAL_1_COUNTERCW -> true
            ScalarInput.ISV_DIAL_2_CLOCKWISE, ScalarInput.ISV_DIAL_2_COUNTERCW -> true
            ScalarInput.ISV_KEY_MODE_DIAL -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }

    protected fun getDialStatus(key: Int): Int {
        return ScalarInput.getKeyStatus(key).status
    }

    protected open fun onUpKeyDown(): Boolean {
        return false
    }

    protected open fun onUpKeyUp(): Boolean {
        return false
    }

    protected open fun onDownKeyDown(): Boolean {
        return false
    }

    protected open fun onDownKeyUp(): Boolean {
        return false
    }

    protected open fun onLeftKeyDown(): Boolean {
        return false
    }

    protected open fun onLeftKeyUp(): Boolean {
        return false
    }

    protected open fun onRightKeyDown(): Boolean {
        return false
    }

    protected open fun onRightKeyUp(): Boolean {
        return false
    }

    protected open fun onEnterKeyDown(): Boolean {
        return false
    }

    protected open fun onEnterKeyUp(): Boolean {
        return false
    }

    protected fun onFnKeyDown(): Boolean {
        return false
    }

    protected fun onFnKeyUp(): Boolean {
        return false
    }

    protected fun onAelKeyDown(): Boolean {
        return false
    }

    protected fun onAelKeyUp(): Boolean {
        return false
    }

    protected fun onMenuKeyDown(): Boolean {
        return false
    }

    protected fun onMenuKeyUp(): Boolean {
        return false
    }

    protected fun onFocusKeyDown(): Boolean {
        return false
    }

    protected fun onFocusKeyUp(): Boolean {
        return false
    }

    protected open fun onShutterKeyDown(): Boolean {
        return false
    }

    protected open fun onShutterKeyUp(): Boolean {
        return false
    }

    protected fun onPlayKeyDown(): Boolean {
        return false
    }

    protected fun onPlayKeyUp(): Boolean {
        return false
    }

    protected fun onMovieKeyDown(): Boolean {
        return false
    }

    protected fun onMovieKeyUp(): Boolean {
        return false
    }

    protected fun onC1KeyDown(): Boolean {
        return false
    }

    protected fun onC1KeyUp(): Boolean {
        return false
    }

    protected fun onLensAttached(): Boolean {
        return false
    }

    protected fun onLensDetached(): Boolean {
        return false
    }

    protected open fun onUpperDialChanged(value: Int): Boolean {
        return false
    }

    protected fun onLowerDialChanged(value: Int): Boolean {
        return false
    }

    protected fun onModeDialChanged(value: Int): Boolean {
        return false
    }

    protected fun onDeleteKeyDown(): Boolean {
        return true
    }

    protected open fun onDeleteKeyUp(): Boolean {
        onBackPressed()
        return true
    }

    fun onDisplayChanged(device: String?) {
        instance.notify(NOTIFICATION_DISPLAY_CHANGED)
    }

    protected fun setAutoPowerOffMode(enable: Boolean) {
        val mode = if (enable) "APO/NORMAL" else "APO/NO" // or "APO/SPECIAL" ?
        val intent = Intent()
        intent.action = "com.android.server.DAConnectionManagerService.apo"
        intent.putExtra("apo_info", mode)
        sendBroadcast(intent)
    }

    protected open fun setColorDepth(highQuality: Boolean) {
        val type = if (highQuality) GS_FRAMEBUFFER_TYPE.ABGR8888 else GS_FRAMEBUFFER_TYPE.RGBA4444
        Gpelibrary.changeFrameBufferPixel(type)
    }

    protected fun notifyAppInfo() {
        val intent = Intent("com.android.server.DAConnectionManagerService.AppInfoReceive")
        intent.putExtra("package_name", componentName.packageName)
        intent.putExtra("class_name", componentName.className)
        //intent.putExtra("pkey", new String[] {});// either this or these two:
        //intent.putExtra("pullingback_key", new String[] {});
        // Exit app when plugging camera into USB
        intent.putExtra("pullingback_key", arrayOf("KEY_USB_CONNECT"))
        // Automatically resume app after power off etc.
        intent.putExtra("resume_key", arrayOf(KEY_POWER_SLIDE_PON, KEY_RELEASE_APO, KEY_PLAY_APO,
                KEY_MEDIA_INOUT_APO, KEY_LENS_APO, KEY_ACCESSORY_APO, KEY_DEDICATED_APO, KEY_POWER_APO, KEY_PLAY_PON))
        sendBroadcast(intent)
    }

    val deviceInfo: DeviceInfo
        get() = DeviceInfo.getInstance()

    companion object {
        const val NOTIFICATION_DISPLAY_CHANGED = "NOTIFICATION_DISPLAY_CHANGED"
        const val KEY_ACCESSORY_APO = "KEY_ACCESSORY_APO"
        const val KEY_DEDICATED_APO = "KEY_DEDICATED_APO"
        const val KEY_LENS_APO = "KEY_LENS_APO"
        const val KEY_MEDIA_INOUT_APO = "KEY_MEDIA_INOUT_APO"
        const val KEY_PLAY_APO = "KEY_PLAY_APO"
        const val KEY_PLAY_PON = "KEY_PLAY_PON"
        const val KEY_POWER_APO = "KEY_POWER_APO"
        const val KEY_POWER_SLIDE_PON = "KEY_POWER_SLIDE_PON"
        const val KEY_RELEASE_APO = "KEY_RELEASE_APO"
    }
}