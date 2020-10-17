package com.obsidium.bettermanual

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

open class OnSwipeTouchListener(context: Context?) : OnTouchListener {
    private val mGestureDetector: GestureDetector
    fun onSwipeLeft() {}
    fun onSwipeRight() {}
    open fun onClick(): Boolean {
        return false
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event)
    }

    open fun onScrolled(distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        private val SWIPE_DISTANCE_THRESHOLD = 50
        private val SWIPE_VELOCITY_THRESHOLD = 50

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            return onScrolled(distanceX, distanceY)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return onClick()
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val distanceX = e2.x - e1.x
            val distanceY = e2.y - e1.y
            if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceX > 0) onSwipeRight() else onSwipeLeft()
                return true
            }
            return false
        }
    }

    init {
        mGestureDetector = GestureDetector(context, GestureListener())
    }
}