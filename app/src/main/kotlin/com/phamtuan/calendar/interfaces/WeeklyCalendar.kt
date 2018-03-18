package com.phamtuan.calendar.interfaces

import com.phamtuan.calendar.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
