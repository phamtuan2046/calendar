package com.phamtuan.calendar.interfaces

import android.content.Context
import com.phamtuan.calendar.models.DayMonthly

interface MonthlyCalendar {
    fun updateMonthlyCalendar(context: Context, month: String, days: List<DayMonthly>, checkedEvents: Boolean)
}
