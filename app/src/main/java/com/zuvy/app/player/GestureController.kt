package com.zuvy.app.player

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs

/**
 * Advanced gesture handler for video player
 * Handles all touch gestures: tap, double tap, long press, swipe, pinch
 */
class GestureController(
    private val context: Context,
    private val callback: GestureCallback
) {
    interface GestureCallback {
        fun onSingleTap()
        fun onDoubleTapLeft()
        fun onDoubleTapCenter()
        fun onDoubleTapRight()
        fun onLongPressStart()
        fun onLongPressEnd()
        fun onBrightnessChange(delta: Float)
        fun onVolumeChange(delta: Float)
        fun onSeekStart()
        fun onSeekChange(delta: Long)
        fun onSeekEnd()
        fun onZoom(scale: Float)
        fun onSwipeUp()
        fun onSwipeDown()
    }
    
    private var viewWidth = 0
    private var viewHeight = 0
    
    // Gesture state
    private var isGestureInProgress = false
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var isBrightnessGesture = false
    private var isVolumeGesture = false
    private var isSeekGesture = false
    private var isLongPressing = false
    
    // Tap detection
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private val doubleTapTimeout = 300L
    
    // Swipe thresholds
    private val swipeThreshold = 50f
    private val seekMultiplier = 60000L // Max 60 seconds swipe
    
    private val tapGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            callback.onSingleTap()
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val zone = when {
                e.x < viewWidth / 3 -> "left"
                e.x > viewWidth * 2 / 3 -> "right"
                else -> "center"
            }
            
            when (zone) {
                "left" -> callback.onDoubleTapLeft()
                "right" -> callback.onDoubleTapRight()
                "center" -> callback.onDoubleTapCenter()
            }
            return true
        }
        
        override fun onLongPress(e: MotionEvent) {
            isLongPressing = true
            callback.onLongPressStart()
        }
    })
    
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            callback.onZoom(detector.scaleFactor)
            return true
        }
    })
    
    fun onTouch(view: View, event: MotionEvent): Boolean {
        viewWidth = view.width
        viewHeight = view.height
        
        // Handle scale gestures
        scaleGestureDetector.onTouchEvent(event)
        
        // Handle tap gestures
        tapGestureDetector.onTouchEvent(event)
        
        // Handle swipe gestures
        handleSwipeGesture(event)
        
        return true
    }
    
    private fun handleSwipeGesture(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = event.x
                gestureStartY = event.y
                isBrightnessGesture = false
                isVolumeGesture = false
                isSeekGesture = false
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isLongPressing) return
                
                val deltaX = event.x - gestureStartX
                val deltaY = event.y - gestureStartY
                
                // Determine gesture type on first significant movement
                if (!isBrightnessGesture && !isVolumeGesture && !isSeekGesture) {
                    when {
                        abs(deltaY) > swipeThreshold && gestureStartX < viewWidth / 3 -> {
                            isBrightnessGesture = true
                        }
                        abs(deltaY) > swipeThreshold && gestureStartX > viewWidth * 2 / 3 -> {
                            isVolumeGesture = true
                        }
                        abs(deltaX) > swipeThreshold -> {
                            isSeekGesture = true
                            callback.onSeekStart()
                        }
                        deltaY > swipeThreshold -> {
                            callback.onSwipeDown()
                        }
                        deltaY < -swipeThreshold -> {
                            callback.onSwipeUp()
                        }
                    }
                }
                
                // Handle active gesture
                when {
                    isBrightnessGesture -> {
                        val progress = -deltaY / viewHeight
                        callback.onBrightnessChange(progress)
                    }
                    isVolumeGesture -> {
                        val progress = -deltaY / viewHeight
                        callback.onVolumeChange(progress)
                    }
                    isSeekGesture -> {
                        val seekAmount = (deltaX / viewWidth * seekMultiplier).toLong()
                        callback.onSeekChange(seekAmount)
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isLongPressing) {
                    isLongPressing = false
                    callback.onLongPressEnd()
                }
                
                if (isSeekGesture) {
                    callback.onSeekEnd()
                }
                
                isBrightnessGesture = false
                isVolumeGesture = false
                isSeekGesture = false
            }
        }
    }
    
    fun isGestureInProgress(): Boolean = isGestureInProgress
}
