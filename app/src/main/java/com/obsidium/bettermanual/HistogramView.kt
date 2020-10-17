/*
 * Modified code, original license:
 *
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.obsidium.bettermanual

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil

class HistogramView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val mPaint = Paint()
    private val mPath = Path()
    private var mHistogram: ShortArray? = null
    fun setHistogram(histogram: ShortArray?) {
        mHistogram = histogram
        invalidate()
    }

    private fun drawHistogram(canvas: Canvas, histogram: ShortArray) {
        var max: Short = 0
        for (value in histogram) {
            if (value > max) max = value
        }
        val w = width.toFloat()
        val h = height.toFloat()
        val dx = 0f
        val wl = w / histogram.size
        val wh = h / max
        mPaint.reset()
        mPaint.isAntiAlias = false
        mPaint.setARGB(100, 255, 255, 255)
        mPaint.strokeWidth = ceil(wl.toDouble()).toFloat()
        mPaint.style = Paint.Style.STROKE
        canvas.drawRect(dx, 0f, dx + w - wl, h - wl, mPaint)
        canvas.drawLine(dx + w / 3, 1f, dx + w / 3, h - 1, mPaint)
        canvas.drawLine(dx + 2 * w / 3, 1f, dx + 2 * w / 3, h - 1, mPaint)
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = 6f
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        mPath.reset()
        mPath.moveTo(dx, h)
        var firstPointEncountered = false
        var prev = 0f
        var last = 0f
        for (i in histogram.indices) {
            val x = i * wl + dx
            val l = histogram[i] * wh
            if (l != 0f) {
                val v = h - (l + prev) / 2.0f
                if (!firstPointEncountered) {
                    mPath.lineTo(x, h)
                    firstPointEncountered = true
                }
                mPath.lineTo(x, v)
                prev = l
                last = x
            }
        }
        mPath.lineTo(last, h)
        mPath.lineTo(w, h)
        mPath.close()
        mPaint.setARGB(255, 255, 255, 255)
        canvas.drawPath(mPath, mPaint)
        /*
        m_paint.setStrokeWidth(2);
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setARGB(255, 200, 200, 200);
        canvas.drawPath(m_path, m_paint);
        */
    }

    public override fun onDraw(canvas: Canvas) {
        canvas.drawARGB(50, 0, 0, 0)
        if (mHistogram != null) drawHistogram(canvas, mHistogram!!)
    }
}