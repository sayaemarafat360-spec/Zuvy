package com.zuvy.app.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.zuvy.app.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time audio visualizer with multiple visualization modes
 */
class AudioVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        const val VISUALIZER_MODE_BARS = 0
        const val VISUALIZER_MODE_WAVE = 1
        const val VISUALIZER_MODE_CIRCLE = 2
        const val VISUALIZER_MODE_PARTICLES = 3
    }
    
    // Visualizer modes
    enum class VisualizationMode {
        BARS,       // Classic frequency bars
        WAVE,       // Waveform display
        CIRCLE,     // Circular visualizer
        PARTICLES   // Particle effects
    }
    
    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0
    
    private var fftData: ByteArray? = null
    private var waveData: ByteArray? = null
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    private var mode = VisualizationMode.BARS
    private var color = Color.parseColor("#6C63FF")  // App accent
    private var glowColor = Color.parseColor("#406C63FF")
    private var barCount = 64
    private var smoothingFactor = 0.6f
    
    // Animation state
    private var particles = mutableListOf<Particle>()
    private var previousAmplitudes = FloatArray(barCount)
    
    // Animation properties
    private var rotationAngle = 0f
    private var pulseScale = 1f
    
    init {
        paint.style = Paint.Style.FILL
        glowPaint.style = Paint.Style.FILL
        glowPaint.color = glowColor
        glowPaint.maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    
    /**
     * Set audio session ID for visualization
     */
    fun setAudioSessionId(sessionId: Int) {
        if (audioSessionId == sessionId) return
        
        release()
        audioSessionId = sessionId
        
        try {
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Float
                    ) {
                        waveform?.let { waveData = it }
                    }
                    
                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Float
                    ) {
                        fft?.let { fftData = it }
                        postInvalidate()
                    }
                }, 
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true
                )
                
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Set visualization mode
     */
    fun setMode(mode: VisualizationMode) {
        this.mode = mode
        invalidate()
    }
    
    /**
     * Set visualization color
     */
    fun setColor(color: Int) {
        this.color = color
        this.glowColor = Color.argb(
            64,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
        glowPaint.color = glowColor
        invalidate()
    }
    
    /**
     * Set number of bars for bar visualization
     */
    fun setBarCount(count: Int) {
        barCount = count.coerceIn(16, 128)
        previousAmplitudes = FloatArray(barCount)
    }
    
    /**
     * Set smoothing factor (0-1)
     */
    fun setSmoothing(factor: Float) {
        smoothingFactor = factor.coerceIn(0f, 1f)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        when (mode) {
            VisualizationMode.BARS -> drawBars(canvas)
            VisualizationMode.WAVE -> drawWave(canvas)
            VisualizationMode.CIRCLE -> drawCircle(canvas, centerX, centerY)
            VisualizationMode.PARTICLES -> drawParticles(canvas, centerX, centerY)
        }
        
        // Update rotation for circular mode
        rotationAngle += 0.5f
        if (rotationAngle >= 360f) rotationAngle = 0f
        
        // Pulse animation
        pulseScale = 1f + 0.02f * sin(System.currentTimeMillis() / 200.0).toFloat()
    }
    
    private fun drawBars(canvas: Canvas) {
        val fft = fftData ?: return
        
        val barWidth = width.toFloat() / barCount
        val magnitudes = calculateMagnitudes(fft, barCount)
        
        for (i in 0 until barCount) {
            // Apply smoothing
            val amplitude = if (previousAmplitudes[i] != 0f) {
                previousAmplitudes[i] * smoothingFactor + magnitudes[i] * (1 - smoothingFactor)
            } else {
                magnitudes[i]
            }
            previousAmplitudes[i] = amplitude
            
            // Draw bar with gradient
            val barHeight = amplitude * height * 0.8f
            val left = i * barWidth + barWidth * 0.1f
            val top = height - barHeight
            val right = left + barWidth * 0.8f
            val bottom = height.toFloat()
            
            // Gradient color based on height
            val intensity = (amplitude * 255).toInt().coerceIn(0, 255)
            paint.color = adjustColorIntensity(color, intensity / 255f)
            
            // Draw glow
            canvas.drawRect(left - 2, top - 2, right + 2, bottom, glowPaint)
            
            // Draw bar
            canvas.drawRoundRect(left, top, right, bottom, 4f, 4f, paint)
        }
    }
    
    private fun drawWave(canvas: Canvas) {
        val wave = waveData ?: return
        
        path.reset()
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        
        val points = wave.size
        val stepX = width.toFloat() / points
        
        path.moveTo(0f, height / 2f)
        
        for (i in wave.indices) {
            val x = i * stepX
            val y = height / 2f + (wave[i].toFloat() / 128f) * (height / 2f) * 0.8f
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Draw glow effect
        canvas.drawPath(path, glowPaint)
        
        // Draw wave
        paint.color = color
        canvas.drawPath(path, paint)
        
        // Draw mirror wave (inverted)
        path.reset()
        for (i in wave.indices) {
            val x = i * stepX
            val y = height / 2f - (wave[i].toFloat() / 128f) * (height / 2f) * 0.8f
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        paint.color = adjustColorIntensity(color, 0.5f)
        canvas.drawPath(path, paint)
        
        paint.style = Paint.Style.FILL
    }
    
    private fun drawCircle(canvas: Canvas, centerX: Float, centerY: Float) {
        val fft = fftData ?: return
        
        val magnitudes = calculateMagnitudes(fft, barCount)
        val radius = minOf(width, height) / 3f
        
        paint.style = Paint.Style.FILL
        
        for (i in 0 until barCount) {
            val angle = (i.toFloat() / barCount) * 360f + rotationAngle
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            
            val amplitude = magnitudes[i] * radius * 0.8f
            
            val startX = centerX + radius * cos(rad)
            val startY = centerY + radius * sin(rad)
            val endX = centerX + (radius + amplitude) * cos(rad)
            val endY = centerY + (radius + amplitude) * sin(rad)
            
            // Gradient color
            val intensity = (magnitudes[i] * 255).toInt().coerceIn(0, 255)
            paint.color = adjustColorIntensity(color, intensity / 255f)
            
            // Draw line
            paint.strokeWidth = 4f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(startX, startY, endX, endY, paint)
            
            // Draw dot at end
            paint.style = Paint.Style.FILL
            canvas.drawCircle(endX, endY, 3f, paint)
        }
        
        // Draw center circle
        paint.color = color
        paint.alpha = 100
        canvas.drawCircle(centerX, centerY, radius * 0.3f * pulseScale, paint)
        paint.alpha = 255
    }
    
    private fun drawParticles(canvas: Canvas, centerX: Float, centerY: Float) {
        val fft = fftData ?: return
        
        val avgMagnitude = calculateMagnitudes(fft, 1)[0]
        
        // Spawn new particles based on audio intensity
        if (avgMagnitude > 0.3f && particles.size < 100) {
            for (i in 0..(avgMagnitude * 5).toInt()) {
                particles.add(Particle(
                    x = centerX,
                    y = centerY,
                    vx = (Math.random().toFloat() - 0.5f) * 10 * avgMagnitude,
                    vy = (Math.random().toFloat() - 0.5f) * 10 * avgMagnitude,
                    size = (Math.random() * 8 + 2).toFloat(),
                    life = 1f,
                    color = adjustColorIntensity(color, Math.random().toFloat())
                ))
            }
        }
        
        // Update and draw particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            // Update position
            p.x += p.vx
            p.y += p.vy
            p.life -= 0.02f
            
            // Remove dead particles
            if (p.life <= 0) {
                iterator.remove()
                continue
            }
            
            // Draw particle
            paint.color = p.color
            paint.alpha = (p.life * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.size * p.life, paint)
        }
        
        paint.alpha = 255
    }
    
    private fun calculateMagnitudes(fft: ByteArray, bands: Int): FloatArray {
        val magnitudes = FloatArray(bands)
        val n = fft.size / 2
        val bandSize = n / bands
        
        for (i in 0 until bands) {
            var sum = 0.0
            for (j in 0 until bandSize) {
                val index = (i * bandSize + j) * 2
                if (index + 1 < fft.size) {
                    val real = fft[index].toFloat()
                    val imag = fft[index + 1].toFloat()
                    sum += Math.sqrt((real * real + imag * imag).toDouble())
                }
            }
            // Normalize to 0-1 range
            magnitudes[i] = (sum / bandSize / 128f).coerceIn(0f, 1f)
        }
        
        return magnitudes
    }
    
    private fun adjustColorIntensity(color: Int, intensity: Float): Int {
        val r = (Color.red(color) * intensity).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * intensity).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * intensity).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
    
    /**
     * Release visualizer resources
     */
    fun release() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
        fftData = null
        waveData = null
        particles.clear()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
    
    /**
     * Particle class for particle visualization
     */
    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var life: Float,
        var color: Int
    )
}
