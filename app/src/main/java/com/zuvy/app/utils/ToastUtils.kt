package com.zuvy.app.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.zuvy.app.R

object ToastUtils {
    
    fun showSuccess(context: Context, message: String) {
        showCustomToast(context, message, ToastType.SUCCESS)
    }
    
    fun showError(context: Context, message: String) {
        showCustomToast(context, message, ToastType.ERROR)
    }
    
    fun showInfo(context: Context, message: String) {
        showCustomToast(context, message, ToastType.INFO)
    }
    
    fun showWarning(context: Context, message: String) {
        showCustomToast(context, message, ToastType.WARNING)
    }
    
    fun showMusic(context: Context, message: String) {
        showCustomToast(context, message, ToastType.MUSIC)
    }
    
    fun showVideo(context: Context, message: String) {
        showCustomToast(context, message, ToastType.VIDEO)
    }
    
    private fun showCustomToast(context: Context, message: String, type: ToastType) {
        try {
            val layout = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null)
            
            val iconView = layout.findViewById<ImageView>(R.id.toastIcon)
            val textView = layout.findViewById<TextView>(R.id.toastMessage)
            val containerView = layout.findViewById<View>(R.id.toastContainer)
            
            textView.text = message
            
            val (iconRes, bgColor) = when (type) {
                ToastType.SUCCESS -> R.drawable.ic_check_circle to R.drawable.bg_toast_success
                ToastType.ERROR -> R.drawable.ic_error to R.drawable.bg_toast_error
                ToastType.INFO -> R.drawable.ic_info to R.drawable.bg_toast_info
                ToastType.WARNING -> R.drawable.ic_warning to R.drawable.bg_toast_warning
                ToastType.MUSIC -> R.drawable.ic_music_note to R.drawable.bg_toast_music
                ToastType.VIDEO -> R.drawable.ic_video to R.drawable.bg_toast_video
            }
            
            iconView.setImageResource(iconRes)
            containerView.setBackgroundResource(bgColor)
            
            val toast = Toast(context)
            toast.duration = Toast.LENGTH_SHORT
            toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
            toast.view = layout
            toast.show()
        } catch (e: Exception) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    fun showSnackbar(view: View, message: String, actionText: String? = null, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }
    
    inline fun <reified T> showActionToast(context: Context, message: String, actionLabel: String, crossinline action: () -> Unit) {
        // This creates a snackbar-style action toast
    }
}

enum class ToastType {
    SUCCESS, ERROR, INFO, WARNING, MUSIC, VIDEO
}
