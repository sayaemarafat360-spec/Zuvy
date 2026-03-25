package com.zuvy.app.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.zuvy.app.R
import com.zuvy.app.databinding.ViewTutorialTooltipBinding

/**
 * TutorialManager - Interactive animated tutorial system for video player
 * 
 * Features:
 * - Step-by-step guided tutorials
 * - Animated highlights and tooltips
 * - Interactive touch hints
 * - Gesture demonstrations
 * - Progress tracking
 */
class TutorialManager(private val context: Context) {

    private var currentStep = 0
    private var tutorialSteps: List<TutorialStep> = emptyList()
    private var overlayView: FrameLayout? = null
    private var highlightView: View? = null
    private var tooltipWindow: PopupWindow? = null
    private var onTutorialCompleteListener: (() -> Unit)? = null
    private var onStepListener: ((Int, TutorialStep) -> Unit)? = null

    // SharedPreferences for persistence
    private val prefs = context.getSharedPreferences(TUTORIAL_PREFS, Context.MODE_PRIVATE)

    companion object {
        const val TUTORIAL_PREFS = "tutorial_prefs"
        const val KEY_VIDEO_TUTORIAL_STEP = "video_tutorial_step"
        const val KEY_TUTORIAL_SKIPPED = "tutorial_skipped"

        // Tutorial step keys
        const val TUTORIAL_PLAY_PAUSE = "play_pause"
        const val TUTORIAL_SEEK = "seek"
        const val TUTORIAL_DOUBLE_TAP = "double_tap"
        const val TUTORIAL_SWIPE_BRIGHTNESS = "swipe_brightness"
        const val TUTORIAL_SWIPE_VOLUME = "swipe_volume"
        const val TUTORIAL_PINCH_ZOOM = "pinch_zoom"
        const val TUTORIAL_LOCK = "lock"
        const val TUTORIAL_SETTINGS = "settings"
        const val TUTORIAL_GESTURES = "gestures"
    }

    /**
     * Check if tutorial is needed
     */
    fun needsTutorial(): Boolean {
        return !prefs.getBoolean(KEY_TUTORIAL_SKIPPED, false) &&
                prefs.getInt(KEY_VIDEO_TUTORIAL_STEP, 0) < getTotalSteps()
    }

    /**
     * Get current tutorial step
     */
    fun getCurrentStep(): Int = prefs.getInt(KEY_VIDEO_TUTORIAL_STEP, 0)

    /**
     * Get total steps
     */
    fun getTotalSteps(): Int = tutorialSteps.size

    /**
     * Set tutorial steps
     */
    fun setTutorialSteps(steps: List<TutorialStep>): TutorialManager {
        this.tutorialSteps = steps
        return this
    }

    /**
     * Set completion listener
     */
    fun setOnTutorialCompleteListener(listener: () -> Unit): TutorialManager {
        this.onTutorialCompleteListener = listener
        return this
    }

    /**
     * Set step listener
     */
    fun setOnStepListener(listener: (Int, TutorialStep) -> Unit): TutorialManager {
        this.onStepListener = listener
        return this
    }

    /**
     * Start the tutorial
     */
    fun start(rootView: ViewGroup) {
        currentStep = getCurrentStep()
        if (currentStep >= tutorialSteps.size) {
            onTutorialCompleteListener?.invoke()
            return
        }

        createOverlay(rootView)
        showStep(currentStep)
    }

    /**
     * Create overlay for tutorial
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlay(rootView: ViewGroup) {
        // Create overlay frame layout
        overlayView = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
        }

        rootView.addView(overlayView)

        // Create highlight view
        highlightView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(0, 0)
            background = createHighlightDrawable()
            visibility = View.GONE
        }
        overlayView?.addView(highlightView)

        // Touch listener for overlay
        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Check if touch is on highlighted area
                    val step = tutorialSteps.getOrNull(currentStep) ?: return@setOnTouchListener false
                    if (step.interactive) {
                        // Let the touch pass through to the target
                        false
                    } else {
                        // Consume touch on overlay
                        true
                    }
                }
                else -> false
            }
        }
    }

    /**
     * Show a specific tutorial step
     */
    private fun showStep(stepIndex: Int) {
        if (stepIndex >= tutorialSteps.size) {
            completeTutorial()
            return
        }

        val step = tutorialSteps[stepIndex]
        currentStep = stepIndex

        // Save progress
        prefs.edit().putInt(KEY_VIDEO_TUTORIAL_STEP, stepIndex).apply()

        // Notify listener
        onStepListener?.invoke(stepIndex, step)

        // Highlight target view
        highlightView(step.targetView, step.highlightShape)

        // Show tooltip
        showTooltip(step)

        // Start animations
        animateStep(step)
    }

    /**
     * Highlight a target view
     */
    private fun highlightView(targetView: View?, shape: HighlightShape) {
        if (targetView == null || overlayView == null) return

        // Get target location on screen
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)

