package com.zuvy.app.player

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Manages subtitle loading, parsing, and rendering
 * Supports SRT, VTT, ASS/SSA formats
 */
class SubtitleManager(private val context: Context) {
    
    data class Subtitle(
        val index: Int,
        val startTime: Long, // milliseconds
        val endTime: Long,
        val text: String,
        val styling: SubtitleStyle? = null
    )
    
    data class SubtitleStyle(
        val fontColor: Int = Color.WHITE,
        val backgroundColor: Int = Color.TRANSPARENT,
        val fontSize: Float = 18f,
        val fontFamily: String? = null,
        val bold: Boolean = false,
        val italic: Boolean = false
    )
    
    private var subtitles: List<Subtitle> = emptyList()
    private var currentSubtitle: Subtitle? = null
    private var offsetMs: Long = 0 // Sync offset
    
    // User preferences
    var enabled: Boolean = false
    var style: SubtitleStyle = SubtitleStyle()
    
    suspend fun loadSubtitle(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = readSubtitleFile(uri)
            subtitles = when {
                content.contains("WEBVTT") -> parseVTT(content)
                content.contains("[Script Info]") -> parseASS(content)
                else -> parseSRT(content)
            }
            enabled = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun loadSubtitle(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext false
            
            val content = file.readText()
            subtitles = when {
                content.contains("WEBVTT") -> parseVTT(content)
                content.contains("[Script Info]") -> parseASS(content)
                else -> parseSRT(content)
            }
            enabled = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun readSubtitleFile(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open subtitle file")
        return BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { it.readText() }
    }
    
    // SRT Parser
    private fun parseSRT(content: String): List<Subtitle> {
        val subtitles = mutableListOf<Subtitle>()
        val blocks = content.split(Regex("\\n\\s*\\n"))
        
        var index = 0
        for (block in blocks) {
            val lines = block.trim().split("\n")
            if (lines.size < 3) continue
            
            try {
                // Parse timing: 00:00:00,000 --> 00:00:00,000
                val timeLine = lines[1]
                val times = timeLine.split(" --> ")
                if (times.size != 2) continue
                
                val startTime = parseSRTTime(times[0].trim())
                val endTime = parseSRTTime(times[1].trim())
                
                // Parse text (may span multiple lines)
                val text = lines.drop(2).joinToString("\n")
                    .replace(Regex("<[^>]+>"), "") // Remove HTML tags
                
                subtitles.add(Subtitle(
                    index = index++,
                    startTime = startTime,
                    endTime = endTime,
                    text = text
                ))
            } catch (e: Exception) {
                // Skip malformed blocks
            }
        }
        
        return subtitles
    }
    
    private fun parseSRTTime(timeStr: String): Long {
        // Format: HH:MM:SS,mmm
        val parts = timeStr.replace(",", ":").split(":")
        if (parts.size != 4) return 0L
        
        return parts[0].toLong() * 3600000 +
               parts[1].toLong() * 60000 +
               parts[2].toLong() * 1000 +
               parts[3].toLong()
    }
    
    // VTT Parser
    private fun parseVTT(content: String): List<Subtitle> {
        val subtitles = mutableListOf<Subtitle>()
        val lines = content.split("\n")
        
        var index = 0
        var i = 0
        while (i < lines.size) {
            // Look for timing line
            if (lines[i].contains("-->")) {
                val times = lines[i].split(" --> ")
                val startTime = parseVTTTime(times[0].trim())
                val endTime = parseVTTTime(times[1].trim().split(" ")[0])
                
                // Collect text lines
                val textLines = mutableListOf<String>()
                i++
                while (i < lines.size && lines[i].isNotBlank()) {
                    textLines.add(lines[i].replace(Regex("<[^>]+>"), ""))
                    i++
                }
                
                subtitles.add(Subtitle(
                    index = index++,
                    startTime = startTime,
                    endTime = endTime,
                    text = textLines.joinToString("\n")
                ))
            }
            i++
        }
        
        return subtitles
    }
    
    private fun parseVTTTime(timeStr: String): Long {
        // Format: HH:MM:SS.mmm or MM:SS.mmm
        val parts = timeStr.replace(".", ":").split(":")
        
        return when (parts.size) {
            4 -> parts[0].toLong() * 3600000 + parts[1].toLong() * 60000 +
                  parts[2].toLong() * 1000 + parts[3].toLong()
            3 -> parts[0].toLong() * 60000 + parts[1].toLong() * 1000 + parts[2].toLong()
            else -> 0L
        }
    }
    
    // ASS/SSA Parser (simplified)
    private fun parseASS(content: String): List<Subtitle> {
        val subtitles = mutableListOf<Subtitle>()
        val lines = content.split("\n")
        
        var index = 0
        for (line in lines) {
            if (line.startsWith("Dialogue:")) {
                val parts = line.substring(9).split(",")
                if (parts.size >= 10) {
                    val startTime = parseASSTime(parts[1].trim())
                    val endTime = parseASSTime(parts[2].trim())
                    val text = parts.drop(9).joinToString(",")
                        .replace(Regex("\\{[^}]+\\}"), "") // Remove ASS tags
                        .replace("\\N", "\n") // Newline in ASS
                    
                    subtitles.add(Subtitle(
                        index = index++,
                        startTime = startTime,
                        endTime = endTime,
                        text = text
                    ))
                }
            }
        }
        
        return subtitles
    }
    
    private fun parseASSTime(timeStr: String): Long {
        // Format: H:MM:SS.cc
        val parts = timeStr.split(":")
        if (parts.size != 3) return 0L
        
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val seconds = parts[2].split(".")
        
        return hours * 3600000 + minutes * 60000 +
               seconds[0].toLong() * 1000 +
               (if (seconds.size > 1) seconds[1].padEnd(3, '0').toLong() else 0L)
    }
    
    /**
     * Update subtitle display based on current playback position
     */
    fun update(position: Long, textView: TextView) {
        if (!enabled) {
            textView.visibility = View.GONE
            return
        }
        
        val adjustedPosition = position + offsetMs
        
        val subtitle = subtitles.find { 
            it.startTime <= adjustedPosition && it.endTime >= adjustedPosition 
        }
        
        if (subtitle != currentSubtitle) {
            currentSubtitle = subtitle
            
            if (subtitle != null) {
                val styledText = SpannableStringBuilder(subtitle.text)
                
                // Apply background
                styledText.setSpan(
                    BackgroundColorSpan(style.backgroundColor),
                    0, styledText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // Apply foreground color
                styledText.setSpan(
                    ForegroundColorSpan(style.fontColor),
                    0, styledText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // Apply bold/italic
                if (style.bold || style.italic) {
                    val styleSpan = when {
                        style.bold && style.italic -> Typeface.BOLD_ITALIC
                        style.bold -> Typeface.BOLD
                        else -> Typeface.ITALIC
                    }
                    styledText.setSpan(
                        StyleSpan(styleSpan),
                        0, styledText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                
                textView.text = styledText
                textView.textSize = style.fontSize
                textView.visibility = View.VISIBLE
            } else {
                textView.visibility = View.GONE
            }
        }
    }
    
    /**
     * Adjust subtitle timing offset
     */
    fun adjustOffset(delta: Long) {
        offsetMs += delta
    }
    
    fun setOffset(offset: Long) {
        offsetMs = offset
    }
    
    /**
     * Find subtitle files matching video name
     */
    fun findMatchingSubtitles(videoPath: String): List<File> {
        val videoFile = File(videoPath)
        val parentDir = videoFile.parentFile ?: return emptyList()
        val baseName = videoFile.nameWithoutExtension
        
        return parentDir.listFiles()?.filter { file ->
            val name = file.nameWithoutExtension
            val ext = file.extension.lowercase()
            ext in listOf("srt", "vtt", "ass", "ssa") &&
            (name == baseName || name.startsWith("$baseName."))
        } ?: emptyList()
    }
    
    fun release() {
        subtitles = emptyList()
        currentSubtitle = null
    }
}
