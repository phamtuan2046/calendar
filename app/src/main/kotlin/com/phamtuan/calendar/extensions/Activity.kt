package com.phamtuan.calendar.extensions

import com.phamtuan.calendar.BuildConfig
import com.phamtuan.calendar.R
import com.phamtuan.calendar.helpers.IcsExporter
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getFilePublicUri
import com.simplemobiletools.commons.extensions.shareUri
import com.simplemobiletools.commons.extensions.toast
import java.io.File

fun BaseSimpleActivity.shareEvents(ids: List<Int>) {
    val file = getTempFile()
    if (file == null) {
        toast(R.string.unknown_error_occurred)
        return
    }

    val events = dbHelper.getEventsWithIds(ids)
    IcsExporter().exportEvents(this, file, events) {
        if (it == IcsExporter.ExportResult.EXPORT_OK) {
            val uri = getFilePublicUri(file, BuildConfig.APPLICATION_ID)
            shareUri(uri, BuildConfig.APPLICATION_ID)
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}
