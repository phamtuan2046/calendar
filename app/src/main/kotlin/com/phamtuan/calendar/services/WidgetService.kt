package com.phamtuan.calendar.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.phamtuan.calendar.adapters.EventListWidgetAdapter

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = com.phamtuan.calendar.adapters.EventListWidgetAdapter(applicationContext)
}
