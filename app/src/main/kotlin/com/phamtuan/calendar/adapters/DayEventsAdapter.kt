package com.phamtuan.calendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.phamtuan.calendar.R
import com.phamtuan.calendar.activities.SimpleActivity
import com.phamtuan.calendar.dialogs.DeleteEventDialog
import com.phamtuan.calendar.extensions.config
import com.phamtuan.calendar.extensions.shareEvents
import com.phamtuan.calendar.helpers.Formatter
import com.phamtuan.calendar.interfaces.DeleteEventsListener
import com.phamtuan.calendar.models.AdsItem
import com.phamtuan.calendar.models.Event
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_item_day_view.view.*

class DayEventsAdapter(activity: SimpleActivity, val datas: ArrayList<Any>, val listener: DeleteEventsListener?, recyclerView: MyRecyclerView,
                       itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    companion object {
        val TYPE_ADS = 0
        val TYPE_DATA = 1
    }

    private var allDayString = resources.getString(R.string.all_day)
    private var replaceDescriptionWithLocation = activity.config.replaceDescription

    override fun getActionMenuId() = R.menu.cab_day

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.event_item_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = datas.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MyRecyclerViewAdapter.ViewHolder {
        return when (viewType) {
            TYPE_DATA -> createViewHolder(R.layout.event_item_day_view, parent)
            else -> createViewHolder(com.simplemobiletools.commons.R.layout.item_ads_facebook_even, parent)
        }
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        if(datas[position] is Event){
            val event = datas[position] as Event
            val view = holder.bindView(event) { itemView, layoutPosition ->
                setupView(itemView, event)
            }
            bindViewHolder(holder, position, view)
        }else{
            var ad = datas[position] as AdsItem
            holder.innitAdsFace(ad.nativeAd!!, activity)
        }
    }

    override fun getItemCount() = datas.size
    override fun getItemViewType(position: Int): Int {
        return if (datas[position] is Event) {
            TYPE_DATA
        } else {
            TYPE_ADS
        }
    }

    private fun setupView(view: View, event: Event) {
        view.apply {
            event_section_title.text = event.title
            event_item_description.text = if (replaceDescriptionWithLocation) event.location else event.description
            event_item_start.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(context, event.startTS)
            event_item_end.beInvisibleIf(event.startTS == event.endTS)
            event_item_color.applyColorFilter(event.color)

            if (event.startTS != event.endTS) {
                val startCode = Formatter.getDayCodeFromTS(event.startTS)
                val endCode = Formatter.getDayCodeFromTS(event.endTS)

                event_item_end.apply {
                    text = Formatter.getTimeFromTS(context, event.endTS)
                    if (startCode != endCode) {
                        if (event.getIsAllDay()) {
                            text = Formatter.getDateFromCode(context, endCode, true)
                        } else {
                            append(" (${Formatter.getDateFromCode(context, endCode, true)})")
                        }
                    } else if (event.getIsAllDay()) {
                        beInvisible()
                    }
                }
            }

            event_item_start.setTextColor(textColor)
            event_item_end.setTextColor(textColor)
            event_section_title.setTextColor(textColor)
            event_item_description.setTextColor(textColor)
        }
    }

    private fun shareEvents() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        selectedPositions.forEach {
            var dt = datas[it] as Event
            eventIds.add(dt.id)
        }
        activity.shareEvents(eventIds.distinct())
    }

    private fun askConfirmDelete() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        val timestamps = ArrayList<Int>(selectedPositions.size)
        selectedPositions.forEach {
            var dt = datas[it] as Event
            eventIds.add(dt.id)
            timestamps.add(dt.startTS)
        }

        com.phamtuan.calendar.dialogs.DeleteEventDialog(activity, eventIds) {
            val eventsToDelete = ArrayList<Event>(selectedPositions.size)
            selectedPositions.sortedDescending().forEach {
                val event = datas[it] as Event
                eventsToDelete.add(event)
            }
            datas.removeAll(eventsToDelete)

            if (it) {
                listener?.deleteItems(eventIds)
            } else {
                listener?.addEventRepeatException(eventIds, timestamps)
            }
            removeSelectedItems()
        }
    }

    fun adFaceNative(any: AdsItem) {
        if (datas.isNotEmpty() && datas[0] is AdsItem) datas.removeAt(0)
        datas.add(0, any)
        notifyItemChanged(0)
    }
}
