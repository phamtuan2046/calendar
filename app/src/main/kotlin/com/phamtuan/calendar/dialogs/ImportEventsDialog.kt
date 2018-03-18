package com.phamtuan.calendar.dialogs

import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import com.phamtuan.calendar.R
import com.phamtuan.calendar.activities.SimpleActivity
import com.phamtuan.calendar.extensions.config
import com.phamtuan.calendar.extensions.dbHelper
import com.phamtuan.calendar.helpers.DBHelper
import com.phamtuan.calendar.helpers.IcsImporter
import com.phamtuan.calendar.helpers.IcsImporter.ImportResult.*
import com.simplemobiletools.commons.extensions.setBackgroundWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import kotlinx.android.synthetic.main.dialog_import_events.view.*

class ImportEventsDialog(val activity: com.phamtuan.calendar.activities.SimpleActivity, val path: String, val callback: (refreshView: Boolean) -> Unit) {
    var currEventTypeId = DBHelper.REGULAR_EVENT_TYPE_ID

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_import_events, null) as ViewGroup).apply {
            updateEventType(this)
            import_event_type_holder.setOnClickListener {
                com.phamtuan.calendar.dialogs.SelectEventTypeDialog(activity, currEventTypeId) {
                    currEventTypeId = it
                    updateEventType(this)
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.import_events) {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    activity.toast(R.string.importing)
                    Thread {
                        val result = IcsImporter(activity).importEvents(path, currEventTypeId)
                        handleParseResult(result)
                        dismiss()
                    }.start()
                }
            }
        }
    }

    private fun updateEventType(view: ViewGroup) {
        val eventType = activity.dbHelper.getEventType(currEventTypeId)
        view.import_event_type_title.text = eventType!!.getDisplayTitle()
        view.import_event_type_color.setBackgroundWithStroke(eventType.color, activity.config.backgroundColor)
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        activity.toast(when (result) {
            IMPORT_OK -> R.string.events_imported_successfully
            IMPORT_PARTIAL -> R.string.importing_some_events_failed
            else -> R.string.importing_events_failed
        })
        callback(result != IMPORT_FAIL)
    }
}
