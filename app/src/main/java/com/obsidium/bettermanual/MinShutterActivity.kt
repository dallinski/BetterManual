package com.obsidium.bettermanual

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.github.ma1co.pmcademo.app.BaseActivity
import com.sony.scalar.hardware.CameraEx

class MinShutterActivity : BaseActivity(), OnSeekBarChangeListener {
    private var mSbshutter: SeekBar? = null
    private var mTvInfo: TextView? = null
    private var mCamera: CameraEx? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_min_shutter)
        mSbshutter = findViewById(R.id.sbShutter) as SeekBar
        mSbshutter!!.setOnSeekBarChangeListener(this)
        mSbshutter!!.max = CameraUtil.MIN_SHUTTER_VALUES.size - 1
        mTvInfo = findViewById(R.id.tvInfo) as TextView
        val btnOk = findViewById(R.id.btnOk) as Button
        btnOk.setOnClickListener { onBackPressed() }
        title = "Minimum Shutter Speed"
    }

    public override fun onResume() {
        super.onResume()
        mCamera = CameraEx.open(0, null)
        mCamera?.setShutterSpeedChangeListener { shutterSpeedInfo, cameraEx ->
            val idx = CameraUtil.getShutterValueIndex(shutterSpeedInfo.currentAvailableMin_n, shutterSpeedInfo.currentAvailableMin_d)
            if (idx >= 0) {
                mSbshutter!!.progress = idx
                mTvInfo!!.text = CameraUtil.formatShutterSpeed(shutterSpeedInfo.currentAvailableMin_n, shutterSpeedInfo.currentAvailableMin_d)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Save minimum shutter speed
        val prefs = Preferences(this)
        val paramsModifier = mCamera!!.createParametersModifier(mCamera!!.normalCamera.parameters)
        prefs.minShutterSpeed = paramsModifier.autoShutterSpeedLowLimit
        mCamera!!.release()
        mCamera = null
    }

    override fun onEnterKeyDown(): Boolean {
        onBackPressed()
        return true
    }

    override fun onUpperDialChanged(value: Int): Boolean {
        mSbshutter!!.incrementProgressBy(value)
        val params = mCamera!!.createEmptyParameters()
        mCamera!!.createParametersModifier(params).autoShutterSpeedLowLimit = CameraUtil.MIN_SHUTTER_VALUES[mSbshutter!!.progress]
        mCamera!!.normalCamera.parameters = params
        return true
    }

    /* OnSeekBarChangeListener */
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            val params = mCamera!!.createEmptyParameters()
            mCamera!!.createParametersModifier(params).autoShutterSpeedLowLimit = CameraUtil.MIN_SHUTTER_VALUES[progress]
            mCamera!!.normalCamera.parameters = params
        }
    }

    override fun onStartTrackingTouch(var1: SeekBar) {}
    override fun onStopTrackingTouch(var1: SeekBar) {}
    override fun setColorDepth(highQuality: Boolean) {
        super.setColorDepth(false)
    }
}