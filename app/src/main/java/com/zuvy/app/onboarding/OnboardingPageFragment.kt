package com.zuvy.app.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentOnboardingPageBinding

/**
 * OnboardingPageFragment - Individual onboarding page
 * 
 * Features:
 * - Animated illustrations
 * - Interactive elements
 * - Smooth transitions
 */
class OnboardingPageFragment : Fragment() {

    private var _binding: FragmentOnboardingPageBinding? = null
    private val binding get() = _binding!!

    private var pageData: OnboardingPage? = null

    companion object {
        private const val ARG_PAGE_DATA = "page_data"

        fun newInstance(page: OnboardingPage): OnboardingPageFragment {
            return OnboardingPageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PAGE_DATA, page)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageData = arguments?.getParcelable(ARG_PAGE_DATA)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pageData?.let { setupPage(it) }
    }

    private fun setupPage(page: OnboardingPage) {
        // Set content
        binding.titleText.text = page.title
        binding.descriptionText.text = page.description
        
        // Set illustration
        if (page.illustrationRes != 0) {
            binding.illustrationImage.setImageResource(page.illustrationRes)
        }

        // Set background gradient
        page.backgroundColor?.let { color ->
            binding.root.setBackgroundColor(color)
        }

        // Start entrance animations
        startAnimations()
    }

    private fun startAnimations() {
        // Reset states
        binding.illustrationImage.alpha = 0f
        binding.illustrationImage.scaleX = 0.5f
        binding.illustrationImage.scaleY = 0.5f
        binding.illustrationImage.translationY = 50f

        binding.titleText.alpha = 0f
        binding.titleText.translationY = 30f

        binding.descriptionText.alpha = 0f
        binding.descriptionText.translationY = 30f

        // Animate illustration
        val scaleX = ObjectAnimator.ofFloat(binding.illustrationImage, "scaleX", 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.illustrationImage, "scaleY", 0.5f, 1f)
        val alphaImage = ObjectAnimator.ofFloat(binding.illustrationImage, "alpha", 0f, 1f)
        val translationImage = ObjectAnimator.ofFloat(binding.illustrationImage, "translationY", 50f, 0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alphaImage, translationImage)
            duration = 600
            interpolator = OvershootInterpolator(1.2f)
            startDelay = 200
        }.start()

        // Animate title
        binding.titleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animate description
        binding.descriptionText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Start continuous animation for illustration
        startContinuousAnimation()
    }

    private fun startContinuousAnimation() {
        // Floating animation for illustration
        val floatUp = ObjectAnimator.ofFloat(binding.illustrationImage, "translationY", 0f, -15f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val pulseScale = ObjectAnimator.ofFloat(binding.illustrationImage, "scale", 1f, 1.05f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        floatUp.startDelay = 800
        floatUp.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Data class for onboarding page
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val illustrationRes: Int = 0,
    val backgroundColor: Int? = null,
    val animationType: AnimationType = AnimationType.FADE
) : android.os.Parcelable {
    constructor(parcel: android.os.Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        if (parcel.readByte() == 1.toByte()) parcel.readInt() else null,
        AnimationType.values()[parcel.readInt()]
    )

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeInt(illustrationRes)
        backgroundColor?.let { 
            parcel.writeByte(1)
            parcel.writeInt(it)
        } ?: parcel.writeByte(0)
        parcel.writeInt(animationType.ordinal)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : android.os.Parcelable.Creator<OnboardingPage> {
        override fun createFromParcel(parcel: android.os.Parcel): OnboardingPage {
            return OnboardingPage(parcel)
        }

        override fun newArray(size: Int): Array<OnboardingPage?> {
            return arrayOfNulls(size)
        }
    }
}

enum class AnimationType {
    FADE,
    SLIDE,
    SCALE,
    BOUNCE
}
