package com.phamtuan.calendar.fragments

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.RelativeLayout
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.NativeAd
import com.phamtuan.calendar.R
import com.phamtuan.calendar.activities.DayActivity
import com.phamtuan.calendar.activities.EventActivity
import com.phamtuan.calendar.activities.SimpleActivity
import com.phamtuan.calendar.adapters.DayEventsAdapter
import com.phamtuan.calendar.extensions.config
import com.phamtuan.calendar.extensions.dbHelper
import com.phamtuan.calendar.extensions.getAppropriateTheme
import com.phamtuan.calendar.extensions.getFilteredEvents
import com.phamtuan.calendar.helpers.DAY_CODE
import com.phamtuan.calendar.helpers.EVENT_ID
import com.phamtuan.calendar.helpers.EVENT_OCCURRENCE_TS
import com.phamtuan.calendar.helpers.Formatter
import com.phamtuan.calendar.interfaces.DeleteEventsListener
import com.phamtuan.calendar.interfaces.NavigationListener
import com.phamtuan.calendar.models.AdsItem
import com.phamtuan.calendar.models.Event
import com.phamtuan.calendar.util.Constance
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.fragment_day.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

class DayFragment : Fragment(), DeleteEventsListener {
    var mListener: NavigationListener? = null
    private var mTextColor = 0
    private var mDayCode = ""
    private var lastHash = 0

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    private var datas = ArrayList<Any>()
    private var faceAdsNative: NativeAd? = null
    private var dayEventsAdapter: DayEventsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_day, container, false)
        mRes = resources
        mHolder = view.day_holder

        mDayCode = arguments!!.getString(DAY_CODE)
        val day = Formatter.getDayTitle(context!!, mDayCode)
        mHolder.top_value.apply {
            text = day
            setOnClickListener { pickDay() }
            setTextColor(context.config.textColor)
        }

        setupButtons()
        return view
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
    }

    private fun setupButtons() {
        mTextColor = context!!.config.textColor

        mHolder.apply {
            top_left_arrow.applyColorFilter(mTextColor)
            top_right_arrow.applyColorFilter(mTextColor)
            top_left_arrow.background = null
            top_right_arrow.background = null

            top_left_arrow.setOnClickListener {
                mListener?.goLeft()
            }

            top_right_arrow.setOnClickListener {
                mListener?.goRight()
            }
        }
    }

    fun getDayEventsAdapter() = mHolder.day_events?.adapter as? DayEventsAdapter

    private fun pickDay() {
        activity!!.setTheme(context!!.getAppropriateTheme())
        val view = layoutInflater.inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById<DatePicker>(R.id.date_picker)

        val dateTime = Formatter.getDateTimeFromCode(mDayCode)
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        AlertDialog.Builder(context!!)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { dialog, which -> positivePressed(dateTime, datePicker) }
                .create().apply {
            activity?.setupDialogStuff(view, this)
        }
    }

    private fun positivePressed(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val day = datePicker.dayOfMonth
        val newDateTime = dateTime.withDate(year, month, day)
        mListener?.goToDateTime(newDateTime)
    }

    fun checkEvents() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        context!!.dbHelper.getEvents(startTS, endTS) {
            receivedEvents(it)
        }
    }

    private fun receivedEvents(events: List<Event>) {
        val filtered = context?.getFilteredEvents(events) ?: ArrayList()
        val newHash = filtered.hashCode()
        if (newHash == lastHash || !isAdded) {
            return
        }
        lastHash = newHash

        val replaceDescription = context!!.config.replaceDescription
        val sorted = ArrayList<Event>(filtered.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, {
            if (replaceDescription) it.location else it.description
        })))

        activity?.runOnUiThread {
            updateEvents(sorted)
        }
    }

    private fun updateEvents(events: ArrayList<Event>) {
        if (activity == null)
            return
        datas.clear()
        datas.addAll(events)
        initFaceAds()
        dayEventsAdapter = DayEventsAdapter(activity as SimpleActivity, datas, this, mHolder.day_events) {
            editEvent(it as Event)
        }.apply {
            setupDragListener(true)
            addVerticalDividers(true)
            mHolder.day_events.adapter = this
        }
    }

    private fun editEvent(event: Event) {
        Intent(context, EventActivity::class.java).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            startActivity(this)
        }
    }

    fun initFaceAds() {
        faceAdsNative = NativeAd(context, Constance.FACEBOOK_NATIVE_ID_3)
        faceAdsNative?.setAdListener(object : AdListener {
            override fun onAdClicked(p0: Ad?) {
                Log.d("faceAdsNative", "onAdClicked")
            }

            override fun onError(p0: Ad?, p1: AdError?) {
                Log.d("faceAdsNative", "onError")
            }

            override fun onAdLoaded(p0: Ad?) {
                dayEventsAdapter?.adFaceNative(AdsItem(faceAdsNative))
            }

            override fun onLoggingImpression(p0: Ad?) {
                Log.d("faceAdsNative", "onLoggingImpression")
            }
        })
        faceAdsNative?.loadAd()
    }

    override fun deleteItems(ids: ArrayList<Int>) {
        val eventIDs = Array(ids.size, { i -> (ids[i].toString()) })
        context!!.dbHelper.deleteEvents(eventIDs, true)
    }

    override fun addEventRepeatException(parentIds: ArrayList<Int>, timestamps: ArrayList<Int>) {
        parentIds.forEachIndexed { index, value ->
            context!!.dbHelper.addEventRepeatException(parentIds[index], timestamps[index])
        }
        (activity as DayActivity).recheckEvents()
    }
}
