package com.phamtuan.calendar.helpers

import android.content.Context
import com.phamtuan.calendar.extensions.dbHelper
import com.phamtuan.calendar.interfaces.WeeklyCalendar
import com.phamtuan.calendar.models.Event
import java.util.*

class WeeklyCalendarImpl(val mCallback: WeeklyCalendar, val mContext: Context) {
    var mEvents = ArrayList<Event>()

    fun updateWeeklyCalendar(weekStartTS: Int) {
        val startTS = weekStartTS
        val endTS = startTS + WEEK_SECONDS
        mContext.dbHelper.getEvents(startTS, endTS) {
            mEvents = it as ArrayList<Event>
            mCallback.updateWeeklyCalendar(it)
        }
    }
}