        val width = targetView.width
        val height = targetView.height

        // Calculate padding for highlight
        val padding = context.resources.getDimensionPixelSize(R.dimen.tutorial_highlight_padding)

        // Update highlight view
        highlightView?.apply {
            val lp = layoutParams as FrameLayout.LayoutParams
            lp.width = width + (padding * 2)
            lp.height = height + (padding * 2)
            lp.leftMargin = location[0] - padding
            lp.topMargin = location[1] - padding
            layoutParams = lp

            (background as? GradientDrawable)?.cornerRadii = when (shape) {
                HighlightShape.CIRCLE -> floatArrayOf(
                    width / 2f, width / 2f,
                    width / 2f, width / 2f,
                    width / 2f, width / 2f,
                    width / 2f, width / 2f
                )
                HighlightShape.ROUNDED_RECT -> floatArrayOf(
                    16f, 16f, 16f, 16f,
                    16f, 16f, 16f, 16f
                )
                HighlightShape.RECTANGLE -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            }

            visibility = View.VISIBLE
        }
    }

    /**
     * Show tooltip for current step
     */
    private fun showTooltip(step: TutorialStep) {
        // Dismiss existing tooltip
        tooltipWindow?.dismiss()

        // Create tooltip view
        val binding = ViewTutorialTooltipBinding.inflate(LayoutInflater.from(context))

        binding.titleText.text = step.title
        binding.descriptionText.text = step.description
        binding.stepIndicator.text = "${currentStep + 1}/${tutorialSteps.size}"

        binding.nextButton.text = if (currentStep == tutorialSteps.size - 1) "Done" else "Next"
        binding.nextButton.setOnClickListener {
            nextStep()
        }

        binding.skipButton.setOnClickListener {
            skipTutorial()
        }

        // Create popup window
        tooltipWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = false
            isFocusable = false

            // Show tooltip at position
            val targetView = step.targetView ?: return
            val location = IntArray(2)
            targetView.getLocationOnScreen(location)

            val offsetX = when (step.tooltipPosition) {
                TooltipPosition.TOP, TooltipPosition.BOTTOM -> targetView.width / 2 - 150
                TooltipPosition.START -> -350
                TooltipPosition.END -> targetView.width + 20
                TooltipPosition.CENTER -> 0
            }

            val offsetY = when (step.tooltipPosition) {
                TooltipPosition.TOP -> -300
                TooltipPosition.BOTTOM -> targetView.height + 20
                TooltipPosition.START, TooltipPosition.END -> -50
                TooltipPosition.CENTER -> 0
            }

            showAtLocation(targetView, Gravity.NO_GRAVITY, location[0] + offsetX, location[1] + offsetY)
        }

        // Animate tooltip entrance
        binding.root.alpha = 0f
        binding.root.translationY = 20f
        binding.root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    /**
     * Animate the current step
     */
    private fun animateStep(step: TutorialStep) {
        // Pulse animation on highlight
        highlightView?.let { view ->
            val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f)
            val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f)
            val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.1f, 1f)
            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.1f, 1f)

            AnimatorSet().apply {
                playSequentially(
                    AnimatorSet().apply { playTogether(scaleUpX, scaleUpY) },
                    AnimatorSet().apply { playTogether(scaleDownX, scaleDownY) }
                )
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }

        // Show gesture hint if interactive
        if (step.showGestureHint) {
            showGestureHint(step.gestureType)
        }
    }

    /**
     * Show gesture hint animation
     */
    private fun showGestureHint(gestureType: GestureType) {
        // This would show an animated hand performing the gesture
        // For now, we'll use a simple indicator
        when (gestureType) {
            GestureType.TAP -> {
                // Show tap animation
            }
            GestureType.DOUBLE_TAP -> {
                // Show double tap animation
            }
            GestureType.SWIPE_VERTICAL -> {
                // Show vertical swipe animation
            }
            GestureType.SWIPE_HORIZONTAL -> {
                // Show horizontal swipe animation
            }
            GestureType.PINCH -> {
                // Show pinch animation
            }
            GestureType.LONG_PRESS -> {
                // Show long press animation
            }
        }
    }

    /**
     * Go to next step
     */
    fun nextStep() {
        currentStep++
        if (currentStep >= tutorialSteps.size) {
            completeTutorial()
        } else {
            showStep(currentStep)
        }
    }

    /**
     * Go to previous step
     */
    fun previousStep() {
        if (currentStep > 0) {
            showStep(--currentStep)
        }
    }

    /**
     * Skip tutorial
     */
    fun skipTutorial() {
        prefs.edit()
            .putBoolean(KEY_TUTORIAL_SKIPPED, true)
            .putInt(KEY_VIDEO_TUTORIAL_STEP, tutorialSteps.size)
            .apply()
        completeTutorial()
    }

    /**
     * Complete tutorial
     */
    private fun completeTutorial() {
        prefs.edit()
            .putInt(KEY_VIDEO_TUTORIAL_STEP, tutorialSteps.size)
            .apply()

        dismiss()
        onTutorialCompleteListener?.invoke()
    }

    /**
     * Dismiss tutorial
     */
    fun dismiss() {
        tooltipWindow?.dismiss()
        highlightView?.visibility = View.GONE
        overlayView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        overlayView = null
    }

    /**
     * Create highlight drawable
     */
    private fun createHighlightDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.primary))
            alpha = 80
        }
    }

    /**
     * Create default video player tutorial steps
     */
    fun createDefaultVideoPlayerTutorial(
        playPauseButton: View?,
        seekBar: View?,
        brightnessArea: View?,
        volumeArea: View?,
        lockButton: View?,
        settingsButton: View?,
        doubleTapLeft: View?,
        doubleTapRight: View?
    ): List<TutorialStep> {
        return listOf(
            TutorialStep(
                key = TUTORIAL_PLAY_PAUSE,
                targetView = playPauseButton,
                title = "Play & Pause",
                description = "Tap here to play or pause the video",
                tooltipPosition = TooltipPosition.BOTTOM,
                highlightShape = HighlightShape.CIRCLE,
                interactive = true,
                showGestureHint = true,
                gestureType = GestureType.TAP
            ),
            TutorialStep(
                key = TUTORIAL_SEEK,
                targetView = seekBar,
                title = "Seek Through Video",
                description = "Drag to seek to any position in the video",
                tooltipPosition = TooltipPosition.TOP,
                highlightShape = HighlightShape.ROUNDED_RECT,
                interactive = true,
                showGestureHint = true,
                gestureType = GestureType.SWIPE_HORIZONTAL
            ),
            TutorialStep(
                key = TUTORIAL_DOUBLE_TAP,
                targetView = doubleTapLeft,
                title = "Double Tap to Seek",
                description = "Double tap left side to rewind 10 seconds, right side to forward 10 seconds",
                tooltipPosition = TooltipPosition.CENTER,
                highlightShape = HighlightShape.ROUNDED_RECT,
                interactive = true,
                showGestureHint = true,
                gestureType = GestureType.DOUBLE_TAP
            ),
            TutorialStep(
                key = TUTORIAL_SWIPE_BRIGHTNESS,
                targetView = brightnessArea,
                title = "Adjust Brightness",
                description = "Swipe up/down on the left side to adjust screen brightness",
                tooltipPosition = TooltipPosition.END,
                highlightShape = HighlightShape.ROUNDED_RECT,
                interactive = true,
                showGestureHint = true,
                gestureType = GestureType.SWIPE_VERTICAL
            ),
            TutorialStep(
                key = TUTORIAL_SWIPE_VOLUME,
                targetView = volumeArea,
                title = "Adjust Volume",
                description = "Swipe up/down on the right side to adjust volume",
                tooltipPosition = TooltipPosition.START,
                highlightShape = HighlightShape.ROUNDED_RECT,
                interactive = true,
                showGestureHint = true,
                gestureType = GestureType.SWIPE_VERTICAL
            ),
            TutorialStep(
                key = TUTORIAL_LOCK,
                targetView = lockButton,
                title = "Lock Screen",
                description = "Lock the player to prevent accidental touches",
                tooltipPosition = TooltipPosition.BOTTOM,
                highlightShape = HighlightShape.CIRCLE,
                interactive = true,
                showGestureHint = true,
                gestureType = GestureType.TAP
            ),
            TutorialStep(
                key = TUTORIAL_SETTINGS,
                targetView = settingsButton,
                title = "Player Settings",
                description = "Access playback speed, subtitles, audio tracks, and more",
                tooltipPosition = TooltipPosition.BOTTOM,
                highlightShape = HighlightShape.CIRCLE,
                interactive = true,
                showGestureHint = true,
                gestureType = GestureType.TAP
            )
        )
    }
}

/**
 * Data class for tutorial step
 */
data class TutorialStep(
    val key: String,
    val targetView: View?,
    val title: String,
    val description: String,
    val tooltipPosition: TooltipPosition = TooltipPosition.BOTTOM,
    val highlightShape: HighlightShape = HighlightShape.ROUNDED_RECT,
    val interactive: Boolean = false,
    val showGestureHint: Boolean = false,
    val gestureType: GestureType = GestureType.TAP
)

enum class TooltipPosition {
    TOP,
    BOTTOM,
    START,
    END,
    CENTER
}

enum class HighlightShape {
    CIRCLE,
    ROUNDED_RECT,
    RECTANGLE
}

enum class GestureType {
    TAP,
    DOUBLE_TAP,
    SWIPE_VERTICAL,
    SWIPE_HORIZONTAL,
    PINCH,
    LONG_PRESS
}
