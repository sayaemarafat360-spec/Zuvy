package com.zuvy.app.player

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zuvy.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Manages lyrics parsing and synchronization
 */
class LyricsManager(
    private val scope: CoroutineScope
) {
    
    companion object {
        // LRC timestamp pattern: [mm:ss.xx] or [mm:ss:xx] or [mm:ss.xxx]
        private val LRC_TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\]")
        private val LRC_METADATA_PATTERN = Pattern.compile("\\[(\\w+):(.+)\\]")
    }
    
    data class LyricLine(
        val timestamp: Long,  // in milliseconds
        val text: String,
        var isHighlighted: Boolean = false
    )
    
    data class LyricsMetadata(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val author: String? = null,  // LRC creator
        val length: Long? = null     // song length in milliseconds
    )
    
    private var lyrics: List<LyricLine> = emptyList()
    private var metadata: LyricsMetadata = LyricsMetadata()
    private var currentIndex = -1
    private var offsetMs = 0L  // Manual offset adjustment
    
    private var updateJob: Job? = null
    private var onLyricsUpdateListener: ((List<LyricLine>, Int) -> Unit)? = null
    
    /**
     * Parse LRC format lyrics
     */
    fun parseLrc(content: String): Boolean {
        val lines = mutableListOf<LyricLine>()
        val metaBuilder = LyricsMetadata.Builder()
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach
            
            // Check for metadata
            val metaMatcher = LRC_METADATA_PATTERN.matcher(trimmedLine)
            if (metaMatcher.find()) {
                val key = metaMatcher.group(1)
                val value = metaMatcher.group(2)
                
                when (key?.lowercase()) {
                    "ti" -> metaBuilder.title(value)
                    "ar" -> metaBuilder.artist(value)
                    "al" -> metaBuilder.album(value)
                    "au" -> metaBuilder.author(value)
                    "length" -> {
                        val parts = value?.split(":")
                        if (parts != null && parts.size == 2) {
                            metaBuilder.length(parts[0].toLong() * 60000 + parts[1].toDouble().times(1000).toLong())
                        }
                    }
                }
                return@forEach
            }
            
            // Parse timestamps and lyrics
            val timestamps = mutableListOf<Long>()
            var lyricText = trimmedLine
            
            val timestampMatcher = LRC_TIMESTAMP_PATTERN.matcher(trimmedLine)
            while (timestampMatcher.find()) {
                val minutes = timestampMatcher.group(1)?.toLong() ?: 0
                val seconds = timestampMatcher.group(2)?.toLong() ?: 0
                val millis = timestampMatcher.group(3)?.let { 
                    if (it.length == 2) it.toLong() * 10 else it.toLong()
                } ?: 0
                
                timestamps.add(minutes * 60000 + seconds * 1000 + millis)
            }
            
            // Remove timestamps from lyric text
            lyricText = LRC_TIMESTAMP_PATTERN.matcher(trimmedLine).replaceAll("").trim()
            
            // Add line for each timestamp (some LRC files have multiple timestamps per line)
            timestamps.forEach { timestamp ->
                if (lyricText.isNotEmpty()) {
                    lines.add(LyricLine(timestamp, lyricText))
                }
            }
        }
        
        // Sort by timestamp
        lines.sortBy { it.timestamp }
        
        if (lines.isNotEmpty()) {
            lyrics = lines
            metadata = metaBuilder.build()
            return true
        }
        
        return false
    }
    
    /**
     * Load lyrics from file
     */
    suspend fun loadFromFile(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val content = BufferedReader(InputStreamReader(file.inputStream())).use { reader ->
                    reader.readText()
                }
                parseLrc(content)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * Load lyrics from raw string
     */
    fun loadFromString(content: String): Boolean {
        return parseLrc(content)
    }
    
    /**
     * Start syncing lyrics with playback
     */
    fun startSync(
        getPosition: () -> Long,
        isPlaying: () -> Boolean
    ) {
        stopSync()
        
        updateJob = scope.launch {
            while (isActive) {
                if (isPlaying()) {
                    val position = getPosition() + offsetMs
                    updateCurrentLine(position)
                }
                delay(50) // Update every 50ms for smooth transitions
            }
        }
    }
    
    /**
     * Stop syncing
     */
    fun stopSync() {
        updateJob?.cancel()
        updateJob = null
    }
    
    /**
     * Update current line based on position
     */
    private fun updateCurrentLine(position: Long) {
        var newIndex = -1
        
        // Find the current line
        for (i in lyrics.indices) {
            if (lyrics[i].timestamp <= position) {
                newIndex = i
            } else {
                break
            }
        }
        
        // Update if changed
        if (newIndex != currentIndex) {
            val oldIndex = currentIndex
            currentIndex = newIndex
            
            // Update highlight states
            lyrics.forEachIndexed { index, line ->
                line.isHighlighted = index == currentIndex
            }
            
            onLyricsUpdateListener?.invoke(lyrics, currentIndex)
        }
    }
    
    /**
     * Set lyrics update listener
     */
    fun setOnLyricsUpdateListener(listener: (List<LyricLine>, Int) -> Unit) {
        onLyricsUpdateListener = listener
    }
    
    /**
     * Get current lyrics
     */
    fun getLyrics(): List<LyricLine> = lyrics
    
    /**
     * Get current line index
     */
    fun getCurrentIndex(): Int = currentIndex
    
    /**
     * Get current lyric line
     */
    fun getCurrentLine(): LyricLine? = lyrics.getOrNull(currentIndex)
    
    /**
     * Get metadata
     */
    fun getMetadata(): LyricsMetadata = metadata
    
    /**
     * Set manual offset (positive = lyrics appear later)
     */
    fun setOffset(offsetMs: Long) {
        this.offsetMs = offsetMs
    }
    
    /**
     * Get current offset
     */
    fun getOffset(): Long = offsetMs
    
    /**
     * Check if lyrics are loaded
     */
    fun hasLyrics(): Boolean = lyrics.isNotEmpty()
    
    /**
     * Clear lyrics
     */
    fun clear() {
        lyrics = emptyList()
        metadata = LyricsMetadata()
        currentIndex = -1
        stopSync()
    }
    
    /**
     * Builder for LyricsMetadata
     */
    data class LyricsMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val author: String?,
        val length: Long?
    ) {
        class Builder {
            private var title: String? = null
            private var artist: String? = null
            private var album: String? = null
            private var author: String? = null
            private var length: Long? = null
            
            fun title(title: String?) = apply { this.title = title }
            fun artist(artist: String?) = apply { this.artist = artist }
            fun album(album: String?) = apply { this.album = album }
            fun author(author: String?) = apply { this.author = author }
            fun length(length: Long?) = apply { this.length = length }
            
            fun build() = LyricsMetadata(title, artist, album, author, length)
        }
    }
}

