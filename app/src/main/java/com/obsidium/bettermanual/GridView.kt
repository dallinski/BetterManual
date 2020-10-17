package com.obsidium.bettermanual

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import com.sony.scalar.hardware.avio.DisplayManager.VideoRect

class GridView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val mPaint = Paint()
    private var mVideoRect: VideoRect? = null
    fun setVideoRect(videoRect: VideoRect?) {
        mVideoRect = videoRect
    }

    public override fun onDraw(canvas: Canvas) {
        canvas.drawARGB(0, 0, 0, 0)
        if (mVideoRect != null) {
            val w = width.toFloat()
            val h = height.toFloat()
            val w3 = (mVideoRect!!.pxRight - mVideoRect!!.pxLeft).toFloat() / 3.0f
            val h3 = h / 3.0f

            // Vertical lines
            canvas.drawLine(mVideoRect!!.pxLeft + w3, 0f, mVideoRect!!.pxLeft + w3, h, mPaint)
            canvas.drawLine(mVideoRect!!.pxLeft + w3 * 2, 0f, mVideoRect!!.pxLeft + w3 * 2, h, mPaint)

            // Horizontal lines
            canvas.drawLine(mVideoRect!!.pxLeft.toFloat(), h3, w - mVideoRect!!.pxLeft, h3, mPaint)
            canvas.drawLine(mVideoRect!!.pxLeft.toFloat(), h3 * 2, w - mVideoRect!!.pxLeft, h3 * 2, mPaint)
        }
    }

    init {
        mPaint.isAntiAlias = false
        mPaint.setARGB(100, 100, 100, 100)
        mPaint.strokeWidth = 2f
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
}