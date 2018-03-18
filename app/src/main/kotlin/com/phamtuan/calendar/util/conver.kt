package com.phamtuan.calendar.util

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ExpandableListView

/**
 * Created by P.Tuan on 1/23/2018.
 */

class conver : Activity() {
    internal var listView: ExpandableListView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listView!!.setOnChildClickListener { expandableListView, view, i, i1, l -> false }
    }
}
