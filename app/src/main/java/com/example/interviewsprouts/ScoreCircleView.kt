package com.example.interviewsprouts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class ScoreCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var score: Int = 0

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D5E9FF")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B7CFF")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    fun setScore(value: Int) {
        score = value.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val stroke = size * 0.10f
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
        scorePaint.textSize = size * 0.20f

        val padding = stroke / 2f + 4f
        val rect = RectF(padding, padding, size - padding, size - padding)
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        canvas.save()
        canvas.translate(left, top)
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        canvas.drawArc(rect, -90f, 360f * score / 100f, false, progressPaint)
        val y = size / 2f - (scorePaint.descent() + scorePaint.ascent()) / 2f
        canvas.drawText("$score%", size / 2f, y, scorePaint)
        canvas.restore()
    }
}
