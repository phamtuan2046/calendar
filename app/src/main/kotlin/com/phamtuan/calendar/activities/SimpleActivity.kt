package com.phamtuan.calendar.activities

import com.phamtuan.calendar.R
import com.phamtuan.calendar.extensions.getFormattedMinutes
import com.phamtuan.calendar.extensions.getRepetitionText
import com.phamtuan.calendar.helpers.DAY
import com.phamtuan.calendar.helpers.MONTH
import com.phamtuan.calendar.helpers.WEEK
import com.phamtuan.calendar.helpers.YEAR
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.models.RadioItem
import java.util.TreeSet
import kotlin.collections.ArrayList

open class SimpleActivity : BaseSimpleActivity() {
    protected fun showEventReminderDialog(curMinutes: Int, callback: (minutes: Int) -> Unit) {
        hideKeyboard()
        val minutes = TreeSet<Int>()
        minutes.apply {
            add(-1)
            add(0)
            add(10)
            add(30)
            add(curMinutes)
        }

        val items = ArrayList<RadioItem>(minutes.size + 1)
        minutes.mapIndexedTo(items, { index, value ->
            RadioItem(index, getFormattedMinutes(value), value)
        })

        var selectedIndex = 0
        minutes.forEachIndexed { index, value ->
            if (value == curMinutes)
                selectedIndex = index
        }

        items.add(RadioItem(-2, getString(R.string.custom)))

        RadioGroupDialog(this, items, selectedIndex) {
            if (it == -2) {
                com.phamtuan.calendar.dialogs.CustomEventReminderDialog(this) {
                    callback(it)
                }
            } else {
                callback(it as Int)
            }
        }
    }

    protected fun showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
        hideKeyboard()
        val seconds = TreeSet<Int>()
        seconds.apply {
            add(0)
            add(DAY)
            add(WEEK)
            add(MONTH)
            add(YEAR)
            add(curSeconds)
        }

        val items = ArrayList<RadioItem>(seconds.size + 1)
        seconds.mapIndexedTo(items, { index, value ->
            RadioItem(index, getRepetitionText(value), value)
        })

        var selectedIndex = 0
        seconds.forEachIndexed { index, value ->
            if (value == curSeconds)
                selectedIndex = index
        }

        items.add(RadioItem(-1, getString(R.string.custom)))

        RadioGroupDialog(this, items, selectedIndex) {
            if (it == -1) {
                com.phamtuan.calendar.dialogs.CustomEventRepeatIntervalDialog(this) {
                    callback(it)
                }
            } else {
                callback(it as Int)
            }
        }
    }
}
