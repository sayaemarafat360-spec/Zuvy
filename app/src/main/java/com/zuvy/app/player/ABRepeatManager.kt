package com.zuvy.app.player

/**
 * Manages A-B repeat functionality
 */
class ABRepeatManager {
    
    data class RepeatSegment(
        val id: String,
        val name: String,
        val startTime: Long, // milliseconds
        val endTime: Long,
        val videoId: String
    )
    
    private var startTime: Long? = null
    private var endTime: Long? = null
    private var isActive: Boolean = false
    private var savedSegments: MutableList<RepeatSegment> = mutableListOf()
    
    /**
     * Set point A (start)
     */
    fun setPointA(time: Long): String {
        startTime = time
        endTime = null
        isActive = false
        return formatTime(time)
    }
    
    /**
     * Set point B (end) and activate
     */
    fun setPointB(time: Long): Boolean {
        if (startTime == null) return false
        
        endTime = time
        
        // Validate
        if (endTime!! <= startTime!!) {
            // Swap if reversed
            val temp = startTime
            startTime = endTime
            endTime = temp
        }
        
        isActive = true
        return true
    }
    
    /**
     * Check if playback should loop back
     */
    fun shouldLoop(currentPosition: Long): Boolean {
        if (!isActive || startTime == null || endTime == null) return false
        
        return currentPosition >= endTime!!
    }
    
    /**
     * Get the position to loop back to
     */
    fun getLoopPosition(): Long = startTime ?: 0L
    
    /**
     * Get current repeat range
     */
    fun getRepeatRange(): Pair<Long, Long>? {
        if (startTime != null && endTime != null) {
            return Pair(startTime!!, endTime!!)
        }
        return null
    }
    
    /**
     * Clear current A-B repeat
     */
    fun clear() {
        startTime = null
        endTime = null
        isActive = false
    }
    
    /**
     * Save current segment
     */
    fun saveSegment(name: String, videoId: String): RepeatSegment? {
        val range = getRepeatRange() ?: return null
        
        val segment = RepeatSegment(
            id = System.currentTimeMillis().toString(),
            name = name,
            startTime = range.first,
            endTime = range.second,
            videoId = videoId
        )
        
        savedSegments.add(segment)
        return segment
    }
    
    /**
     * Get saved segments for a video
     */
    fun getSegmentsForVideo(videoId: String): List<RepeatSegment> {
        return savedSegments.filter { it.videoId == videoId }
    }
    
    /**
     * Load a saved segment
     */
    fun loadSegment(segment: RepeatSegment) {
        startTime = segment.startTime
        endTime = segment.endTime
        isActive = true
    }
    
    /**
     * Delete a saved segment
     */
    fun deleteSegment(segmentId: String) {
        savedSegments.removeAll { it.id == segmentId }
    }
    
    /**
     * Check if A-B repeat is currently active
     */
    fun isRepeatActive(): Boolean = isActive && startTime != null && endTime != null
    
    /**
     * Check if point A is set
     */
    fun hasPointA(): Boolean = startTime != null
    
    /**
     * Check if point B is set
     */
    fun hasPointB(): Boolean = endTime != null
    
    /**
     * Toggle A-B repeat on/off
     */
    fun toggle() {
        if (startTime != null && endTime != null) {
            isActive = !isActive
        }
    }
    
    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
