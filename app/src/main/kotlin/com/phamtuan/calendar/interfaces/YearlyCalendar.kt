package com.phamtuan.calendar.interfaces

import android.util.SparseArray
import com.phamtuan.calendar.models.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
