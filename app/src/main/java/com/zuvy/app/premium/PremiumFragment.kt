package com.zuvy.app.premium

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentPremiumBinding
import com.zuvy.app.databinding.ItemPremiumPlanBinding
import com.zuvy.app.databinding.ItemPremiumFeatureBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * PremiumFragment - Premium subscription screen
 * 
 * Features:
 * - 3 subscription plans (Monthly, Yearly, Lifetime)
 * - Premium features list
 * - Animated UI
 * - Simulated purchase flow
 */
@AndroidEntryPoint
class PremiumFragment : Fragment() {

    private var _binding: FragmentPremiumBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PremiumViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupObservers()
        setupClickListeners()
        startEntranceAnimation()
    }

    private fun setupViews() {
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Setup premium features list
        setupFeaturesList()

        // Setup subscription plans
        setupPlans()
    }

    private fun setupFeaturesList() {
        val featuresContainer = binding.featuresContainer
        featuresContainer.removeAllViews()

        viewModel.premiumFeatures.forEachIndexed { index, feature ->
            val featureBinding = ItemPremiumFeatureBinding.inflate(
                layoutInflater,
                featuresContainer,
                false
            ).apply {
                featureIcon.setImageResource(getFeatureIcon(feature))
                featureName.text = feature.displayName
                root.alpha = 0f
                root.translationY = 20f
            }
            featuresContainer.addView(featureBinding.root)

            // Animate feature items
            featureBinding.root.postDelayed({
                featureBinding.root.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }, 100L + (index * 50L))
        }
    }

    private fun setupPlans() {
        updatePlanSelection(viewModel.selectedPlan.value)
    }

    private fun updatePlanSelection(selectedPlan: PremiumEngine.SubscriptionPlan) {
        val plansContainer = binding.plansContainer
        plansContainer.removeAllViews()

        PremiumEngine.SubscriptionPlan.entries.forEach { plan ->
            val planBinding = ItemPremiumPlanBinding.inflate(
                layoutInflater,
                plansContainer,
                false
            ).apply {
                val isSelected = plan == selectedPlan
                val savings = viewModel.getSavings(plan)

                planName.text = plan.displayName
                planPrice.text = viewModel.getFormattedPrice(plan)
                pricePerMonth.text = viewModel.getPricePerMonth(plan)

                // Show savings badge for yearly and lifetime
                if (savings > 0) {
                    savingsBadge.text = "Save $savings%"
                    savingsBadge.visibility = View.VISIBLE
                } else {
                    savingsBadge.visibility = View.GONE
                }

                // Selection indicator
                selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
                root.strokeWidth = if (isSelected) 2 else 0
                root.strokeColor = if (isSelected) 
                    resources.getColor(R.color.primary, null) 
                else 
                    resources.getColor(R.color.card_dark, null)

                root.setOnClickListener {
                    viewModel.selectPlan(plan)
                    animatePlanSelection(root)
                }
            }
            plansContainer.addView(planBinding.root)
        }
    }

    private fun animatePlanSelection(view: View) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f).apply {
            duration = 100
        }
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f).apply {
            duration = 200
            interpolator = OvershootInterpolator()
        }

        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            start()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe selected plan
                launch {
                    viewModel.selectedPlan.collect { plan ->
                        updatePlanSelection(plan)
                    }
                }

                // Observe premium status
                launch {
                    viewModel.isPremium.collect { isPremium ->
                        if (isPremium) {
                            showPremiumStatus()
                        }
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.purchaseButton.isEnabled = !isLoading
                        binding.purchaseButton.text = if (isLoading) "Processing..." else "Subscribe Now"
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // Observe purchase success
                launch {
                    viewModel.showPurchaseSuccess.collect { showSuccess ->
                        if (showSuccess) {
                            showSuccessDialog()
                            viewModel.dismissSuccessDialog()
                        }
                    }
                }

                // Observe errors
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            showErrorDialog(it)
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.purchaseButton.setOnClickListener {
            viewModel.startPurchase(requireActivity())
        }

        binding.restoreButton.setOnClickListener {
            viewModel.restorePurchases()
        }

        binding.termsButton.setOnClickListener {
            // TODO: Open terms of service
        }

        binding.privacyButton.setOnClickListener {
            // TODO: Open privacy policy
        }

        // Test mode button (for development)
        binding.testModeButton?.setOnClickListener {
            showTestModeDialog()
        }
    }

    private fun startEntranceAnimation() {
        // Animate header
        binding.headerContainer.apply {
            alpha = 0f
            translationY = -50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Animate crown icon
        binding.crownIcon.apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    private fun showPremiumStatus() {
        binding.purchaseButton.text = "You are Premium!"
        binding.purchaseButton.isEnabled = false
        binding.purchaseButton.setIconResource(R.drawable.ic_check)
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Welcome to Premium!")
            .setMessage("You now have access to all premium features. Enjoy an ad-free experience!")
            .setPositiveButton("Start Exploring") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Purchase Failed")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTestModeDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Test Mode")
            .setItems(arrayOf(
                "Simulate Premium (Monthly)",
                "Simulate Premium (Yearly)",
                "Simulate Premium (Lifetime)",
                "Reset Premium Status"
            )) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.selectPlan(PremiumEngine.SubscriptionPlan.MONTHLY)
                        viewModel.simulatePremiumActivation()
                    }
                    1 -> {
                        viewModel.selectPlan(PremiumEngine.SubscriptionPlan.YEARLY)
                        viewModel.simulatePremiumActivation()
                    }
                    2 -> {
                        viewModel.selectPlan(PremiumEngine.SubscriptionPlan.LIFETIME)
                        viewModel.simulatePremiumActivation()
                    }
                    3 -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            // Reset premium
                            // viewModel.resetPremium() // Would need to add this method
                        }
                    }
                }
            }
            .show()
    }

    private fun getFeatureIcon(feature: PremiumEngine.PremiumFeature): Int {
        return when (feature) {
            PremiumEngine.PremiumFeature.NO_ADS -> R.drawable.ic_block
            PremiumEngine.PremiumFeature.EQUALIZER -> R.drawable.ic_equalizer
            PremiumEngine.PremiumFeature.AUDIO_BOOST -> R.drawable.ic_volume_boost
            PremiumEngine.PremiumFeature.CROSSFADE -> R.drawable.ic_shuffle
            PremiumEngine.PremiumFeature.VISUALIZER -> R.drawable.ic_visualizer
            PremiumEngine.PremiumFeature.LYRICS -> R.drawable.ic_lyrics
            PremiumEngine.PremiumFeature.SLEEP_TIMER -> R.drawable.ic_sleep
            PremiumEngine.PremiumFeature.THEME -> R.drawable.ic_palette
            PremiumEngine.PremiumFeature.BACKUP -> R.drawable.ic_cloud_upload
            PremiumEngine.PremiumFeature.RINGTONE -> R.drawable.ic_ringtone
            PremiumEngine.PremiumFeature.LYRICS_EDITOR -> R.drawable.ic_edit
            PremiumEngine.PremiumFeature.VIDEO_FILTERS -> R.drawable.ic_filter
            PremiumEngine.PremiumFeature.GIF_RECORDER -> R.drawable.ic_gif
            PremiumEngine.PremiumFeature.A_B_REPEAT -> R.drawable.ic_repeat
            PremiumEngine.PremiumFeature.HIGH_RES -> R.drawable.ic_hd
            PremiumEngine.PremiumFeature.CASTING -> R.drawable.ic_cast
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
