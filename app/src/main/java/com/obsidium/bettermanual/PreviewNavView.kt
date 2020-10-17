package com.obsidium.bettermanual

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Pair
import android.view.View

class PreviewNavView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var mPosFract: Pair<Float, Float>? = null
    private var mZoomFactor = 0f
    private val mPaint = Paint()
    fun update(position: Pair<Int, Int>?, zoomFactor: Float) {
        mZoomFactor = zoomFactor
        mPosFract = if (position != null) Pair(position.first.toFloat() / 1000.0f, position.second.toFloat() / 1000.0f) else null
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        // solid black background
        canvas.drawARGB(255, 0, 0, 0)
        if (mPosFract != null && mZoomFactor != 0.0f) {
            val w = width.toFloat()
            val h = height.toFloat()
            // white outer frame
            mPaint.setARGB(255, 255, 255, 255)
            canvas.drawRect(0f, 0f, w - STROKE_WIDTH, h - STROKE_WIDTH, mPaint)

            // 0, 0 is the center
            val centerX = w / 2.0f
            val centerY = h / 2.0f
            val curCenterX = centerX + mPosFract!!.first * centerX
            val curCenterY = centerY + mPosFract!!.second * centerY
            val w2 = w / (mZoomFactor * 2.0f)
            val h2 = h / (mZoomFactor * 2.0f)

            // red position rectangle
            mPaint.setARGB(255, 255, 0, 0)
            canvas.drawRect(curCenterX - w2, curCenterY - h2, curCenterX + w2, curCenterY + h2, mPaint)
        }
    }

    companion object {
        private const val STROKE_WIDTH = 2.0f
    }

    init {
        mPaint.isAntiAlias = false
        mPaint.strokeWidth = STROKE_WIDTH
        mPaint.style = Paint.Style.STROKE
    }
}