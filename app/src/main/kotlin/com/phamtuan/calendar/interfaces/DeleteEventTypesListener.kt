package com.phamtuan.calendar.interfaces

import com.phamtuan.calendar.models.EventType
import java.util.*

interface DeleteEventTypesListener {
    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean)
}
