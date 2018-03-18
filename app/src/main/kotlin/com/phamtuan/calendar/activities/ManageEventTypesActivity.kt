package com.phamtuan.calendar.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.phamtuan.calendar.R
import com.phamtuan.calendar.adapters.ManageEventTypesAdapter
import com.phamtuan.calendar.dialogs.UpdateEventTypeDialog
import com.phamtuan.calendar.extensions.dbHelper
import com.phamtuan.calendar.interfaces.DeleteEventTypesListener
import com.phamtuan.calendar.models.EventType
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.activity_manage_event_types.*
import java.util.*

class ManageEventTypesActivity : com.phamtuan.calendar.activities.SimpleActivity(), DeleteEventTypesListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event_types)

        getEventTypes()
        updateTextColors(manage_event_types_list)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        com.phamtuan.calendar.dialogs.UpdateEventTypeDialog(this, eventType?.copy()) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        dbHelper.getEventTypes {
            runOnUiThread {
                val adapter = com.phamtuan.calendar.adapters.ManageEventTypesAdapter(this, it, this, manage_event_types_list) {
                    showEventTypeDialog(it as EventType)
                }
                adapter.setupDragListener(true)
                manage_event_types_list.adapter = adapter
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_event_types, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_event_type -> showEventTypeDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean) {
        if (eventTypes.any { it.caldavCalendarId != 0 }) {
            toast(R.string.unsync_caldav_calendar)
        }

        dbHelper.deleteEventTypes(eventTypes, deleteEvents) {
            if (it == 0) {
                toast(R.string.unknown_error_occurred)
            }
        }
    }
}
