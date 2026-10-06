package com.happyhouse.photo.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class OrganizeCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var labels: List<String> = List(SLOT_COUNT) { "" }
        set(value) {
            field = value
            invalidate()
        }

    var photo: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    var onFilePhoto: ((Int) -> Unit)? = null
    var onChooseSlot: ((Int) -> Unit)? = null

    private var dragX = 0f
    private var dragY = 0f
    private var dragging = false
    private var highlight = -1

    private val wedgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arc = RectF()

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.36f
        val stroke = radius * 0.55f
        wedgePaint.strokeWidth = stroke
        val arcRadius = radius * 0.78f
        arc.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)
        textPaint.textSize = resources.displayMetrics.density * 12f

        for (index in 0 until SLOT_COUNT) {
            val start = -90f - SWEEP / 2f + index * 45f
            wedgePaint.color = if (index == highlight) 0xFF1F3A5F.toInt() else 0xFFE6EAF0.toInt()
            canvas.drawArc(arc, start, SWEEP, false, wedgePaint)
            val mid = Math.toRadians((start + SWEEP / 2f).toDouble())
            val label = labels.getOrElse(index) { "" }.ifBlank { context.getString(R.string.set_album) }
            textPaint.color = if (index == highlight) 0xFFFFFFFF.toInt() else 0xFF1F3A5F.toInt()
            val shown = if (label.length > 12) label.take(11) + "…" else label
            canvas.drawText(
                shown,
                cx + (cos(mid) * arcRadius).toFloat(),
                cy + (sin(mid) * arcRadius).toFloat() + textPaint.textSize / 3f,
                textPaint
            )
        }

        val bitmap = photo ?: return
        val photoRadius = radius * 0.62f
        val left = cx + dragX - photoRadius
        val top = cy + dragY - photoRadius
        val dest = RectF(left, top, left + photoRadius * 2, top + photoRadius * 2)
        canvas.save()
        canvas.clipPath(android.graphics.Path().apply {
            addCircle(cx + dragX, cy + dragY, photoRadius, android.graphics.Path.Direction.CW)
        })
        canvas.drawBitmap(bitmap, null, dest, photoPaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val dx = event.x - cx
        val dy = event.y - cy
        val radius = min(width, height) * 0.36f
        val inner = radius * 0.7f
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = hypot(dx, dy) <= inner && photo != null
                if (dragging) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    dragX = dx
                    dragY = dy
                    highlight = slotAt(dx, dy, radius, radius * 0.9f) ?: -1
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return true
                dragX = dx
                dragY = dy
                highlight = slotAt(dx, dy, radius, radius * 0.9f) ?: -1
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val fileSlot = slotAt(dx, dy, radius, radius * 0.9f)
                val chooseSlot = slotAt(dx, dy, radius, radius * 0.5f)
                if (dragging && event.actionMasked == MotionEvent.ACTION_UP && fileSlot != null) {
                    onFilePhoto?.invoke(fileSlot)
                } else if (!dragging && event.actionMasked == MotionEvent.ACTION_UP && chooseSlot != null) {
                    onChooseSlot?.invoke(chooseSlot)
                }
                dragging = false
                dragX = 0f
                dragY = 0f
                highlight = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun slotAt(dx: Float, dy: Float, radius: Float, minDistance: Float): Int? {
        if (hypot(dx, dy) < minDistance) return null
        val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        val fromTop = (degrees + 90.0 + 360.0) % 360.0
        val index = ((fromTop + 22.5) / 45.0).toInt() % SLOT_COUNT
        return index
    }

    companion object {
        const val SLOT_COUNT = 8
        private const val SWEEP = 36f
    }
}
