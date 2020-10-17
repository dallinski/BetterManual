package com.obsidium.bettermanual

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FocusScaleView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val mPaint = Paint()
    private var mMax = 0
    private var mMin = 0
    private var mCur = 0
    fun setMaxPosition(max: Int) {
        if (max > 0) mMax = max
        invalidate()
    }

    fun setMinPosition(min: Int) {
        if (min < mMax) mMin = min
        invalidate()
    }

    fun setCurPosition(pos: Int) {
        if (pos in 0..mMax) mCur = pos
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        canvas.drawARGB(0, 0, 0, 0)
        if (mMax != 0) {
            val w = width.toFloat()
            val h = height.toFloat()
            // Draw frame
            mPaint.setARGB(100, 255, 255, 255)
            mPaint.style = Paint.Style.STROKE
            canvas.drawRect(0f, 0f, w, h, mPaint)
            // Draw bar
            val w2 = w - BORDER_WIDTH * 2
            if (mCur > mMin) mPaint.setARGB(150, 0, 255, 0) else mPaint.setARGB(150, 255, 0, 0)
            mPaint.style = Paint.Style.FILL
            canvas.drawRect(BORDER_WIDTH.toFloat(), BORDER_WIDTH.toFloat(), BORDER_WIDTH + mCur.toFloat() / mMax.toFloat() * w2, h - BORDER_WIDTH, mPaint)
        }
    }

    companion object {
        private const val BORDER_WIDTH = 2
    }

    init {
        mPaint.isAntiAlias = false
        mPaint.strokeWidth = BORDER_WIDTH.toFloat()
    }
}