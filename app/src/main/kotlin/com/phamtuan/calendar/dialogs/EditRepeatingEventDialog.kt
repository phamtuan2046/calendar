package com.phamtuan.calendar.dialogs

import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.view.WindowManager
import com.phamtuan.calendar.R
import com.phamtuan.calendar.activities.SimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_edit_repeating_event.view.*

class EditRepeatingEventDialog(val activity: com.phamtuan.calendar.activities.SimpleActivity, val callback: (allOccurrences: Boolean) -> Unit) {
    var dialog: AlertDialog

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_edit_repeating_event, null) as ViewGroup).apply {
            edit_repeating_event_one_only.setOnClickListener { sendResult(false) }
            edit_repeating_event_all_occurrences.setOnClickListener { sendResult(true) }
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this) {
                window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            }
        }
    }

    private fun sendResult(allOccurrences: Boolean) {
        callback(allOccurrences)
        dialog.dismiss()
    }
}
