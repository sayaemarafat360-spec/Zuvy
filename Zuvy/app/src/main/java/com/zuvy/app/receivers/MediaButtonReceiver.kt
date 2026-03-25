package com.zuvy.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (event?.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        // Handle play
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        // Handle pause
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        // Handle play/pause toggle
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        // Handle next
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        // Handle previous
                    }
                }
            }
        }
    }
}
