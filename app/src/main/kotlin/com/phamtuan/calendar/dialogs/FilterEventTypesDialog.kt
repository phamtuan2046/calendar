package com.phamtuan.calendar.dialogs

import android.support.v7.app.AlertDialog
import com.phamtuan.calendar.R
import com.phamtuan.calendar.activities.SimpleActivity
import com.phamtuan.calendar.adapters.FilterEventTypeAdapter
import com.phamtuan.calendar.extensions.config
import com.phamtuan.calendar.extensions.dbHelper
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_filter_event_types.view.*

class FilterEventTypesDialog(val activity: com.phamtuan.calendar.activities.SimpleActivity, val callback: () -> Unit) {
    var dialog: AlertDialog
    val view = activity.layoutInflater.inflate(R.layout.dialog_filter_event_types, null)

    init {
        val eventTypes = activity.dbHelper.fetchEventTypes()
        val displayEventTypes = activity.config.displayEventTypes
        view.filter_event_types_list.adapter = com.phamtuan.calendar.adapters.FilterEventTypeAdapter(activity, eventTypes, displayEventTypes)

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialogInterface, i -> confirmEventTypes() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.filter_events_by_type)
        }
    }

    private fun confirmEventTypes() {
        val selectedItems = (view.filter_event_types_list.adapter as com.phamtuan.calendar.adapters.FilterEventTypeAdapter).getSelectedItemsSet()
        if (activity.config.displayEventTypes != selectedItems) {
            activity.config.displayEventTypes = selectedItems
            callback()
        }
        dialog.dismiss()
    }
}
