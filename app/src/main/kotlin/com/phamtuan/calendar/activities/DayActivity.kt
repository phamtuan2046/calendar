package com.phamtuan.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.util.SparseIntArray
import android.view.Menu
import android.view.MenuItem
import com.phamtuan.calendar.R
import com.phamtuan.calendar.adapters.MyDayPagerAdapter
import com.phamtuan.calendar.dialogs.FilterEventTypesDialog
import com.phamtuan.calendar.extensions.config
import com.phamtuan.calendar.extensions.dbHelper
import com.phamtuan.calendar.extensions.getNewEventTimestampFromCode
import com.phamtuan.calendar.helpers.DAY_CODE
import com.phamtuan.calendar.helpers.Formatter
import com.phamtuan.calendar.helpers.NEW_EVENT_START_TS
import com.phamtuan.calendar.interfaces.NavigationListener
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.activity_day.*
import org.joda.time.DateTime
import java.util.*

class DayActivity : com.phamtuan.calendar.activities.SimpleActivity(), NavigationListener, ViewPager.OnPageChangeListener {
    private val PREFILLED_DAYS = 121
    private var mDayCode = ""
    private var mPagerDays: MutableList<String>? = null
    private var mPagerPos = 0
    private var eventTypeColors = SparseIntArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day)
        val intent = intent ?: return
        mDayCode = intent.getStringExtra(DAY_CODE)
        if (mDayCode.isEmpty())
            return

        fillViewPager(mDayCode)

        day_fab.setOnClickListener { addNewEvent() }
        updateTextColors(day_coordinator)

        dbHelper.getEventTypes {
            if (!isActivityDestroyed()) {
                eventTypeColors.clear()
                it.map { eventTypeColors.put(it.id, it.color) }
                invalidateOptionsMenu()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_day, menu)
        menu.findItem(R.id.filter).isVisible = eventTypeColors.size() > 1 || config.displayEventTypes.isEmpty()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter -> showFilterDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun fillViewPager(targetDay: String) {
        getDays(targetDay)
        val daysAdapter = MyDayPagerAdapter(supportFragmentManager, mPagerDays!!, this)
        mPagerPos = mPagerDays!!.size / 2
        view_pager.apply {
            adapter = daysAdapter
            currentItem = mPagerPos
            addOnPageChangeListener(this@DayActivity)
        }
    }

    private fun showFilterDialog() {
        com.phamtuan.calendar.dialogs.FilterEventTypesDialog(this) {
            recheckEvents()
        }
    }

    private fun addNewEvent() {
        Intent(applicationContext, com.phamtuan.calendar.activities.EventActivity::class.java).apply {
            putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(mPagerDays?.get(view_pager.currentItem).toString()))
            startActivity(this)
        }
    }

    private fun getDays(code: String) {
        mPagerDays = ArrayList(PREFILLED_DAYS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_DAYS / 2..PREFILLED_DAYS / 2) {
            mPagerDays!!.add(Formatter.getDayCodeFromDateTime(today.plusDays(i)))
        }
    }

    fun recheckEvents() {
        (view_pager.adapter as com.phamtuan.calendar.adapters.MyDayPagerAdapter).checkDayEvents(mPagerPos)
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        mPagerPos = position
        (view_pager.adapter as com.phamtuan.calendar.adapters.MyDayPagerAdapter).destroyMultiselector(position)
    }

    override fun goLeft() {
        view_pager.currentItem = view_pager.currentItem - 1
    }

    override fun goRight() {
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        fillViewPager(Formatter.getDayCodeFromDateTime(dateTime))
    }
}
