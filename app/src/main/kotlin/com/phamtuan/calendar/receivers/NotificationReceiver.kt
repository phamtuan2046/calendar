package com.phamtuan.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.phamtuan.calendar.extensions.dbHelper
import com.phamtuan.calendar.extensions.notifyEvent
import com.phamtuan.calendar.extensions.scheduleAllEvents
import com.phamtuan.calendar.extensions.updateListWidget
import com.phamtuan.calendar.helpers.EVENT_ID
import com.phamtuan.calendar.helpers.Formatter

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Simple Calendar")
        wakelock.acquire(5000)

        context.updateListWidget()
        val id = intent.getIntExtra(EVENT_ID, -1)
        if (id == -1)
            return

        val event = context.dbHelper.getEventWithId(id)
        if (event == null || event.getReminders().isEmpty())
            return

        if (!event.ignoreEventOccurrences.contains(Formatter.getDayCodeFromTS(event.startTS).toInt())) {
            context.notifyEvent(event)
        }
        context.scheduleAllEvents()
    }
}
