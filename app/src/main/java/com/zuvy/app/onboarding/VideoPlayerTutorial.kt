package com.zuvy.app.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.edit
import com.zuvy.app.R

/**
 * VideoPlayerTutorial - Interactive tutorial overlay for video player features
 * 
 * Features:
 * - Spotlight/highlight effect on controls
 * - Animated tooltips
 * - Step-by-step guidance
 * - Gesture demonstrations
 * - Skip/replay options
 */
class VideoPlayerTutorial(
    private val context: Context,
    private val rootView: ViewGroup
) {
    companion object {
        const val PREF_NAME = "video_player_tutorial"
        const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
        const val KEY_TUTORIAL_STEP = "tutorial_step"

        // Tutorial steps
        const val STEP_GESTURE_VOLUME = 0
        const val STEP_GESTURE_BRIGHTNESS = 1
        const val STEP_GESTURE_SEEK = 2
        const val STEP_DOUBLE_TAP_SEEK = 3
        const val STEP_PINCH_ZOOM = 4
        const val STEP_LOCK_SCREEN = 5
        const val STEP_PLAYBACK_SPEED = 6
        const val STEP_SUBTITLES = 7
        const val STEP_ROTATION = 8
        const val STEP_MORE_OPTIONS = 9
    }

    private var overlayView: View? = null
    private var currentStep = 0
    private var isShowing = false
    private var spotlightView: View? = null
    private var tooltipPopup: PopupWindow? = null

    private val tutorialSteps = listOf(
        TutorialStep(
            stepId = STEP_GESTURE_VOLUME,
            title = "Volume Control",
            description = "Swipe up/down on the right side of the screen to adjust volume",
            gestureHint = "👆 Swipe vertically on right side",
            highlightPosition = HighlightPosition.RIGHT
        ),
        TutorialStep(
            stepId = STEP_GESTURE_BRIGHTNESS,
            title = "Brightness Control",
            description = "Swipe up/down on the left side of the screen to adjust brightness",
            gestureHint = "👆 Swipe vertically on left side",
            highlightPosition = HighlightPosition.LEFT
        ),
        TutorialStep(
            stepId = STEP_GESTURE_SEEK,
            title = "Seek Control",
            description = "Swipe left/right anywhere to seek through the video",
            gestureHint = "👈👉 Swipe horizontally",
            highlightPosition = HighlightPosition.CENTER
        ),
        TutorialStep(
            stepId = STEP_DOUBLE_TAP_SEEK,
            title = "Double Tap to Seek",
            description = "Double-tap left side to rewind 10s, right side to forward 10s",
            gestureHint = "👆👆 Double-tap sides",
            highlightPosition = HighlightPosition.FULL
        ),
        TutorialStep(
            stepId = STEP_PINCH_ZOOM,
            title = "Pinch to Zoom",
            description = "Pinch with two fingers to zoom in/out on the video",
            gestureHint = "✌️ Pinch gesture",
            highlightPosition = HighlightPosition.CENTER
        ),
        TutorialStep(
            stepId = STEP_LOCK_SCREEN,
            title = "Lock Screen",
            description = "Tap the lock icon to disable all touches while watching",
            gestureHint = "🔒 Tap lock button",
            highlightPosition = HighlightPosition.TOP_LEFT
        ),
        TutorialStep(
            stepId = STEP_PLAYBACK_SPEED,
            title = "Playback Speed",
            description = "Change playback speed from 0.25x to 4x for faster or slower viewing",
            gestureHint = "⚡ Tap speed button",
            highlightPosition = HighlightPosition.TOP_RIGHT
        ),
        TutorialStep(
            stepId = STEP_SUBTITLES,
            title = "Subtitles",
            description = "Enable subtitles and choose from available languages",
            gestureHint = "💬 Tap subtitle button",
            highlightPosition = HighlightPosition.TOP_RIGHT
        ),
        TutorialStep(
            stepId = STEP_ROTATION,
            title = "Screen Rotation",
            description = "Tap rotate button to switch between portrait and landscape",
            gestureHint = "🔄 Tap rotate button",
            highlightPosition = HighlightPosition.TOP_RIGHT
        ),
        TutorialStep(
            stepId = STEP_MORE_OPTIONS,
            title = "More Options",
            description = "Access equalizer, sleep timer, filters, and more advanced features",
            gestureHint = "⚙️ Tap more button",
            highlightPosition = HighlightPosition.TOP_RIGHT
        )
    )

    /**
     * Check if tutorial should be shown
     */
    fun shouldShowTutorial(): Boolean {
        return !context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TUTORIAL_COMPLETED, false)
    }

    /**
     * Start the tutorial
     */
    fun start() {
        if (isShowing) return
        
        isShowing = true
        currentStep = 0
        createOverlay()
        showStep(currentStep)
    }

    /**
     * Start from a specific step
     */
    fun startFromStep(step: Int) {
        if (isShowing) return
        
        isShowing = true
        currentStep = step
        createOverlay()
        showStep(currentStep)
    }

    /**
     * Create the dimmed overlay
     */
    private fun createOverlay() {
        overlayView = View(context).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            isClickable = true
            isFocusable = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    // Handle tap on highlighted area or next button
                    true
                } else {
                    false
                }
            }
        }
        
        rootView.addView(overlayView)
    }

    /**
     * Show a specific tutorial step
     */
    private fun showStep(stepIndex: Int) {
        if (stepIndex >= tutorialSteps.size) {
            complete()
            return
        }

        val step = tutorialSteps[stepIndex]
        showSpotlight(step)
        showTooltip(step)
        animateStepTransition()
    }

    /**
     * Show spotlight/highlight for a step
     */
    private fun showSpotlight(step: TutorialStep) {
        // Remove previous spotlight
        spotlightView?.let { rootView.removeView(it) }
        
        // Create spotlight view based on highlight position
        spotlightView = createSpotlightView(step.highlightPosition)
        spotlightView?.let {
            rootView.addView(it)
            animateSpotlightEntrance(it)
        }
    }

    /**
     * Create spotlight view for different positions
     */
    private fun createSpotlightView(position: HighlightPosition): View {
        return View(context).apply {
            val size = when (position) {
                HighlightPosition.LEFT, HighlightPosition.RIGHT -> 150
                HighlightPosition.CENTER -> 200
                HighlightPosition.FULL -> 300
                HighlightPosition.TOP_LEFT, HighlightPosition.TOP_RIGHT -> 100
            }
            
            val (x, y) = when (position) {
                HighlightPosition.LEFT -> 50 to rootView.height / 2 - size / 2
                HighlightPosition.RIGHT -> rootView.width - size - 50 to rootView.height / 2 - size / 2
                HighlightPosition.CENTER -> rootView.width / 2 - size / 2 to rootView.height / 2 - size / 2
                HighlightPosition.FULL -> rootView.width / 2 - size / 2 to rootView.height / 2 - size / 2
                HighlightPosition.TOP_LEFT -> 50 to 150
                HighlightPosition.TOP_RIGHT -> rootView.width - size - 50 to 150
            }
            
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                this.leftMargin = x
                this.topMargin = y
            }
            
            // Create circular spotlight effect
            setBackgroundResource(R.drawable.bg_spotlight)
            alpha = 0f
        }
    }

    /**
     * Show tooltip for current step
     */
    private fun showTooltip(step: TutorialStep) {
        tooltipPopup?.dismiss()
        
        val tooltipView = createTooltipView(step)
        tooltipPopup = PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            isOutsideTouchable = false
            isFocusable = false
            setBackgroundDrawable(null)
            
            // Calculate position based on highlight
            val (x, y) = calculateTooltipPosition(step.highlightPosition)
            showAtLocation(rootView, Gravity.NO_GRAVITY, x, y)
        }
        
        animateTooltipEntrance(tooltipView)
    }

    /**
     * Create tooltip view
     */
    private fun createTooltipView(step: TutorialStep): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundResource(R.drawable.bg_tooltip)
            
            val outValue = android.util.TypedValue().apply {
                context.resources.getValue(android.R.dimen.tooltip_precise_anchor_threshold, this, true)
            }
        }
        
        // Step indicator
        val stepIndicator = TextView(context).apply {
            text = "Step ${step.stepId + 1}/${tutorialSteps.size}"
            setTextColor(context.getColor(R.color.primary))
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        container.addView(stepIndicator)
        
        // Title
        val titleView = TextView(context).apply {
            text = step.title
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 0)
        }
        container.addView(titleView)
        
        // Description
        val descView = TextView(context).apply {
            text = step.description
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 14f
            setPadding(0, 8, 0, 12)
        }
        container.addView(descView)
        
        // Gesture hint
        val gestureView = TextView(context).apply {
            text = step.gestureHint
            setTextColor(context.getColor(R.color.accent))
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        container.addView(gestureView)
        
        // Button row
        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Skip button
        val skipBtn = TextView(context).apply {
            text = "Skip"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 13f
            setPadding(16, 8, 16, 8)
            setOnClickListener { skip() }
        }
        buttonRow.addView(skipBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        // Next button
        val nextBtn = TextView(context).apply {
            text = if (currentStep == tutorialSteps.size - 1) "Got it!" else "Next"
            setTextColor(context.getColor(R.color.primary))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16, 8, 16, 8)
            setOnClickListener { nextStep() }
        }
        buttonRow.addView(nextBtn)
        
        container.addView(buttonRow)
        
        return container
    }

    /**
     * Calculate tooltip position based on highlight position
     */
    private fun calculateTooltipPosition(position: HighlightPosition): Pair<Int, Int> {
        val screenWidth = rootView.width
        val screenHeight = rootView.height
        
        return when (position) {
            HighlightPosition.LEFT -> screenWidth / 4 to screenHeight / 2 + 150
            HighlightPosition.RIGHT -> screenWidth * 3 / 4 - 200 to screenHeight / 2 + 150
            HighlightPosition.CENTER -> screenWidth / 2 - 150 to screenHeight / 2 + 150
            HighlightPosition.FULL -> screenWidth / 2 - 150 to screenHeight * 2 / 3
            HighlightPosition.TOP_LEFT -> 50 to 300
            HighlightPosition.TOP_RIGHT -> screenWidth - 350 to 300
        }
    }

    /**
     * Animate spotlight entrance
     */
    private fun animateSpotlightEntrance(view: View) {
        view.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    /**
     * Animate tooltip entrance
     */
    private fun animateTooltipEntrance(view: View) {
        view.apply {
            alpha = 0f
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    /**
     * Animate step transition
     */
    private fun animateStepTransition() {
        overlayView?.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    /**
     * Go to next step
     */
    fun nextStep() {
        currentStep++
        if (currentStep >= tutorialSteps.size) {
            complete()
        } else {
            showStep(currentStep)
        }
    }

    /**
     * Skip tutorial
     */
    fun skip() {
        complete()
    }

    /**
     * Complete tutorial
     */
    fun complete() {
        isShowing = false
        
        // Save completion
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TUTORIAL_COMPLETED, true)
            .apply()
        
        // Animate out
        overlayView?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.withEndAction {
                cleanup()
            }
            ?.start()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        tooltipPopup?.dismiss()
        tooltipPopup = null
        
        spotlightView?.let { rootView.removeView(it) }
        spotlightView = null
        
        overlayView?.let { rootView.removeView(it) }
        overlayView = null
        
        isShowing = false
    }

    /**
     * Reset tutorial (for testing)
     */
    fun reset() {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TUTORIAL_COMPLETED, false)
            .putInt(KEY_TUTORIAL_STEP, 0)
            .apply()
    }

    // ============================================
    // DATA CLASSES
    // ============================================

    data class TutorialStep(
        val stepId: Int,
        val title: String,
        val description: String,
        val gestureHint: String,
        val highlightPosition: HighlightPosition
    )

    enum class HighlightPosition {
        LEFT, RIGHT, CENTER, FULL, TOP_LEFT, TOP_RIGHT
    }
}
