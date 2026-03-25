package com.zuvy.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.zuvy.app.R
import kotlin.math.min

/**
 * Swipe gesture helper for RecyclerView items
 * Supports swipe left and right with customizable actions
 */
class SwipeGestureHelper(
    context: Context,
    private val onSwipeLeft: (Int) -> Unit,
    private val onSwipeRight: (Int) -> Unit,
    private val leftSwipeLabel: String = "Delete",
    private val rightSwipeLabel: String = "Add to Queue"
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteBackgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.error)
    }
    
    private val addToQueueBackgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.success)
    }
    
    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        textSize = 42f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        when (direction) {
            ItemTouchHelper.LEFT -> onSwipeLeft(position)
            ItemTouchHelper.RIGHT -> onSwipeRight(position)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val background = RectF()
        
        // Calculate background rectangle
        if (dX > 0) {
            // Swiping right - Add to Queue
            background.left = itemView.left.toFloat()
            background.top = itemView.top.toFloat()
            background.right = itemView.left + dX
            background.bottom = itemView.bottom.toFloat()
            c.drawRect(background, addToQueueBackgroundPaint)
            
            // Draw text
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textBottom = background.top + (background.height() + textHeight) / 2
            c.drawText(
                rightSwipeLabel,
                background.left + background.width() / 2,
                textBottom,
                textPaint
            )
        } else if (dX < 0) {
            // Swiping left - Delete
            background.left = itemView.right + dX
            background.top = itemView.top.toFloat()
            background.right = itemView.right.toFloat()
            background.bottom = itemView.bottom.toFloat()
            c.drawRect(background, deleteBackgroundPaint)
            
            // Draw text
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textBottom = background.top + (background.height() + textHeight) / 2
            c.drawText(
                leftSwipeLabel,
                background.left + background.width() / 2,
                textBottom,
                textPaint
            )
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.3f
    
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 2

    companion object {
        /**
         * Attach swipe gestures to a RecyclerView
         */
        fun attachToRecyclerView(
            recyclerView: RecyclerView,
            context: Context,
            onSwipeLeft: (Int) -> Unit,
            onSwipeRight: (Int) -> Unit,
            leftSwipeLabel: String = "Delete",
            rightSwipeLabel: String = "Add to Queue"
        ): SwipeGestureHelper {
            val helper = SwipeGestureHelper(
                context,
                onSwipeLeft,
                onSwipeRight,
                leftSwipeLabel,
                rightSwipeLabel
            )
            ItemTouchHelper(helper).attachToRecyclerView(recyclerView)
            return helper
        }
    }
}
