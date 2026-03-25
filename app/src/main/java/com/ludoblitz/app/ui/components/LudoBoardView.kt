package com.ludoblitz.app.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.ludoblitz.app.R

/**
 * Custom View for rendering the Ludo game board
 * Handles drawing the board, paths, home bases, and tokens
 */
class LudoBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yellowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val homePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Board dimensions
    private var cellSize = 0f
    private var boardSize = 0f
    private var padding = 0f

    // Colors
    private val redColor: Int
    private val greenColor: Int
    private val yellowColor: Int
    private val blueColor: Int
    private val boardBgColor: Int
    private val pathColor: Int
    private val borderColor: Int

    init {
        // Get colors from resources
        redColor = context.getColor(R.color.token_red)
        greenColor = context.getColor(R.color.token_green)
        yellowColor = context.getColor(R.color.token_yellow)
        blueColor = context.getColor(R.color.token_blue)
        boardBgColor = context.getColor(R.color.board_background)
        pathColor = context.getColor(R.color.board_path)
        borderColor = context.getColor(R.color.board_border)

        // Initialize paints
        initPaints()
    }

    private fun initPaints() {
        boardPaint.apply {
            color = boardBgColor
            style = Paint.Style.FILL
        }

        pathPaint.apply {
            color = pathColor
            style = Paint.Style.FILL
        }

        redPaint.apply {
            color = redColor
            style = Paint.Style.FILL
        }

        greenPaint.apply {
            color = greenColor
            style = Paint.Style.FILL
        }

        yellowPaint.apply {
            color = yellowColor
            style = Paint.Style.FILL
        }

        bluePaint.apply {
            color = blueColor
            style = Paint.Style.FILL
        }

        borderPaint.apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        starPaint.apply {
            color = context.getColor(R.color.safe_zone_star)
            style = Paint.Style.FILL
        }

        homePaint.apply {
            color = context.getColor(R.color.board_home_center)
            style = Paint.Style.FILL
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boardSize = w.toFloat()
        cellSize = boardSize / 15f
        padding = cellSize / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw board background
        drawBoardBackground(canvas)

        // Draw home bases (colored corners)
        drawHomeBases(canvas)

        // Draw paths
        drawPaths(canvas)

        // Draw home stretch (colored paths to center)
        drawHomeStretches(canvas)

        // Draw center home
        drawCenterHome(canvas)

        // Draw safe zones (stars)
        drawSafeZones(canvas)

        // Draw borders
        drawBorders(canvas)
    }

    private fun drawBoardBackground(canvas: Canvas) {
        val rect = RectF(0f, 0f, boardSize, boardSize)
        canvas.drawRoundRect(rect, 16f, 16f, boardPaint)
    }

    private fun drawHomeBases(canvas: Canvas) {
        // Red home (top-left)
        val redRect = RectF(0f, 0f, cellSize * 6, cellSize * 6)
        canvas.drawRoundRect(redRect, 16f, 16f, redPaint)

        // Green home (top-right)
        val greenRect = RectF(cellSize * 9, 0f, boardSize, cellSize * 6)
        canvas.drawRoundRect(greenRect, 16f, 16f, greenPaint)

        // Yellow home (bottom-right)
        val yellowRect = RectF(cellSize * 9, cellSize * 9, boardSize, boardSize)
        canvas.drawRoundRect(yellowRect, 16f, 16f, yellowPaint)

        // Blue home (bottom-left)
        val blueRect = RectF(0f, cellSize * 9, cellSize * 6, boardSize)
        canvas.drawRoundRect(blueRect, 16f, 16f, bluePaint)

        // Draw token circles in each home
        drawHomeTokenCircles(canvas)
    }

    private fun drawHomeTokenCircles(canvas: Canvas) {
        val circleRadius = cellSize * 0.35f

        // Red tokens
        drawTokenCircle(canvas, cellSize * 1.5f, cellSize * 1.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 4.5f, cellSize * 1.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 1.5f, cellSize * 4.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 4.5f, cellSize * 4.5f, circleRadius, pathColor)

        // Green tokens
        drawTokenCircle(canvas, cellSize * 10.5f, cellSize * 1.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 13.5f, cellSize * 1.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 10.5f, cellSize * 4.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 13.5f, cellSize * 4.5f, circleRadius, pathColor)

        // Yellow tokens
        drawTokenCircle(canvas, cellSize * 10.5f, cellSize * 10.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 13.5f, cellSize * 10.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 10.5f, cellSize * 13.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 13.5f, cellSize * 13.5f, circleRadius, pathColor)

        // Blue tokens
        drawTokenCircle(canvas, cellSize * 1.5f, cellSize * 10.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 4.5f, cellSize * 10.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 1.5f, cellSize * 13.5f, circleRadius, pathColor)
        drawTokenCircle(canvas, cellSize * 4.5f, cellSize * 13.5f, circleRadius, pathColor)
    }

    private fun drawTokenCircle(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawCircle(cx, cy, radius, borderPaint)
    }

    private fun drawPaths(canvas: Canvas) {
        // Horizontal path (top)
        for (i in 6..8) {
            for (j in 0..5) {
                drawCell(canvas, j * cellSize, i * cellSize, pathPaint)
            }
            for (j in 9..14) {
                drawCell(canvas, j * cellSize, i * cellSize, pathPaint)
            }
        }

        // Horizontal path (bottom) - already covered by home bases
        
        // Vertical path (left)
        for (i in 6..8) {
            for (j in 0..5) {
                drawCell(canvas, i * cellSize, j * cellSize, pathPaint)
            }
            for (j in 9..14) {
                drawCell(canvas, i * cellSize, j * cellSize, pathPaint)
            }
        }

        // Middle path
        for (i in 6..8) {
            for (j in 6..8) {
                drawCell(canvas, i * cellSize, j * cellSize, pathPaint)
            }
        }
    }

    private fun drawCell(canvas: Canvas, left: Float, top: Float, paint: Paint) {
        val rect = RectF(left, top, left + cellSize, top + cellSize)
        canvas.drawRect(rect, paint)
        canvas.drawRect(rect, borderPaint)
    }

    private fun drawHomeStretches(canvas: Canvas) {
        // Red home stretch (horizontal from left to center)
        for (i in 1..5) {
            drawCell(canvas, i * cellSize, 7 * cellSize, redPaint)
        }

        // Green home stretch (vertical from top to center)
        for (i in 1..5) {
            drawCell(canvas, 7 * cellSize, i * cellSize, greenPaint)
        }

        // Yellow home stretch (horizontal from right to center)
        for (i in 9..13) {
            drawCell(canvas, i * cellSize, 7 * cellSize, yellowPaint)
        }

        // Blue home stretch (vertical from bottom to center)
        for (i in 9..13) {
            drawCell(canvas, 7 * cellSize, i * cellSize, bluePaint)
        }
    }

    private fun drawCenterHome(canvas: Canvas) {
        // Draw triangles for each color pointing to center
        val centerX = cellSize * 7.5f
        val centerY = cellSize * 7.5f
        val size = cellSize * 1.5f

        // Red triangle (pointing right)
        val redPath = Path().apply {
            moveTo(centerX - size, centerY - size)
            lineTo(centerX, centerY)
            lineTo(centerX - size, centerY + size)
            close()
        }
        canvas.drawPath(redPath, redPaint)

        // Green triangle (pointing down)
        val greenPath = Path().apply {
            moveTo(centerX - size, centerY - size)
            lineTo(centerX, centerY)
            lineTo(centerX + size, centerY - size)
            close()
        }
        canvas.drawPath(greenPath, greenPaint)

        // Yellow triangle (pointing left)
        val yellowPath = Path().apply {
            moveTo(centerX + size, centerY - size)
            lineTo(centerX, centerY)
            lineTo(centerX + size, centerY + size)
            close()
        }
        canvas.drawPath(yellowPath, yellowPaint)

        // Blue triangle (pointing up)
        val bluePath = Path().apply {
            moveTo(centerX - size, centerY + size)
            lineTo(centerX, centerY)
            lineTo(centerX + size, centerY + size)
            close()
        }
        canvas.drawPath(bluePath, bluePaint)

        // Draw center circle
        canvas.drawCircle(centerX, centerY, cellSize * 0.5f, homePaint)
        canvas.drawCircle(centerX, centerY, cellSize * 0.5f, borderPaint)
    }

    private fun drawSafeZones(canvas: Canvas) {
        // Draw stars at safe positions
        val starPositions = listOf(
            Pair(1, 8),   // Red safe
            Pair(6, 2),   // Green safe
            Pair(8, 13),  // Yellow safe
            Pair(13, 6)   // Blue safe
        )

        starPositions.forEach { (col, row) ->
            drawStar(canvas, col * cellSize + cellSize / 2, row * cellSize + cellSize / 2)
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float) {
        val radius = cellSize * 0.3f
        val path = Path()
        val points = 5

        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) radius else radius * 0.5f
            val angle = Math.PI / 2 + i * Math.PI / points
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy - (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        canvas.drawPath(path, starPaint)
    }

    private fun drawBorders(canvas: Canvas) {
        // Draw outer border
        val rect = RectF(0f, 0f, boardSize, boardSize)
        canvas.drawRoundRect(rect, 16f, 16f, borderPaint)
    }
}
