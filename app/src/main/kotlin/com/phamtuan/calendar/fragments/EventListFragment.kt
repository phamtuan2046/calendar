package com.phamtuan.calendar.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.NativeAd
import com.phamtuan.calendar.R
import com.phamtuan.calendar.adapters.EventListAdapter
import com.phamtuan.calendar.extensions.config
import com.phamtuan.calendar.extensions.dbHelper
import com.phamtuan.calendar.extensions.getFilteredEvents
import com.phamtuan.calendar.extensions.seconds
import com.phamtuan.calendar.helpers.EVENT_ID
import com.phamtuan.calendar.helpers.EVENT_OCCURRENCE_TS
import com.phamtuan.calendar.helpers.Formatter
import com.phamtuan.calendar.interfaces.DeleteEventsListener
import com.phamtuan.calendar.models.*
import com.phamtuan.calendar.util.Constance
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime
import java.util.*

class EventListFragment : Fragment(), DeleteEventsListener {
    private var mEvents: List<Event> = ArrayList()
    private var prevEventsHash = 0
    private var lastHash = 0
    lateinit var mView: View
    private var faceAdsNative: NativeAd? = null
    private var eventsAdapter: EventListAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_event_list, container, false)
        val placeholderText = String.format(getString(R.string.two_string_placeholder), "${getString(R.string.no_upcoming_events)}\n", getString(R.string.add_some_events))
        mView.calendar_empty_list_placeholder.text = placeholderText
        return mView
    }


    override fun onResume() {
        super.onResume()
        checkEvents()
        initFaceAds()
    }

    private fun checkEvents() {
        val fromTS = DateTime().seconds() - context!!.config.displayPastEvents * 60
        val toTS = DateTime().plusYears(1).seconds()
        context!!.dbHelper.getEvents(fromTS, toTS) {
            receivedEvents(it)
        }
    }

    fun initFaceAds() {
        faceAdsNative = NativeAd(context, Constance.FACEBOOK_NATIVE_ID_2)
        faceAdsNative?.setAdListener(object : AdListener {
            override fun onAdClicked(p0: Ad?) {
                Log.d("faceAdsNative", "onAdClicked")
            }

            override fun onError(p0: Ad?, p1: AdError?) {
                Log.d("faceAdsNative", "onError")
            }

            override fun onAdLoaded(p0: Ad?) {
                eventsAdapter?.adFaceNative(AdsItem(faceAdsNative))
            }

            override fun onLoggingImpression(p0: Ad?) {
                Log.d("faceAdsNative", "onLoggingImpression")
            }
        })
        faceAdsNative?.loadAd()
    }

    private fun receivedEvents(events: MutableList<Event>) {
        if (context == null || activity == null)
            return

        val newHash = events.hashCode()
        if (newHash == lastHash) {
            return
        }
        lastHash = newHash

        val filtered = context!!.getFilteredEvents(events)
        val hash = filtered.hashCode()
        if (prevEventsHash == hash)
            return

        prevEventsHash = hash
        mEvents = filtered
        val listItems = ArrayList<ListItem>(mEvents.size)
        val replaceDescription = context!!.config.replaceDescription
        val sorted = mEvents.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { if (replaceDescription) it.location else it.description }))
        val sublist = sorted.subList(0, Math.min(sorted.size, 100))
        var prevCode = ""
        sublist.forEach {
            val code = Formatter.getDayCodeFromTS(it.startTS)
            if (code != prevCode) {
                val day = Formatter.getDayTitle(context!!, code)
                listItems.add(ListSection(day))
                prevCode = code
            }
            listItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description, it.getIsAllDay(), it.color, it.location))
        }

        eventsAdapter = EventListAdapter(activity as com.phamtuan.calendar.activities.SimpleActivity, listItems, true, this, mView.calendar_events_list) {
            if (it is ListEvent) {
                editEvent(it)
            }
        }

        activity?.runOnUiThread {
            mView.calendar_events_list.apply {
                this@apply.adapter = eventsAdapter
            }
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        mView.calendar_empty_list_placeholder.beVisibleIf(mEvents.isEmpty())
        mView.calendar_events_list.beGoneIf(mEvents.isEmpty())
        if (activity != null)
            mView.calendar_empty_list_placeholder.setTextColor(activity!!.config.textColor)
    }

    private fun editEvent(event: ListEvent) {
        Intent(context, com.phamtuan.calendar.activities.EventActivity::class.java).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            startActivity(this)
        }
    }

    override fun deleteItems(ids: ArrayList<Int>) {
        val eventIDs = Array(ids.size, { i -> (ids[i].toString()) })
        context!!.dbHelper.deleteEvents(eventIDs, true)
        checkEvents()
    }

    override fun addEventRepeatException(parentIds: ArrayList<Int>, timestamps: ArrayList<Int>) {
        parentIds.forEachIndexed { index, value ->
            context!!.dbHelper.addEventRepeatException(value, timestamps[index])
        }
        checkEvents()
    }
}
