package com.phamtuan.calendar.wigets

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.phamtuan.calendar.R
import com.phamtuan.calendar.extensions.config
import com.phamtuan.calendar.helpers.MEDIUM_ALPHA
import com.phamtuan.calendar.models.DayYearly
import com.simplemobiletools.commons.extensions.adjustAlpha
import java.util.*

class SmallMonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private var paint: Paint
    private var todayCirclePaint: Paint
    private var dayWidth = 0f
    private var textColor = 0
    private var days = 31
    private var isLandscape = false
    private var mEvents: ArrayList<DayYearly>? = null

    var firstDay = 0
    var todaysId = 0

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    fun setDays(days: Int) {
        this.days = days
        invalidate()
    }

    fun setEvents(events: ArrayList<DayYearly>?) {
        mEvents = events
        post { invalidate() }
    }

    init {
        val attributes = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.SmallMonthView,
                0, 0)

        try {
            days = attributes.getInt(R.styleable.SmallMonthView_days, 31)
        } finally {
            attributes.recycle()
        }

        val baseColor = context.config.textColor
        textColor = baseColor.adjustAlpha(MEDIUM_ALPHA)

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = resources.getDimensionPixelSize(R.dimen.year_view_day_text_size).toFloat()
            textAlign = Paint.Align.RIGHT
        }

        todayCirclePaint = Paint(paint)
        todayCirclePaint.color = context.config.primaryColor.adjustAlpha(MEDIUM_ALPHA)
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dayWidth == 0f) {
            dayWidth = if (isLandscape) {
                (canvas.width / 9).toFloat()
            } else {
                (canvas.width / 7).toFloat()
            }
        }

        var curId = 1 - firstDay
        for (y in 1..6) {
            for (x in 1..7) {
                if (curId in 1..days) {
                    canvas.drawText(curId.toString(), x * dayWidth, y * dayWidth, getPaint(curId))

                    if (curId == todaysId) {
                        val dividerConstant = if (isLandscape) 6 else 4
                        canvas.drawCircle(x * dayWidth - dayWidth / dividerConstant, y * dayWidth - dayWidth / dividerConstant, dayWidth * 0.41f, todayCirclePaint)
                    }
                }
                curId++
            }
        }
    }

    private fun getPaint(curId: Int): Paint {
        val colors = mEvents?.get(curId)?.eventColors ?: HashSet()
        if (colors.isNotEmpty()) {
            val curPaint = Paint(paint)
            curPaint.color = colors.first()
            return curPaint
        }

        return paint
    }
}