/**
 * Custom view for displaying synchronized lyrics
 */
class LyricsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val recyclerView: RecyclerView
    private val adapter: LyricsAdapter
    private var currentIndex = -1
    
    private val highlightColor = Color.parseColor("#6C63FF")  // App accent color
    private val normalColor = Color.parseColor("#80FFFFFF")   // Semi-transparent white
    
    init {
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            overScrollMode = OVER_SCROLL_NEVER
        }
        addView(recyclerView)
        
        adapter = LyricsAdapter()
        recyclerView.adapter = adapter
    }
    
    /**
     * Update lyrics
     */
    fun setLyrics(lyrics: List<LyricsManager.LyricLine>) {
        adapter.setLyrics(lyrics)
    }
    
    /**
     * Scroll to current line
     */
    fun scrollToLine(index: Int, smooth: Boolean = true) {
        if (index == currentIndex) return
        currentIndex = index
        
        if (index >= 0 && index < adapter.itemCount) {
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            
            if (smooth) {
                // Smooth scroll to center the current line
                layoutManager.smoothScrollToPosition(recyclerView, null, index)
            } else {
                layoutManager.scrollToPositionWithOffset(index, height / 3)
            }
        }
        
        adapter.highlightLine(index)
    }
    
    /**
     * Set text size
     */
    fun setTextSize(size: Float) {
        adapter.setTextSize(size)
    }
    
    /**
     * Set highlight color
     */
    fun setHighlightColor(color: Int) {
        adapter.setHighlightColor(color)
    }
    
    /**
     * RecyclerView adapter for lyrics
     */
    private inner class LyricsAdapter : RecyclerView.Adapter<LyricsViewHolder>() {
        
        private var lyrics: List<LyricsManager.LyricLine> = emptyList()
        private var highlightedIndex = -1
        private var textSize = 16f
        private var highlightColor = Color.parseColor("#6C63FF")
        
        fun setLyrics(newLyrics: List<LyricsManager.LyricLine>) {
            lyrics = newLyrics
            notifyDataSetChanged()
        }
        
        fun highlightLine(index: Int) {
            val oldIndex = highlightedIndex
            highlightedIndex = index
            notifyItemChanged(oldIndex)
            notifyItemChanged(index)
        }
        
        fun setTextSize(size: Float) {
            textSize = size
            notifyDataSetChanged()
        }
        
        fun setHighlightColor(color: Int) {
            highlightColor = color
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricsViewHolder {
            val textView = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 16, 32, 16)
                gravity = android.view.Gravity.CENTER
                textSize = this@LyricsAdapter.textSize
            }
            return LyricsViewHolder(textView)
        }
        
        override fun onBindViewHolder(holder: LyricsViewHolder, position: Int) {
            val lyric = lyrics[position]
            val textView = holder.itemView as TextView
            
            val text = SpannableString(lyric.text)
            val color = if (position == highlightedIndex) highlightColor else normalColor
            text.setSpan(
                ForegroundColorSpan(color),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            textView.text = text
            textView.textSize = if (position == highlightedIndex) textSize + 2 else textSize
            textView.alpha = if (position == highlightedIndex) 1f else 0.7f
        }
        
        override fun getItemCount(): Int = lyrics.size
    }
    
    private inner class LyricsViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view)
}
