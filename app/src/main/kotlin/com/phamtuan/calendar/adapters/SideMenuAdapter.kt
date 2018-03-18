package com.phamtuan.calendar.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import com.phamtuan.calendar.R
import com.phamtuan.calendar.models.GroupMenu

/**
 * Created by P.Tuan on 12/28/2017.
 */
class SideMenuAdapter(context: Context,var menus:ArrayList<GroupMenu>) : BaseExpandableListAdapter()  {
    var layoutInflater: LayoutInflater? = null
    init {
        layoutInflater = LayoutInflater.from(context)
    }
    override fun getChildrenCount(p0: Int): Int {
        return menus[p0].sideMenus.size
    }

    override fun getGroupCount(): Int {
        return menus.size
    }

    override fun getGroupView(p0: Int, p1: Boolean, convertView: View?, p3: ViewGroup?): View {

        return layoutInflater!!.inflate(R.layout.layout_header_menu,p3,false)
    }

    override fun getChildView(p0: Int, p1: Int, p2: Boolean, convertView: View?, p4: ViewGroup?): View {
        return layoutInflater!!.inflate(R.layout.item_child_menu,p4,false)
    }

    override fun getGroup(p0: Int): Any {
        return menus[p0]
    }

    override fun getChildId(p0: Int, p1: Int): Long {
       return menus[p0].sideMenus.size.toLong()
    }

    override fun getChild(p0: Int, p1: Int): Any {
        return menus[p0].sideMenus[p1]
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return false
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupId(p0: Int): Long {
        return 1
    }
}