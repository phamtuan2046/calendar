package com.phamtuan.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phamtuan.calendar.extensions.notifyRunningEvents
import com.phamtuan.calendar.extensions.recheckCalDAVCalendars
import com.phamtuan.calendar.extensions.scheduleAllEvents

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, arg1: Intent) {
        context.apply {
            scheduleAllEvents()
            notifyRunningEvents()
            recheckCalDAVCalendars {}
        }
    }
}
