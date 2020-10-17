package com.obsidium.bettermanual

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Pair
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import com.github.ma1co.pmcademo.app.BaseActivity
import com.obsidium.bettermanual.CameraUtil.formatShutterSpeed
import com.obsidium.bettermanual.CameraUtil.getShutterValueIndex
import com.sony.scalar.hardware.CameraEx
import com.sony.scalar.hardware.CameraEx.*
import com.sony.scalar.sysutil.ScalarInput
import com.sony.scalar.sysutil.didep.Settings
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ManualActivity : BaseActivity(), SurfaceHolder.Callback, View.OnClickListener, ShutterListener, ShutterSpeedChangeListener {

    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: CameraEx? = null
    private var mAutoReviewControl: AutoPictureReviewControl? = null
    private var mPictureReviewTime = 0
    private var mPrefs: Preferences? = null
    private var mTvShutter: TextView? = null
    private var mTvAperture: TextView? = null
    private var mTvISO: TextView? = null
    private var mTvExposureCompensation: TextView? = null
    private var mLExposure: LinearLayout? = null
    private var mTvExposure: TextView? = null
    private var mTvLog: TextView? = null
    private var mTvMagnification: TextView? = null
    private var mTvMsg: TextView? = null
    private var mVHist: HistogramView? = null
    private var mLInfoBottom: TableLayout? = null
    private var mIvDriveMode: ImageView? = null
    private var mIvMode: ImageView? = null
    private var mIvTimelapse: ImageView? = null
    private var mIvBracket: ImageView? = null
    private var mVGrid: GridView? = null
    private var mTvHint: TextView? = null
    private var mFocusScaleView: FocusScaleView? = null
    private var mLFocusScale: View? = null

    // Bracketing
    private var mBracketStep = 0 // in 1/3 stops
    private var mBracketMaxPicCount = 0
    private var mBracketPicCount = 0
    private var mBracketShutterDelta = 0
    private var mBracketActive = false
    private var mBracketNextShutterSpeed: Pair<Int, Int>? = null
    private var mBracketNeutralShutterIndex = 0

    // Timelapse
    private var mAutoPowerOffTimeBackup = 0
    private var mTimelapseActive = false
    private var mTimelapseInterval = 0 // ms
    private var mTimelapsePicCount = 0
    private var mTimelapsePicsTaken = 0
    private var mCountdown = 0
    private val mTimelapseRunnable = Runnable { mCamera!!.burstableTakePicture() }
    private val mCountDownRunnable: Runnable = object : Runnable {
        override fun run() {
            if (--mCountdown > 0) {
                mTvMsg!!.text = String.format("Starting in %d...", mCountdown)
                mHandler.postDelayed(this, 1000)
            } else {
                mTvMsg!!.visibility = View.GONE
                if (mTimelapseActive) startShootingTimelapse() else if (mBracketActive) startShootingBracket()
            }
        }
    }
    private val mHideFocusScaleRunnable = Runnable { mLFocusScale!!.visibility = View.GONE }

    // ISO
    private var mCurIso = 0
    private var mSupportedIsos: List<Int>? = null

    // Shutter speed
    private var mNotifyOnNextShutterSpeedChange = false

    // Aperture
    private var mNotifyOnNextApertureChange = false
    private var mHaveApertureControl = false

    // Exposure compensation
    private var mMaxExposureCompensation = 0
    private var mMinExposureCompensation = 0
    private var mCurExposureCompensation = 0
    private var mExposureCompensationStep = 0f

    // Preview magnification
    private var mSupportedPreviewMagnifications: List<Int?>? = null
    private var mZoomLeverPressed = false
    private var mCurPreviewMagnification = 0
    private var mCurPreviewMagnificationFactor = 0f
    private var mCurPreviewMagnificationPos = Pair(0, 0)
    private var mCurPreviewMagnificationMaxPos = 0
    private var mPreviewNavView: PreviewNavView? = null

    internal enum class DialMode {
        SHUTTER, APERTURE, ISO, EXPOSURE, MODE, DRIVE, TIMELAPSE, BRACKET, TIMELAPSE_SET_INTERVAL, TIMELAPSE_SET_PIC_COUNT, BRACKET_SET_STEP, BRACKET_SET_PIC_COUNT
    }

    private var mDialMode: DialMode? = null

    internal enum class SceneMode {
        MANUAL, APERTURE, SHUTTER, OTHER
    }

    private var mSceneMode: SceneMode? = null
    private val mHandler = Handler()
    private val mHideMessageRunnable = Runnable { mTvMsg!!.visibility = View.GONE }
    private var mTakingPicture = false
    private var mShutterKeyDown = false
    private var mHaveTouchscreen = false
    private var mViewFlags = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        if (Thread.getDefaultUncaughtExceptionHandler() !is CustomExceptionHandler) Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler())
        val surfaceView = findViewById(R.id.surfaceView) as SurfaceView
        surfaceView.setOnTouchListener(SurfaceSwipeTouchListener(this))
        mSurfaceHolder = surfaceView.holder
        mSurfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        // not needed - appears to be the default font
        //final Typeface sonyFont = Typeface.createFromFile("system/fonts/Sony_DI_Icons.ttf");
        mTvMsg = findViewById(R.id.tvMsg) as TextView
        mTvAperture = findViewById(R.id.tvAperture) as TextView
        mTvAperture!!.setOnTouchListener(ApertureSwipeTouchListener(this))
        mTvShutter = findViewById(R.id.tvShutter) as TextView
        mTvShutter!!.setOnTouchListener(ShutterSwipeTouchListener(this))
        mTvISO = findViewById(R.id.tvISO) as TextView
        mTvISO!!.setOnTouchListener(IsoSwipeTouchListener(this))
        mTvExposureCompensation = findViewById(R.id.tvExposureCompensation) as TextView
        mTvExposureCompensation!!.setOnTouchListener(ExposureSwipeTouchListener(this))
        mLExposure = findViewById(R.id.lExposure) as LinearLayout
        mTvExposure = findViewById(R.id.tvExposure) as TextView
        mTvExposure!!.setCompoundDrawablesWithIntrinsicBounds(SonyDrawables.p_meteredmanualicon, 0, 0, 0)
        mTvLog = findViewById(R.id.tvLog) as TextView
        mTvLog!!.visibility = if (LOGGING_ENABLED) View.VISIBLE else View.GONE
        mVHist = findViewById(R.id.vHist) as HistogramView
        mTvMagnification = findViewById(R.id.tvMagnification) as TextView
        mLInfoBottom = findViewById(R.id.lInfoBottom) as TableLayout
        mPreviewNavView = findViewById(R.id.vPreviewNav) as PreviewNavView
        mPreviewNavView!!.visibility = View.GONE
        mIvDriveMode = findViewById(R.id.ivDriveMode) as ImageView
        mIvDriveMode!!.setOnClickListener(this)
        mIvMode = findViewById(R.id.ivMode) as ImageView
        mIvMode!!.setOnClickListener(this)
        mIvTimelapse = findViewById(R.id.ivTimelapse) as ImageView
        mIvTimelapse!!.setImageResource(SonyDrawables.p_16_dd_parts_43_shoot_icon_setting_drivemode_invalid)
        mIvTimelapse!!.setOnClickListener(this)
        mIvBracket = findViewById(R.id.ivBracket) as ImageView
        mIvBracket!!.setImageResource(SonyDrawables.p_16_dd_parts_contshot)
        mIvBracket!!.setOnClickListener(this)
        mVGrid = findViewById(R.id.vGrid) as GridView
        mTvHint = findViewById(R.id.tvHint) as TextView
        mTvHint!!.visibility = View.GONE
        mFocusScaleView = findViewById(R.id.vFocusScale) as FocusScaleView
        mLFocusScale = findViewById(R.id.lFocusScale)
        mLFocusScale?.visibility = View.GONE
        (findViewById(R.id.ivFocusRight) as ImageView).setImageResource(SonyDrawables.p_16_dd_parts_rec_focuscontrol_far)
        (findViewById(R.id.ivFocusLeft) as ImageView).setImageResource(SonyDrawables.p_16_dd_parts_rec_focuscontrol_near)
        setDialMode(DialMode.SHUTTER)
        mPrefs = Preferences(this)
        mHaveTouchscreen = deviceInfo.model.compareTo("ILCE-5100") == 0
    }

    private inner class SurfaceSwipeTouchListener(context: Context?) : OnSwipeTouchListener(context) {
        override fun onScrolled(distanceX: Float, distanceY: Float): Boolean {
            if (mCurPreviewMagnification != 0) {
                mCurPreviewMagnificationPos = Pair(max(min(mCurPreviewMagnificationMaxPos, mCurPreviewMagnificationPos.first + distanceX.toInt()), -mCurPreviewMagnificationMaxPos),
                        max(min(mCurPreviewMagnificationMaxPos, mCurPreviewMagnificationPos.second + distanceY.toInt()), -mCurPreviewMagnificationMaxPos))
                mCamera!!.setPreviewMagnification(mCurPreviewMagnification, mCurPreviewMagnificationPos)
                return true
            }
            return false
        }
    }

    private inner class ApertureSwipeTouchListener(context: Context?) : OnSwipeTouchListener(context) {
        private var mLastDistance = 0
        private var mAccumulatedDistance = 0
        override fun onScrolled(distanceX: Float, distanceY: Float): Boolean {
            if (mCurIso != 0) {
                val distance = (if (abs(distanceX) > abs(distanceY)) distanceX else -distanceY).toInt()
                if (mLastDistance > 0 != distance > 0) mAccumulatedDistance = distance else mAccumulatedDistance += distance
                mLastDistance = distance
                if (abs(mAccumulatedDistance) > 10) {
                    var i = abs(mAccumulatedDistance)
                    while (i > 10) {
                        mNotifyOnNextApertureChange = true
                        if (distance > 0) mCamera!!.decrementAperture() else mCamera!!.incrementAperture()
                        i -= 10
                    }
                    mAccumulatedDistance = 0
                    return true
                }
            }
            return false
        }
    }

    private inner class ShutterSwipeTouchListener(context: Context?) : OnSwipeTouchListener(context) {
        private var mLastDistance = 0
        private var mAccumulatedDistance = 0
        override fun onScrolled(distanceX: Float, distanceY: Float): Boolean {
            if (mCurIso != 0) {
                val distance = (if (abs(distanceX) > abs(distanceY)) distanceX else -distanceY).toInt()
                if (mLastDistance > 0 != distance > 0) mAccumulatedDistance = distance else mAccumulatedDistance += distance
                mLastDistance = distance
                if (abs(mAccumulatedDistance) > 10) {
                    var i = abs(mAccumulatedDistance)
                    while (i > 10) {
                        mNotifyOnNextShutterSpeedChange = true
                        if (distance > 0) mCamera!!.decrementShutterSpeed() else mCamera!!.incrementShutterSpeed()
                        i -= 10
                    }
                    mAccumulatedDistance = 0
                    return true
                }
            }
            return false
        }

        override fun onClick(): Boolean {
            if (mSceneMode == SceneMode.APERTURE) {
                // Set minimum shutter speed
                startActivity(Intent(applicationContext, MinShutterActivity::class.java))
                return true
            }
            return false
        }
    }

    private inner class ExposureSwipeTouchListener(context: Context?) : OnSwipeTouchListener(context) {
        private var mLastDistance = 0
        private var mAccumulatedDistance = 0
        override fun onScrolled(distanceX: Float, distanceY: Float): Boolean {
            if (mCurIso != 0) {
                val distance = (if (abs(distanceX) > abs(distanceY)) distanceX else -distanceY).toInt()
                if (mLastDistance > 0 != distance > 0) mAccumulatedDistance = distance else mAccumulatedDistance += distance
                mLastDistance = distance
                if (abs(mAccumulatedDistance) > 10) {
                    var i = abs(mAccumulatedDistance)
                    while (i > 10) {
                        if (distance > 0) decrementExposureCompensation(true) else incrementExposureCompensation(true)
                        i -= 10
                    }
                    mAccumulatedDistance = 0
                    return true
                }
            }
            return false
        }

        override fun onClick(): Boolean {
            // Reset exposure compensation
            setExposureCompensation(0, false)
            return true
        }
    }

    private inner class IsoSwipeTouchListener(context: Context?) : OnSwipeTouchListener(context) {
        private var mLastDistance = 0
        private var mAccumulatedDistance = 0
        override fun onScrolled(distanceX: Float, distanceY: Float): Boolean {
            if (mCurIso != 0) {
                val distance = (if (abs(distanceX) > abs(distanceY)) distanceX else -distanceY).toInt()
                if (mLastDistance > 0 != distance > 0) mAccumulatedDistance = distance else mAccumulatedDistance += distance
                mLastDistance = distance
                if (abs(mAccumulatedDistance) > 10) {
                    var iso = mCurIso
                    var i = abs(mAccumulatedDistance)
                    while (i > 10) {
                        iso = if (distance > 0) getPreviousIso(iso) else getNextIso(iso)
                        i -= 10
                    }
                    mAccumulatedDistance = 0
                    if (iso != 0) {
                        setIso(iso)
                        showMessage(String.format("\uE488 %d", iso))
                    }
                    return true
                }
            }
            return false
        }

        override fun onClick(): Boolean {
            // Toggle manual / automatic ISO
            setIso(if (mCurIso == 0) firstManualIso else 0)
            showMessage(if (mCurIso == 0) "Auto \uE488" else "Manual \uE488")
            return true
        }
    }

    private fun showMessage(msg: String) {
        mTvMsg!!.text = msg
        mTvMsg!!.visibility = View.VISIBLE
        mHandler.removeCallbacks(mHideMessageRunnable)
        mHandler.postDelayed(mHideMessageRunnable, MESSAGE_TIMEOUT.toLong())
    }

    private fun log(str: String) {
        if (LOGGING_ENABLED) mTvLog!!.append(str)
    }

    private fun setIso(iso: Int) {
        //log("setIso: " + String.valueOf(iso) + "\n");
        mCurIso = iso
        mTvISO!!.text = String.format("\uE488 %s", if (iso == 0) "AUTO" else iso.toString())
        val params = mCamera!!.createEmptyParameters()
        mCamera!!.createParametersModifier(params).isoSensitivity = iso
        mCamera!!.normalCamera.parameters = params
    }

    private fun getPreviousIso(current: Int): Int {
        val previousIsoIndex = mSupportedIsos!!.indexOf(current) - 1
        return if (previousIsoIndex < 0)
            0
        else
            mSupportedIsos!![previousIsoIndex]
    }

    private fun getNextIso(current: Int): Int {
        val nextIsoIndex = mSupportedIsos!!.indexOf(current) + 1
        return if (nextIsoIndex >= mSupportedIsos!!.size)
            mSupportedIsos!!.last()
        else
            mSupportedIsos!![nextIsoIndex]
    }

    private val firstManualIso: Int
        get() {
            for (iso in mSupportedIsos!!) {
                if (iso != 0) return iso
            }
            return 0
        }

    private fun updateShutterSpeed(n: Int, d: Int) {
        val text = formatShutterSpeed(n, d)
        mTvShutter!!.text = text
        if (mNotifyOnNextShutterSpeedChange) {
            showMessage(text)
            mNotifyOnNextShutterSpeedChange = false
        }
    }

    private fun setExposureCompensation(value: Int, notify: Boolean) {
        mCurExposureCompensation = value
        val params = mCamera!!.createEmptyParameters()
        params.exposureCompensation = value
        mCamera!!.normalCamera.parameters = params
        updateExposureCompensation(notify)
    }

    private fun decrementExposureCompensation(notify: Boolean) {
        if (mCurExposureCompensation > mMinExposureCompensation) {
            setExposureCompensation(mCurExposureCompensation - 1, notify)
        }
    }

    private fun incrementExposureCompensation(notify: Boolean) {
        if (mCurExposureCompensation < mMaxExposureCompensation) {
            setExposureCompensation(mCurExposureCompensation + 1, notify)
        }
    }

    private fun updateExposureCompensation(notify: Boolean) {
        val text: String = when {
            mCurExposureCompensation == 0 -> "\uEB18\u00B10.0"
            mCurExposureCompensation > 0 -> String.format("\uEB18+%.1f", mCurExposureCompensation * mExposureCompensationStep)
            else -> String.format("\uEB18%.1f", mCurExposureCompensation * mExposureCompensationStep)
        }
        mTvExposureCompensation!!.text = text
        if (notify) showMessage(text)
    }

    private fun updateSceneModeImage(mode: String = mCamera!!.normalCamera.parameters.sceneMode) {
        //log(String.format("updateSceneModeImage %s\n", mode));
        mSceneMode = when (mode) {
            ParametersModifier.SCENE_MODE_MANUAL_EXPOSURE -> {
                mIvMode!!.setImageResource(SonyDrawables.s_16_dd_parts_osd_icon_mode_m)
                SceneMode.MANUAL
            }
            ParametersModifier.SCENE_MODE_APERTURE_PRIORITY -> {
                mIvMode!!.setImageResource(SonyDrawables.s_16_dd_parts_osd_icon_mode_a)
                SceneMode.APERTURE
            }
            ParametersModifier.SCENE_MODE_SHUTTER_PRIORITY -> {
                mIvMode!!.setImageResource(SonyDrawables.s_16_dd_parts_osd_icon_mode_s)
                SceneMode.SHUTTER
            }
            else -> {
                mIvMode!!.setImageResource(SonyDrawables.p_dialogwarning)
                SceneMode.OTHER
            }
        }
    }

    private fun updateViewVisibility() {
        mVHist!!.visibility = if (mViewFlags and VIEW_FLAG_HISTOGRAM != 0) View.VISIBLE else View.GONE
        mVGrid!!.visibility = if (mViewFlags and VIEW_FLAG_GRID != 0) View.VISIBLE else View.GONE
        mLExposure!!.visibility = if (mViewFlags and VIEW_FLAG_EXPOSURE != 0) View.VISIBLE else View.GONE
    }

    private fun cycleVisibleViews() {
        if (++mViewFlags > VIEW_FLAG_MASK) mViewFlags = 0
        updateViewVisibility()
    }

    private fun toggleSceneMode() {
        val newMode: String
        when (mSceneMode) {
            SceneMode.MANUAL -> {
                newMode = ParametersModifier.SCENE_MODE_APERTURE_PRIORITY
                if (mDialMode != DialMode.MODE) setDialMode(if (mHaveApertureControl) DialMode.APERTURE else DialMode.ISO)
                setMinShutterSpeed(mPrefs!!.minShutterSpeed)
            }
            else -> {
                newMode = ParametersModifier.SCENE_MODE_MANUAL_EXPOSURE
                if (mDialMode != DialMode.MODE) setDialMode(DialMode.SHUTTER)
                setMinShutterSpeed(-1)
            }
        }
        setSceneMode(newMode)
    }

    private fun toggleDriveMode() {
        val normalCamera = mCamera!!.normalCamera
        val paramsModifier = mCamera!!.createParametersModifier(normalCamera.parameters)
        val driveMode = paramsModifier.driveMode
        val newMode: String
        val newBurstSpeed: String
        if (driveMode == ParametersModifier.DRIVE_MODE_SINGLE) {
            newMode = ParametersModifier.DRIVE_MODE_BURST
            newBurstSpeed = ParametersModifier.BURST_DRIVE_SPEED_HIGH
        } else if (driveMode == ParametersModifier.DRIVE_MODE_BURST) {
            val burstDriveSpeed = paramsModifier.burstDriveSpeed
            if (burstDriveSpeed == ParametersModifier.BURST_DRIVE_SPEED_LOW) {
                newMode = ParametersModifier.DRIVE_MODE_SINGLE
                newBurstSpeed = burstDriveSpeed
            } else {
                newMode = driveMode
                newBurstSpeed = ParametersModifier.BURST_DRIVE_SPEED_LOW
            }
        } else {
            // Anything else...
            newMode = ParametersModifier.DRIVE_MODE_SINGLE
            newBurstSpeed = ParametersModifier.BURST_DRIVE_SPEED_HIGH
        }
        val params = mCamera!!.createEmptyParameters()
        val newParamsModifier = mCamera!!.createParametersModifier(params)
        newParamsModifier.driveMode = newMode
        newParamsModifier.burstDriveSpeed = newBurstSpeed
        mCamera!!.normalCamera.parameters = params
        updateDriveModeImage()
    }

    private fun updateDriveModeImage() {
        val paramsModifier = mCamera!!.createParametersModifier(mCamera!!.normalCamera.parameters)
        val driveMode = paramsModifier.driveMode
        if (driveMode == ParametersModifier.DRIVE_MODE_SINGLE) {
            mIvDriveMode!!.setImageResource(SonyDrawables.p_drivemode_n_001)
        } else if (driveMode == ParametersModifier.DRIVE_MODE_BURST) {
            val burstDriveSpeed = paramsModifier.burstDriveSpeed
            if (burstDriveSpeed == ParametersModifier.BURST_DRIVE_SPEED_LOW) {
                mIvDriveMode!!.setImageResource(SonyDrawables.p_drivemode_n_003)
            } else if (burstDriveSpeed == ParametersModifier.BURST_DRIVE_SPEED_HIGH) {
                mIvDriveMode!!.setImageResource(SonyDrawables.p_drivemode_n_002)
            }
        } else  //if (driveMode.equals("bracket"))
        {
            // Don't really care about this here
            mIvDriveMode!!.setImageResource(SonyDrawables.p_dialogwarning)
        }
    }

    private fun togglePreviewMagnificationViews(magnificationActive: Boolean) {
        mPreviewNavView!!.visibility = if (magnificationActive) View.VISIBLE else View.GONE
        mTvMagnification!!.visibility = if (magnificationActive) View.VISIBLE else View.GONE
        mLInfoBottom!!.visibility = if (magnificationActive) View.GONE else View.VISIBLE
        mVHist!!.visibility = if (magnificationActive) View.GONE else View.VISIBLE
        setLeftViewVisibility(!magnificationActive)
    }

    private fun setSceneMode(mode: String) {
        val params = mCamera!!.createEmptyParameters()
        params.sceneMode = mode
        mCamera!!.normalCamera.parameters = params
        updateSceneModeImage(mode)
    }

    private fun saveDefaults() {
        val params = mCamera!!.normalCamera.parameters
        val paramsModifier = mCamera!!.createParametersModifier(params)
        // Scene mode
        mPrefs!!.sceneMode = params.sceneMode
        // Drive mode and burst speed
        mPrefs!!.driveMode = paramsModifier.driveMode
        mPrefs!!.burstDriveSpeed = paramsModifier.burstDriveSpeed
        // View visibility
        mPrefs!!.setViewFlags(mViewFlags)

        // TODO: Dial mode
    }

    private fun disableLENR() {
        // Disable long exposure noise reduction
        val params = mCamera!!.createEmptyParameters()
        val paramsModifier = mCamera!!.createParametersModifier(mCamera!!.normalCamera.parameters)
        val modifier = mCamera!!.createParametersModifier(params)
        if (paramsModifier.isSupportedLongExposureNR) modifier.longExposureNR = false
        mCamera!!.normalCamera.parameters = params
    }

    private fun loadDefaults() {
        val params = mCamera!!.createEmptyParameters()
        val modifier = mCamera!!.createParametersModifier(params)
        // Focus mode
        params.focusMode = ParametersModifier.FOCUS_MODE_MANUAL
        // Scene mode
        val sceneMode = mPrefs!!.sceneMode
        params.sceneMode = sceneMode
        // Drive mode and burst speed
        modifier.driveMode = mPrefs!!.driveMode
        modifier.burstDriveSpeed = mPrefs!!.burstDriveSpeed
        // Minimum shutter speed
        if (sceneMode == ParametersModifier.SCENE_MODE_MANUAL_EXPOSURE) modifier.autoShutterSpeedLowLimit = -1 else modifier.autoShutterSpeedLowLimit = mPrefs!!.minShutterSpeed
        // Disable self timer
        modifier.selfTimer = 0
        // Force aspect ratio to 3:2
        modifier.imageAspectRatio = ParametersModifier.IMAGE_ASPECT_RATIO_3_2
        // Apply
        mCamera!!.normalCamera.parameters = params
        // View visibility
        mViewFlags = mPrefs!!.getViewFlags(VIEW_FLAG_GRID or VIEW_FLAG_HISTOGRAM)
        // TODO: Dial mode?
        setDialMode(DialMode.SHUTTER)
        disableLENR()
    }

    private fun setMinShutterSpeed(speed: Int) {
        val params = mCamera!!.createEmptyParameters()
        val modifier = mCamera!!.createParametersModifier(params)
        modifier.autoShutterSpeedLowLimit = speed
        mCamera!!.normalCamera.parameters = params
    }

    override fun onResume() {
        super.onResume()
        mCamera = open(0, null)
        mSurfaceHolder!!.addCallback(this)
        mCamera?.startDirectShutter()
        mAutoReviewControl = AutoPictureReviewControl()
        mCamera?.setAutoPictureReviewControl(mAutoReviewControl)
        // Disable picture review
        mPictureReviewTime = mAutoReviewControl!!.pictureReviewTime
        mAutoReviewControl!!.pictureReviewTime = 0
        mVGrid!!.setVideoRect(displayManager?.displayedVideoRect)

        //log(String.format("getSavingBatteryMode %s\n", getDisplayManager().getSavingBatteryMode()));
        //log(String.format("getScreenGainControlType %s\n", getDisplayManager().getScreenGainControlType()));
        val params = mCamera?.normalCamera?.parameters
        val paramsModifier = mCamera?.createParametersModifier(params)

        // Exposure compensation
        mMaxExposureCompensation = params!!.maxExposureCompensation
        mMinExposureCompensation = params.minExposureCompensation
        mExposureCompensationStep = params.exposureCompensationStep
        mCurExposureCompensation = params.exposureCompensation
        updateExposureCompensation(false)

        /*
        log(String.format("isSupportedFocusHold %b\n", paramsModifier.isSupportedFocusHold()));
        log(String.format("isFocusDriveSupported %b\n", paramsModifier.isFocusDriveSupported()));
        log(String.format("MaxFocusDriveSpeed %d\n", paramsModifier.getMaxFocusDriveSpeed())); // 0
        log(String.format("MaxFocusShift %d\n", paramsModifier.getMaxFocusShift()));
        log(String.format("MinFocusShift %d\n", paramsModifier.getMinFocusShift()));
        log(String.format("isSupportedFocusShift %b\n", paramsModifier.isSupportedFocusShift()));
        dumpList(paramsModifier.getSupportedSelfTimers(), "SupportedSelfTimers");
        */

        //log(String.format("driveMode %s\n", paramsModifier.getDriveMode()));
        //log(String.format("burstDriveSpeed %s\n", paramsModifier.getBurstDriveSpeed()));
        //log(String.format("burstDriveButtonReleaseBehave %s\n", paramsModifier.getBurstDriveButtonReleaseBehave()));

        /*
        dumpList(paramsModifier.getSupportedDriveModes(), "SupportedDriveModes");   // single, burst, bracket
        dumpList(paramsModifier.getSupportedBurstDriveSpeeds(), "SupportedBurstDriveSpeeds");   // low, high
        dumpList(paramsModifier.getSupportedBurstDriveButtonReleaseBehaves(), "SupportedBurstDriveButtonReleaseBehaves");   // null
        dumpList(paramsModifier.getSupportedBracketModes(), "SupportedBracketModes");   // exposure, white-balance, dro
        dumpList(paramsModifier.getSupportedBracketOrders(), "SupportedBracketOrders"); // null
        dumpList(paramsModifier.getSupportedBracketStepPeriods(), "SupportedBracketStepPeriods");   // low, high
        dumpList(paramsModifier.getSupportedExposureBracketModes(), "SupportedExposureBracketModes");   // single, continue
        dumpList(paramsModifier.getSupportedExposureBracketPeriods(), "SupportedExposureBracketPeriods");   // 3, 5, 7, 10, 20, 30
        //dumpList(paramsModifier.getSupportedIsoAutoMinShutterSpeedModes(), "SupportedIsoAutoMinShutterSpeedModes"); // NoSuchMethodError
        log(String.format("isSupportedAutoShutterSpeedLowLimit %b\n", paramsModifier.isSupportedAutoShutterSpeedLowLimit()));
        log(String.format("AutoShutterSpeedLowLimit %d\n", paramsModifier.getAutoShutterSpeedLowLimit()));   // -1 = clear?
        dumpList(Settings.getSupportedAutoPowerOffTimes(), "getSupportedAutoPowerOffTimes");
        log(String.format("getAutoPowerOffTime %d\n", Settings.getAutoPowerOffTime()));  // in seconds
        */

        // Preview/Histogram
        mCamera?.setPreviewAnalizeListener { analizedData, _ -> if (analizedData?.hist != null && analizedData.hist.Y != null && mVHist!!.visibility == View.VISIBLE) mVHist!!.setHistogram(analizedData.hist.Y) }

        // ISO
        mCamera?.setAutoISOSensitivityListener { i, cameraEx -> //log("AutoISOChanged " + String.valueOf(i) + "\n");
            mTvISO!!.text = "\uE488 " + i.toString() + if (mCurIso == 0) "(A)" else ""
        }

        // Shutter
        mCamera?.setShutterSpeedChangeListener(this)
        mCamera?.setShutterListener(this)

        /*
        m_camera.setCaptureStatusListener(new CameraEx.OnCaptureStatusListener()
        {
            @Override
            public void onEnd(int i, int i1, CameraEx cameraEx)
            {
                log(String.format("onEnd i %d i1 %d\n", i, i1));
            }

            @Override
            public void onStart(int i, CameraEx cameraEx)
            {
                log(String.format("onStart i %d\n", i));
            }
        });
        */

        /*
        m_camera.setSettingChangedListener(new CameraEx.SettingChangedListener()
        {
            @Override
            public void onChanged(int[] ints, Camera.Parameters parameters, CameraEx cameraEx)
            {
                for (int value : ints)
                {
                    log("Setting changed: " + String.valueOf(value) + "\n");
                }
            }
        });
        */

        /*
        m_camera.setAutoSceneModeListener(new CameraEx.AutoSceneModeListener()
        {
            @Override
            public void onChanged(String s, CameraEx cameraEx)
            {
                log(String.format("AutoSceneModeListener: %s\n", s));
            }
        });
        */

        // test: list scene modes
        /*
        List<String> scenesModes = params.getSupportedSceneModes();
        for (String s : scenesModes)
            log(s + "\n");
        log("Current scene mode: " + params.getSceneMode() + "\n");
        */

        // Aperture
        mCamera?.setApertureChangeListener { apertureInfo, _ -> // Disable aperture control if not available
            mHaveApertureControl = apertureInfo.currentAperture != 0
            mTvAperture!!.visibility = if (mHaveApertureControl) View.VISIBLE else View.GONE
            /*
                log(String.format("currentAperture %d currentAvailableMin %d currentAvailableMax %d\n",
                        apertureInfo.currentAperture, apertureInfo.currentAvailableMin, apertureInfo.currentAvailableMax));
                */
            val text = String.format("f%.1f", apertureInfo.currentAperture.toFloat() / 100.0f)
            mTvAperture!!.text = text
            if (mNotifyOnNextApertureChange) {
                mNotifyOnNextApertureChange = false
                showMessage(text)
            }
        }

        // Exposure metering
        mCamera?.setProgramLineRangeOverListener(object : ProgramLineRangeOverListener {
            override fun onAERange(b: Boolean, b1: Boolean, b2: Boolean, cameraEx: CameraEx) {
                //log(String.format("onARRange b %b b1 %b b2 %b\n", Boolean.valueOf(b), Boolean.valueOf(b1), Boolean.valueOf(b2)));
            }

            override fun onEVRange(ev: Int, cameraEx: CameraEx) {
                val text: String = if (ev == 0) "\u00B10.0" else if (ev > 0) String.format("+%.1f", ev.toFloat() / 3.0f) else String.format("%.1f", ev.toFloat() / 3.0f)
                mTvExposure!!.text = text
                //log(String.format("onEVRange i %d %f\n", ev, (float)ev / 3.0f));
            }

            override fun onMeteringRange(b: Boolean, cameraEx: CameraEx) {
                //log(String.format("onMeteringRange b %b\n", Boolean.valueOf(b)));
            }
        })
        mSupportedIsos = paramsModifier?.supportedISOSensitivities as List<Int>?
        mCurIso = paramsModifier!!.isoSensitivity
        mTvISO!!.text = String.format("\uE488 %d", mCurIso)
        mTvAperture!!.text = String.format("f%.1f", paramsModifier.aperture.toFloat() / 100.0f)
        val sp: Pair<Int, Int> = paramsModifier.shutterSpeed as Pair<Int, Int>
        updateShutterSpeed(sp.first, sp.second)
        mSupportedPreviewMagnifications = paramsModifier.supportedPreviewMagnification as List<Int?>
        mCamera?.setPreviewMagnificationListener(object : PreviewMagnificationListener {
            override fun onChanged(enabled: Boolean, magFactor: Int, magLevel: Int, coords: Pair<*, *>?, cameraEx: CameraEx) {
                // magnification / 100 = x.y
                // magLevel = value passed to setPreviewMagnification
                /*
                m_tvLog.setText("onChanged enabled:" + String.valueOf(enabled) + " magFactor:" + String.valueOf(magFactor) + " magLevel:" +
                    String.valueOf(magLevel) + " x:" + coords.first + " y:" + coords.second + "\n");
                */
                if (enabled) {
                    //log("m_curPreviewMagnificationMaxPos: " + String.valueOf(m_curPreviewMagnificationMaxPos) + "\n");
                    mCurPreviewMagnification = magLevel
                    mCurPreviewMagnificationFactor = magFactor.toFloat() / 100.0f
                    mCurPreviewMagnificationMaxPos = 1000 - (1000.0f / mCurPreviewMagnificationFactor).toInt()
                    mTvMagnification!!.text = String.format("\uE012 %.2fx", magFactor.toFloat() / 100.0f)
                    mPreviewNavView!!.update(coords as Pair<Int, Int>, mCurPreviewMagnificationFactor)
                } else {
                    mPreviewNavView!!.update(null, 0f)
                    mCurPreviewMagnification = 0
                    mCurPreviewMagnificationMaxPos = 0
                    mCurPreviewMagnificationFactor = 0f
                }
                togglePreviewMagnificationViews(enabled)
            }

            override fun onInfoUpdated(b: Boolean, coords: Pair<*, *>?, cameraEx: CameraEx) {
                // Useless?
                /*
                log("onInfoUpdated b:" + String.valueOf(b) +
                               " x:" + coords.first + " y:" + coords.second + "\n");
                */
            }
        })
        mCamera?.setFocusDriveListener { focusPosition, cameraEx ->
            if (mCurPreviewMagnification == 0) {
                mLFocusScale!!.visibility = View.VISIBLE
                mFocusScaleView!!.setMaxPosition(focusPosition.maxPosition)
                mFocusScaleView!!.setCurPosition(focusPosition.currentPosition)
                mHandler.removeCallbacks(mHideFocusScaleRunnable)
                mHandler.postDelayed(mHideFocusScaleRunnable, 2000)
            }
        }
        loadDefaults()
        updateDriveModeImage()
        updateSceneModeImage()
        updateViewVisibility()

        /* - triggers NPE
        List<Integer> pf = params.getSupportedPreviewFormats();
        if (pf != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("SupportedPreviewFormats: ");
            for (Integer i : pf)
                sb.append(i.toString()).append(",");
            sb.append("\n");
            log(sb.toString());
        }
        */
        /* - return null
        List<Integer> pfr = params.getSupportedPreviewFrameRates();
        if (pfr != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("SupportedPreviewFrameRates: ");
            for (Integer i : pfr)
                sb.append(i.toString()).append(",");
            sb.append("\n");
            log(sb.toString());
        }
        List<Camera.Size> ps = params.getSupportedPreviewSizes();
        if (ps != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("SupportedPreviewSizes: ");
            for (Camera.Size s : ps)
                sb.append(s.toString()).append(",");
            sb.append("\n");
            log(sb.toString());
        }
        */
    }

    override fun onPause() {
        super.onPause()
        saveDefaults()
        mSurfaceHolder!!.removeCallback(this)
        mAutoReviewControl!!.pictureReviewTime = mPictureReviewTime
        mCamera!!.setAutoPictureReviewControl(null)
        mCamera!!.normalCamera.stopPreview()
        mCamera!!.release()
        mCamera = null
    }

    override fun onShutterSpeedChange(shutterSpeedInfo: ShutterSpeedInfo, cameraEx: CameraEx) {
        updateShutterSpeed(shutterSpeedInfo.currentShutterSpeed_n, shutterSpeedInfo.currentShutterSpeed_d)
        if (mBracketActive) {
            log(String.format("Want shutter speed %d/%d, got %d/%d\n",
                    mBracketNextShutterSpeed!!.first, mBracketNextShutterSpeed!!.second,
                    shutterSpeedInfo.currentShutterSpeed_n, shutterSpeedInfo.currentShutterSpeed_d
            ))
            if (shutterSpeedInfo.currentShutterSpeed_n == mBracketNextShutterSpeed!!.first &&
                    shutterSpeedInfo.currentShutterSpeed_d == mBracketNextShutterSpeed!!.second) {
                // Focus speed adjusted, take next picture
                mCamera!!.burstableTakePicture()
            }
        }
    }

    override fun onShutter(i: Int, cameraEx: CameraEx) {
        // i: 0 = success, 1 = canceled, 2 = error
        //log(String.format("onShutter i: %d\n", i));
        if (i != 0) {
            //log(String.format("onShutter ERROR %d\n", i));
            mTakingPicture = false
        }
        mCamera!!.cancelTakePicture()
        if (mTimelapseActive) onShutterTimelapse(i) else if (mBracketActive) onShutterBracket(i)
    }

    private fun onShutterBracket(i: Int) {
        if (i == 0) {
            if (--mBracketPicCount == 0) abortBracketing() else {
                mBracketShutterDelta += mBracketStep
                val shutterIndex = getShutterValueIndex(currentShutterSpeed)
                if (mBracketShutterDelta % 2 == 0) {
                    log(String.format("Adjusting shutter speed by %d\n", -mBracketShutterDelta))
                    // Even, reduce shutter speed
                    mBracketNextShutterSpeed = Pair(CameraUtil.SHUTTER_SPEEDS[shutterIndex + mBracketShutterDelta][0],
                            CameraUtil.SHUTTER_SPEEDS[shutterIndex + mBracketShutterDelta][1])
                    mCamera!!.adjustShutterSpeed(-mBracketShutterDelta)
                } else {
                    log(String.format("Adjusting shutter speed by %d\n", mBracketShutterDelta))
                    // Odd, increase shutter speed
                    mBracketNextShutterSpeed = Pair(CameraUtil.SHUTTER_SPEEDS[shutterIndex - mBracketShutterDelta][0],
                            CameraUtil.SHUTTER_SPEEDS[shutterIndex - mBracketShutterDelta][1])
                    mCamera!!.adjustShutterSpeed(mBracketShutterDelta)
                }
            }
        } else {
            abortBracketing()
        }
    }

    private fun onShutterTimelapse(i: Int) {
        if (i == 0) {
            ++mTimelapsePicsTaken
            if (mTimelapsePicCount < 0 || mTimelapsePicCount == 1) abortTimelapse() else {
                if (mTimelapsePicCount != 0) --mTimelapsePicCount
                if (mTimelapseInterval >= 1000) {
                    if (mTimelapsePicCount > 0) showMessage(String.format("%d pictures remaining", mTimelapsePicCount)) else showMessage(String.format("%d pictures taken", mTimelapsePicsTaken))
                }
                if (mTimelapseInterval != 0) mHandler.postDelayed(mTimelapseRunnable, mTimelapseInterval.toLong()) else mCamera!!.burstableTakePicture()
            }
        } else {
            abortTimelapse()
        }
    }

    // OnClickListener
    override fun onClick(view: View) {
        when (view.id) {
            R.id.ivDriveMode -> toggleDriveMode()
            R.id.ivMode -> toggleSceneMode()
            R.id.ivTimelapse -> prepareTimelapse()
            R.id.ivBracket -> prepareBracketing()
        }
    }

    private fun decrementTimelapseInterval() {
        if (mTimelapseInterval > 0) {
            mTimelapseInterval -= if (mTimelapseInterval <= 1000) 100 else 1000
        }
        updateTimelapseInterval()
    }

    private fun incrementTimelapseInterval() {
        mTimelapseInterval += if (mTimelapseInterval < 1000) 100 else 1000
        updateTimelapseInterval()
    }

    private fun decrementBracketStep() {
        if (mBracketStep > 1) {
            --mBracketStep
            updateBracketStep()
        }
    }

    private fun incrementBracketStep() {
        if (mBracketStep < 9) {
            ++mBracketStep
            updateBracketStep()
        }
    }

    private fun decrementBracketPicCount() {
        if (mBracketPicCount > 3) {
            mBracketPicCount -= 2
            updateBracketPicCount()
        }
    }

    private fun incrementBracketPicCount() {
        if (mBracketPicCount < mBracketMaxPicCount) {
            mBracketPicCount += 2
            updateBracketPicCount()
        }
    }

    private val currentShutterSpeed: Pair<Int, Int>
        private get() {
            val params = mCamera!!.normalCamera.parameters
            val paramsModifier = mCamera!!.createParametersModifier(params)
            return paramsModifier.shutterSpeed as Pair<Int, Int>
        }

    private fun calcMaxBracketPicCount() {
        val index = getShutterValueIndex(currentShutterSpeed)
        val maxSteps = min(index, CameraUtil.SHUTTER_SPEEDS.size - 1 - index)
        mBracketMaxPicCount = maxSteps / mBracketStep * 2 + 1
    }

    private fun updateBracketStep() {
        mTvMsg!!.visibility = View.VISIBLE
        val mod = mBracketStep % 3
        val ev: Int
        ev = when (mod) {
            0 -> 0
            1 -> 3
            else -> 7
        }
        mTvMsg!!.text = String.format("%d.%dEV", mBracketStep / 3, ev)
    }

    private fun updateBracketPicCount() {
        mTvMsg!!.visibility = View.VISIBLE
        mTvMsg!!.text = String.format("%d pictures", mBracketPicCount)
    }

    private fun updateTimelapseInterval() {
        mTvMsg!!.visibility = View.VISIBLE
        when {
            mTimelapseInterval == 0 -> mTvMsg!!.text = getString(R.string.no_delay)
            mTimelapseInterval < 1000 -> mTvMsg!!.text = String.format("%d msec", mTimelapseInterval)
            mTimelapseInterval == 1000 -> mTvMsg!!.text = getString(R.string.one_second)
            else -> mTvMsg!!.text = String.format(getString(R.string.variable_seconds), mTimelapseInterval / 1000)
        }
    }

    private fun updateTimelapsePictureCount() {
        mTvMsg!!.visibility = View.VISIBLE
        mTvMsg!!.text = when (mTimelapsePicCount) {
            0 -> getString(R.string.no_picture_limit)
            else -> String.format(getString(R.string.variable_pictures), mTimelapsePicCount)
        }
    }

    private fun decrementTimelapsePicCount() {
        if (mTimelapsePicCount > 0) --mTimelapsePicCount
        updateTimelapsePictureCount()
    }

    private fun incrementTimelapsePicCount() {
        ++mTimelapsePicCount
        updateTimelapsePictureCount()
    }

    private fun prepareBracketing() {
        if (mDialMode == DialMode.BRACKET_SET_STEP || mDialMode == DialMode.BRACKET_SET_PIC_COUNT) {
            abortBracketing()
        } else {
            if (mSceneMode != SceneMode.MANUAL) {
                showMessage("Scene mode must be set to manual")
                return
            }
            if (mCurIso == 0) {
                showMessage("ISO must be set to manual")
                return
            }
            setLeftViewVisibility(false)
            setDialMode(DialMode.BRACKET_SET_STEP)
            mBracketPicCount = 3
            mBracketStep = 3
            mBracketShutterDelta = 0
            updateBracketStep()

            // Remember current shutter speed
            mBracketNeutralShutterIndex = getShutterValueIndex(currentShutterSpeed)
        }
    }

    private fun abortBracketing() {
        mHandler.removeCallbacks(mCountDownRunnable)
        //m_handler.removeCallbacks(m_timelapseRunnable);
        mBracketActive = false
        showMessage("Bracketing finished")
        setDialMode(DialMode.SHUTTER)
        mCamera!!.startDirectShutter()
        mCamera!!.normalCamera.startPreview()

        // Update controls
        mTvHint!!.visibility = View.GONE
        setLeftViewVisibility(true)
        updateSceneModeImage()
        updateDriveModeImage()
        mViewFlags = mPrefs!!.getViewFlags(mViewFlags)
        updateViewVisibility()

        // Reset to previous shutter speed
        val shutterDiff = mBracketNeutralShutterIndex - getShutterValueIndex(currentShutterSpeed)
        if (shutterDiff != 0) mCamera!!.adjustShutterSpeed(-shutterDiff)
    }

    private fun prepareTimelapse() {
        if (mDialMode == DialMode.TIMELAPSE_SET_INTERVAL || mDialMode == DialMode.TIMELAPSE_SET_PIC_COUNT) abortTimelapse() else {
            setLeftViewVisibility(false)
            setDialMode(DialMode.TIMELAPSE_SET_INTERVAL)
            mTimelapseInterval = 1000
            updateTimelapseInterval()
            mTvHint!!.text = "\uE4CD to set timelapse interval, \uE04C to confirm"
            mTvHint!!.visibility = View.VISIBLE

            // Not supported on some camera models
            try {
                mAutoPowerOffTimeBackup = Settings.getAutoPowerOffTime()
            } catch (e: NoSuchMethodError) {
            }
        }
    }

    private fun setLeftViewVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        mIvTimelapse!!.visibility = visibility
        mIvDriveMode!!.visibility = visibility
        mIvMode!!.visibility = visibility
        mIvBracket!!.visibility = visibility
    }

    private fun startBracketCountdown() {
        mBracketActive = true
        mCamera!!.stopDirectShutter { }
        // Stop preview
        mCamera!!.normalCamera.stopPreview()

        // Hide some bottom views
        mPrefs!!.setViewFlags(mViewFlags)
        mViewFlags = 0
        updateViewVisibility()

        // Start countdown
        mCountdown = COUNTDOWN_SECONDS
        mTvMsg!!.text = String.format("Starting in %d...", mCountdown)
        mHandler.postDelayed(mCountDownRunnable, 1000)
    }

    private fun startTimelapseCountdown() {
        mTimelapseActive = true
        mCamera!!.stopDirectShutter { }
        mTvHint!!.text = "\uE04C to abort"
        // Stop preview (doesn't seem to preserve battery life?)
        mCamera!!.normalCamera.stopPreview()

        // Hide some bottom views
        mPrefs!!.setViewFlags(mViewFlags)
        mViewFlags = 0
        updateViewVisibility()

        // Start countdown
        mCountdown = COUNTDOWN_SECONDS
        mTvMsg!!.text = String.format(getString(R.string.starting_in_variable), mCountdown)
        mHandler.postDelayed(mCountDownRunnable, 1000)
    }

    private fun startShootingBracket() {
        mTvHint!!.visibility = View.GONE
        mTvMsg!!.visibility = View.GONE
        // Take first picture at set shutter speed
        mCamera!!.burstableTakePicture()
    }

    private fun startShootingTimelapse() {
        mTvHint!!.visibility = View.GONE
        mTvMsg!!.visibility = View.GONE
        try {
            Settings.setAutoPowerOffTime(mTimelapseInterval / 1000 * 2)
        } catch (e: NoSuchMethodError) {
        }
        mHandler.post(mTimelapseRunnable)
    }

    private fun abortTimelapse() {
        mHandler.removeCallbacks(mCountDownRunnable)
        mHandler.removeCallbacks(mTimelapseRunnable)
        mTimelapseActive = false
        showMessage(getString(R.string.timelapse_finished))
        setDialMode(DialMode.SHUTTER)
        mCamera!!.startDirectShutter()
        mCamera!!.normalCamera.startPreview()

        // Update controls
        mTvHint!!.visibility = View.GONE
        setLeftViewVisibility(true)
        updateSceneModeImage()
        updateDriveModeImage()
        mViewFlags = mPrefs!!.getViewFlags(mViewFlags)
        updateViewVisibility()
        try {
            Settings.setAutoPowerOffTime(mAutoPowerOffTimeBackup)
        } catch (e: NoSuchMethodError) {
        }
    }

    override fun onUpperDialChanged(value: Int): Boolean {
        return if (mCurPreviewMagnification != 0) {
            movePreviewHorizontal(value * (500.0f / mCurPreviewMagnificationFactor).toInt())
            true
        } else {
            when (mDialMode) {
                DialMode.SHUTTER -> if (value > 0) mCamera!!.incrementShutterSpeed() else mCamera!!.decrementShutterSpeed()
                DialMode.APERTURE -> if (value > 0) mCamera!!.incrementAperture() else mCamera!!.decrementAperture()
                DialMode.ISO -> {
                    val iso = if (value < 0) getPreviousIso(mCurIso) else getNextIso(mCurIso)
                    if (iso != 0) setIso(iso)
                }
                DialMode.EXPOSURE -> if (value < 0) decrementExposureCompensation(false) else incrementExposureCompensation(false)
                DialMode.TIMELAPSE_SET_INTERVAL -> if (value < 0) decrementTimelapseInterval() else incrementTimelapseInterval()
                DialMode.TIMELAPSE_SET_PIC_COUNT -> if (value < 0) decrementTimelapsePicCount() else incrementTimelapsePicCount()
                DialMode.BRACKET_SET_STEP -> if (value < 0) decrementBracketStep() else incrementBracketStep()
                DialMode.BRACKET_SET_PIC_COUNT -> if (value < 0) decrementBracketPicCount() else incrementBracketPicCount()
                DialMode.MODE -> TODO()
                DialMode.DRIVE -> TODO()
                DialMode.TIMELAPSE -> TODO()
                DialMode.BRACKET -> TODO()
                null -> TODO()
            }
            true
        }
    }

    private fun setDialMode(newMode: DialMode) {
        mDialMode = newMode
        mTvShutter!!.setTextColor(if (newMode == DialMode.SHUTTER) Color.GREEN else Color.WHITE)
        mTvAperture!!.setTextColor(if (newMode == DialMode.APERTURE) Color.GREEN else Color.WHITE)
        mTvISO!!.setTextColor(if (newMode == DialMode.ISO) Color.GREEN else Color.WHITE)
        mTvExposureCompensation!!.setTextColor(if (newMode == DialMode.EXPOSURE) Color.GREEN else Color.WHITE)
        mIvMode!!.setColorFilter(if (newMode == DialMode.MODE) Color.GREEN else Companion.PSUEDO_NULL_INT)
        mIvDriveMode!!.setColorFilter(if (newMode == DialMode.DRIVE) Color.GREEN else Companion.PSUEDO_NULL_INT)
        mIvTimelapse!!.setColorFilter(if (newMode == DialMode.TIMELAPSE) Color.GREEN else Companion.PSUEDO_NULL_INT)
        mIvBracket!!.setColorFilter(if (newMode == DialMode.BRACKET) Color.GREEN else Companion.PSUEDO_NULL_INT)
    }

    private fun movePreviewVertical(delta: Int) {
        var newY = mCurPreviewMagnificationPos.second + delta
        if (newY > mCurPreviewMagnificationMaxPos) newY = mCurPreviewMagnificationMaxPos else if (newY < -mCurPreviewMagnificationMaxPos) newY = -mCurPreviewMagnificationMaxPos
        mCurPreviewMagnificationPos = Pair(mCurPreviewMagnificationPos.first, newY)
        mCamera!!.setPreviewMagnification(mCurPreviewMagnification, mCurPreviewMagnificationPos)
    }

    private fun movePreviewHorizontal(delta: Int) {
        var newX = mCurPreviewMagnificationPos.first + delta
        if (newX > mCurPreviewMagnificationMaxPos) newX = mCurPreviewMagnificationMaxPos else if (newX < -mCurPreviewMagnificationMaxPos) newX = -mCurPreviewMagnificationMaxPos
        mCurPreviewMagnificationPos = Pair(newX, mCurPreviewMagnificationPos.second)
        mCamera!!.setPreviewMagnification(mCurPreviewMagnification, mCurPreviewMagnificationPos)
    }

    override fun onEnterKeyUp(): Boolean {
        return true
    }

    override fun onEnterKeyDown(): Boolean {
        /*
        Camera.Size s = m_camera.getNormalCamera().getParameters().getPreviewSize();
        if (s != null)
            log(String.format("previewSize width %d height %d\n", s.width, s.height));
        SurfaceView sv = (SurfaceView)findViewById(R.id.surfaceView);
        log(String.format("surfaceView width %d height %d left %d\n", sv.getWidth(), sv.getHeight(), sv.getLeft()));
        View v = findViewById(R.id.lRoot);
        log(String.format("root width %d height %d left %d\n", v.getWidth(), v.getHeight(), v.getLeft()));
        if (true)
            return true;
        */
        when {
            mTimelapseActive -> {
                abortTimelapse()
                return true
            }
            mBracketActive -> {
                abortBracketing()
                return true
            }
            mCurPreviewMagnification != 0 -> {
                mCurPreviewMagnificationPos = Pair(0, 0)
                mCamera!!.setPreviewMagnification(mCurPreviewMagnification, mCurPreviewMagnificationPos)
                return true
            }
            mDialMode == DialMode.ISO -> {
                // Toggle manual / automatic ISO
                setIso(if (mCurIso == 0) firstManualIso else 0)
                return true
            }
            mDialMode == DialMode.SHUTTER && mSceneMode == SceneMode.APERTURE -> {
                // Set minimum shutter speed
                startActivity(Intent(applicationContext, MinShutterActivity::class.java))
                return true
            }
            mDialMode == DialMode.EXPOSURE -> {
                // Reset exposure compensation
                setExposureCompensation(0, false)
                return true
            }
            mDialMode == DialMode.TIMELAPSE_SET_INTERVAL -> {
                setDialMode(DialMode.TIMELAPSE_SET_PIC_COUNT)
                mTvHint!!.text = "\uE4CD to set picture count, \uE04C to confirm"
                mTimelapsePicCount = 0
                updateTimelapsePictureCount()
                return true
            }
            mDialMode == DialMode.TIMELAPSE_SET_PIC_COUNT -> {
                startTimelapseCountdown()
                return true
            }
            mDialMode == DialMode.BRACKET_SET_STEP -> {
                setDialMode(DialMode.BRACKET_SET_PIC_COUNT)
                mTvHint!!.text = "\uE4CD to set picture count, \uE04C to confirm"
                calcMaxBracketPicCount()
                updateBracketPicCount()
                return true
            }
            mDialMode == DialMode.BRACKET_SET_PIC_COUNT -> {
                startBracketCountdown()
                return true
            }
            mDialMode == DialMode.MODE -> {
                toggleSceneMode()
                return true
            }
            mDialMode == DialMode.DRIVE -> {
                toggleDriveMode()
                return true
            }
            mDialMode == DialMode.TIMELAPSE -> {
                prepareTimelapse()
                return true
            }
            mDialMode == DialMode.BRACKET -> {
                prepareBracketing()
                return true
            }
            else -> return false
        }
    }

    override fun onUpKeyDown(): Boolean {
        return true
    }

    override fun onUpKeyUp(): Boolean {
        return if (mCurPreviewMagnification != 0) {
            movePreviewVertical((-500.0f / mCurPreviewMagnificationFactor).toInt())
            true
        } else {
            // Toggle visibility of some views
            cycleVisibleViews()
            true
        }
    }

    override fun onDownKeyDown(): Boolean {
        return true
    }

    override fun onDownKeyUp(): Boolean {
        return if (mCurPreviewMagnification != 0) {
            movePreviewVertical((500.0f / mCurPreviewMagnificationFactor).toInt())
            true
        } else {
            when (mDialMode) {
                DialMode.SHUTTER -> {
                    if (mHaveApertureControl) {
                        setDialMode(DialMode.APERTURE)
                    }
                }
                DialMode.APERTURE -> setDialMode(DialMode.ISO)
                DialMode.ISO -> setDialMode(DialMode.EXPOSURE)
                DialMode.EXPOSURE -> setDialMode(if (mHaveTouchscreen) DialMode.SHUTTER else DialMode.MODE)
                DialMode.MODE -> setDialMode(DialMode.DRIVE)
                DialMode.DRIVE -> setDialMode(DialMode.TIMELAPSE)
                DialMode.TIMELAPSE -> setDialMode(DialMode.BRACKET)
                DialMode.BRACKET -> setDialMode(DialMode.SHUTTER)
            }
            true
        }
    }

    override fun onLeftKeyDown(): Boolean {
        return true
    }

    override fun onLeftKeyUp(): Boolean {
        if (mCurPreviewMagnification != 0) {
            movePreviewHorizontal((-500.0f / mCurPreviewMagnificationFactor).toInt())
            return true
        }
        return false
    }

    override fun onRightKeyDown(): Boolean {
        return true
    }

    override fun onRightKeyUp(): Boolean {
        if (mCurPreviewMagnification != 0) {
            movePreviewHorizontal((500.0f / mCurPreviewMagnificationFactor).toInt())
            return true
        }
        return false
    }

    override fun onShutterKeyUp(): Boolean {
        mShutterKeyDown = false
        return true
    }

    override fun onShutterKeyDown(): Boolean {
        // direct shutter...
        /*
        log("onShutterKeyDown\n");
        if (!m_takingPicture)
        {
            m_takingPicture = true;
            m_shutterKeyDown = true;
            m_camera.burstableTakePicture();
        }
        */
        return true
    }

    override fun onDeleteKeyUp(): Boolean {
        // Exiting, make sure the app isn't restarted
        val intent = Intent("com.android.server.DAConnectionManagerService.AppInfoReceive")
        intent.putExtra(getString(R.string.package_name), componentName.packageName)
        intent.putExtra(getString(R.string.class_name), componentName.className)
        intent.putExtra(getString(R.string.pullingback_key), arrayOf<String>())
        intent.putExtra(getString(R.string.resume_key), arrayOf<String>())
        sendBroadcast(intent)
        onBackPressed()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val scanCode = event.scanCode
        if (mTimelapseActive && scanCode != ScalarInput.ISV_KEY_ENTER) return true
        // TODO: Use m_supportedPreviewMagnifications
        if (mDialMode != DialMode.TIMELAPSE_SET_INTERVAL && mDialMode != DialMode.TIMELAPSE_SET_PIC_COUNT) {
            if (scanCode == 610 && !mZoomLeverPressed) {
                // zoom lever tele
                mZoomLeverPressed = true
                if (mCurPreviewMagnification == 0) {
                    mCurPreviewMagnification = 100
                    mLFocusScale!!.visibility = View.GONE
                } else mCurPreviewMagnification = 200
                mCamera!!.setPreviewMagnification(mCurPreviewMagnification, mCurPreviewMagnificationPos)
                return true
            } else if (scanCode == 611 && !mZoomLeverPressed) {
                // zoom lever wide
                mZoomLeverPressed = true
                if (mCurPreviewMagnification == 200) {
                    mCurPreviewMagnification = 100
                    mCamera!!.setPreviewMagnification(mCurPreviewMagnification, mCurPreviewMagnificationPos)
                } else {
                    mCurPreviewMagnification = 0
                    mCamera!!.stopPreviewMagnification()
                }
                return true
            } else if (scanCode == 645) {
                // zoom lever returned to neutral position
                mZoomLeverPressed = false
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            val cam = mCamera!!.normalCamera
            cam.setPreviewDisplay(holder)
            cam.startPreview()
        } catch (e: IOException) {
            mTvMsg!!.text = getString(R.string.error_starting_preview)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        //log(String.format("surfaceChanged width %d height %d\n", width, height));
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}
    override fun setColorDepth(highQuality: Boolean) {
        super.setColorDepth(false)
    }

    companion object {
        private const val LOGGING_ENABLED = false
        private const val MESSAGE_TIMEOUT = 1000
        private const val COUNTDOWN_SECONDS = 5
        private const val VIEW_FLAG_GRID = 0x01
        private const val VIEW_FLAG_HISTOGRAM = 0x02
        private const val VIEW_FLAG_EXPOSURE = 0x04
        private const val VIEW_FLAG_MASK = 0x07 // all flags combined

        private const val PSUEDO_NULL_INT = -1;
    }
}