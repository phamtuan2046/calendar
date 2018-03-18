package com.phamtuan.calendar.activities

import android.app.PendingIntent.getActivity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.ActionBarDrawerToggle
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAdListener
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.phamtuan.calendar.BuildConfig
import com.phamtuan.calendar.R
import com.phamtuan.calendar.adapters.MainMenuAdapter
import com.phamtuan.calendar.adapters.MyMonthPagerAdapter
import com.phamtuan.calendar.adapters.MyWeekPagerAdapter
import com.phamtuan.calendar.adapters.SideMenuAdapter
import com.phamtuan.calendar.dialogs.DialogRateApp
import com.phamtuan.calendar.extensions.*
import com.phamtuan.calendar.fragments.EventListFragment
import com.phamtuan.calendar.fragments.WeekFragment
import com.phamtuan.calendar.helpers.*
import com.phamtuan.calendar.helpers.Formatter
import com.phamtuan.calendar.interfaces.NavigationListener
import com.phamtuan.calendar.models.Event
import com.phamtuan.calendar.models.EventType
import com.phamtuan.calendar.models.GroupMenu
import com.phamtuan.calendar.models.SideMenuItem
import com.phamtuan.calendar.presenters.MainPresenter
import com.phamtuan.calendar.util.Constance
import com.phamtuan.calendar.util.DimensionUtils
import com.phamtuan.calendar.util.Utils
import com.phamtuan.calendar.views.MainView
import com.phamtuan.calendar.wigets.MyScrollView
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.joda.time.DateTime
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : SimpleActivity(), NavigationListener, MainView, NavigationView.OnNavigationItemSelectedListener, InterstitialAdListener {

    private var isFabCreateEventShowing = true
    private val TAG = MainActivity::class.java.name

    // QUẢNG CÁO FACEBOOK
    private var interstitialAdFace: com.facebook.ads.InterstitialAd? = null

    private val CALDAV_SYNC_DELAY = 2000L
    private val PREFILLED_MONTHS = 97
    private val PREFILLED_YEARS = 31
    private val PREFILLED_WEEKS = 61

    private var mIsMonthSelected = false
    private var mStoredUseEnglish = false
    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredIsSundayFirst = false
    private var mStoredUse24HourFormat = false
    private var mShouldFilterBeVisible = false
    private var mCalDAVSyncHandler = Handler()

    private var mDefaultWeeklyPage = 0
    private var mDefaultMonthlyPage = 0
    private var mDefaultYearlyPage = 0
    private var menus = ArrayList<GroupMenu>()
    private var menuAdapter: MainMenuAdapter? = null
    var mInterstitialAd: InterstitialAd? = null

    private var handlerFab = Handler()
    private val widthFabCreate = DimensionUtils.dpToPx(56f)
    private val marginFabCreate = DimensionUtils.dpToPx(16f)

    companion object {
        var mWeekScrollY = 0
        var eventTypeColors = SparseIntArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.custom_app_theme)
        setContentView(R.layout.activity_main)
        MobileAds.initialize(this,
                resources.getString(R.string.app_admod_id))
        appLaunched()
        calendar_fab.setOnClickListener { launchNewEventIntent() }
        calendar_fab.setColors(resources.getColor(R.color.white),baseConfig.primaryColor,resources.getColor(R.color.white))
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

//        nav_view.setNavigationItemSelectedListener(this)
        initSideMenu()
        if (resources.getBoolean(R.bool.portrait_only))
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri.authority == "com.android.calendar") {
                // clicking date on a third party widget: content://com.android.calendar/time/1507309245683
                if (intent?.extras?.getBoolean("DETAIL_VIEW", false) == true) {
                    val timestamp = uri.pathSegments.last()
                    if (timestamp.areDigitsOnly()) {
                        openDayAt(timestamp.toLong())
                        return
                    }
                }
            } else {
                tryImportEventsFromFile(uri)
            }
        }
        initAdGoogle()
        storeStateVariables()
        updateViewPager()

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        recheckCalDAVCalendars {}

        if (config.googleSync) {
            val ids = dbHelper.getGoogleSyncEvents().map { it.id.toString() }.toTypedArray()
            dbHelper.deleteEvents(ids, false)
            config.googleSync = false
        }

        checkOpenIntents()
        runnableAnimation.weakFab = WeakReference(calendar_fab)
    }


    override fun onResume() {
        super.onResume()
        if (mStoredUseEnglish != config.useEnglish) {
            restartActivity()
            return
        }

        if (mStoredTextColor != config.textColor || mStoredBackgroundColor != config.backgroundColor || mStoredPrimaryColor != config.primaryColor
                || mStoredDayCode != Formatter.getTodayCode()) {
            updateViewPager()
        }

        dbHelper.getEventTypes {
            com.phamtuan.calendar.activities.MainActivity.Companion.eventTypeColors.clear()
            it.map { com.phamtuan.calendar.activities.MainActivity.Companion.eventTypeColors.put(it.id, it.color) }
            mShouldFilterBeVisible = com.phamtuan.calendar.activities.MainActivity.Companion.eventTypeColors.size() > 1 || config.displayEventTypes.isEmpty()
            invalidateOptionsMenu()
        }

        storeStateVariables()
        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredIsSundayFirst != config.isSundayFirst || mStoredUse24HourFormat != config.use24hourFormat) {
                fillWeeklyViewPager()
            }
        }

        updateWidgets()
        updateTextColors(calendar_coordinator)
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onStop() {
        super.onStop()
        mCalDAVSyncHandler.removeCallbacksAndMessages(null)
        contentResolver.unregisterContentObserver(calDAVSyncObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.apply {
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
            findItem(R.id.go_to_today).isVisible = shouldGoToTodayBeVisible()
            findItem(R.id.refresh_caldav_calendars).isVisible = config.caldavSync
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
//        currentIdSelect = item.itemId
        when (item.itemId) {
            R.id.nav_add_holidays -> {
                addHolidays()
            }
            R.id.nav_add_birthdays -> {
                tryAddBirthdays()
            }
            R.id.nav_add_anniversaries -> {
                tryAddAnniversaries()
            }
            R.id.nav_import -> {
                tryImportEvents()
            }
            R.id.nav_export -> {
                tryExportEvents()
            }
            R.id.nav_setting -> {
                launchSettings()
            }
            R.id.nav_rate_app -> {
                AppUtil.gotoPlaystore(this)
            }
            R.id.nav_about -> {
                launchAbout()
            }
            R.id.nav_exit -> {
                exit()
            }
            R.id.nav_ads -> {
                initAdGoogle()
            }
            R.id.nav_more_app -> {
                AppUtil.gotoStore(this)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.change_view -> showViewDialog()
            R.id.go_to_today -> goToToday()
            R.id.filter -> showFilterDialog()
            R.id.refresh_caldav_calendars -> refreshCalDAVCalendars()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        if (mIsMonthSelected && config.storedView == YEARLY_VIEW) {
            updateView(YEARLY_VIEW)
        } else {
            finish()
        }
    }

    private fun showDialogRateApp() {
        val transaction = supportFragmentManager.beginTransaction()
        var dialog = DialogRateApp.newIntacnce(this, { dialog ->
            dialog.dismiss()
            finish()
        }, { dialog ->
            AppUtil.gotoPlaystore(this)
        })
        transaction.add(dialog, "rateapp")
        transaction.commitAllowingStateLoss()
    }

    private fun storeStateVariables() {
        config.apply {
            mStoredUseEnglish = useEnglish
            mStoredIsSundayFirst = isSundayFirst
            mStoredTextColor = textColor
            mStoredPrimaryColor = primaryColor
            mStoredBackgroundColor = backgroundColor
            mStoredUse24HourFormat = use24hourFormat
        }
        mStoredDayCode = Formatter.getTodayCode()
    }

    private fun checkOpenIntents() {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        if (dayCodeToOpen.isNotEmpty()) {
            openDayCode(dayCodeToOpen)
        }

        val eventIdToOpen = intent.getIntExtra(EVENT_ID, 0)
        val eventOccurrenceToOpen = intent.getIntExtra(EVENT_OCCURRENCE_TS, 0)
        if (eventIdToOpen != 0 && eventOccurrenceToOpen != 0) {
            Intent(this, com.phamtuan.calendar.activities.EventActivity::class.java).apply {
                putExtra(EVENT_ID, eventIdToOpen)
                putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                startActivity(this)
            }
        }
    }

    private fun showViewDialog() {
        val res = resources
        val items = arrayListOf(
                RadioItem(WEEKLY_VIEW, res.getString(R.string.weekly_view)),
                RadioItem(MONTHLY_VIEW, res.getString(R.string.monthly_view)),
                RadioItem(YEARLY_VIEW, res.getString(R.string.yearly_view)),
                RadioItem(EVENTS_LIST_VIEW, res.getString(R.string.simple_event_list)))

        RadioGroupDialog(this, items, config.storedView) {
            updateView(it as Int)
            invalidateOptionsMenu()
        }
    }

    private fun goToToday() {
        if (config.storedView == WEEKLY_VIEW) {
            week_view_view_pager.currentItem = mDefaultWeeklyPage
        } else if (config.storedView == MONTHLY_VIEW) {
            main_view_pager.currentItem = mDefaultMonthlyPage
        } else if (config.storedView == YEARLY_VIEW) {
            if (mIsMonthSelected) {
                openMonthlyToday()
            } else {
                main_view_pager.currentItem = mDefaultYearlyPage
            }
        }
    }

    private fun shouldGoToTodayBeVisible() = when {
        config.storedView == WEEKLY_VIEW -> week_view_view_pager.currentItem != mDefaultWeeklyPage
        config.storedView == MONTHLY_VIEW -> main_view_pager.currentItem != mDefaultMonthlyPage
        config.storedView == YEARLY_VIEW -> main_view_pager.currentItem != mDefaultYearlyPage
        else -> false
    }

    private fun showFilterDialog() {
        com.phamtuan.calendar.dialogs.FilterEventTypesDialog(this) {
            refreshViewPager()
        }
    }

    private fun refreshCalDAVCalendars() {
        toast(R.string.refreshing)
        val uri = CalendarContract.Calendars.CONTENT_URI
        contentResolver.registerContentObserver(uri, false, calDAVSyncObserver)
        Bundle().apply {
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            ContentResolver.requestSync(null, uri.authority, this)
        }
        scheduleCalDAVSync(true)
    }

    private val calDAVSyncObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (!selfChange) {
                mCalDAVSyncHandler.removeCallbacksAndMessages(null)
                mCalDAVSyncHandler.postDelayed({
                    recheckCalDAVCalendars {
                        refreshViewPager()
                        toast(R.string.refreshing_complete)
                    }
                }, CALDAV_SYNC_DELAY)
            }
        }
    }

    private fun addHolidays() {
        val items = getHolidayRadioItems()
        RadioGroupDialog(this, items) {
            toast(R.string.importing)
            Thread {
                val holidays = getString(R.string.holidays)
                var eventTypeId = dbHelper.getEventTypeIdWithTitle(holidays)
                if (eventTypeId == -1) {
                    val eventType = EventType(0, holidays, resources.getColor(R.color.default_holidays_color))
                    eventTypeId = dbHelper.insertEventType(eventType)
                }
                val result = IcsImporter(this).importEvents(it as String, eventTypeId)
                handleParseResult(result)
                if (result != IcsImporter.ImportResult.IMPORT_FAIL) {
                    runOnUiThread {
                        updateViewPager()
                    }
                }
            }.start()
        }
    }

    private fun tryAddBirthdays() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                Thread {
                    addContactEvents(true) {
                        if (it > 0) {
                            toast(R.string.birthdays_added)
                            updateViewPager()
                        } else {
                            toast(R.string.no_birthdays)
                        }
                    }
                }.start()
            } else {
                toast(R.string.no_contacts_permission)
            }
        }
    }

    private fun tryAddAnniversaries() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                Thread {
                    addContactEvents(false) {
                        if (it > 0) {
                            toast(R.string.anniversaries_added)
                            updateViewPager()
                        } else {
                            toast(R.string.no_anniversaries)
                        }
                    }
                }.start()
            } else {
                toast(R.string.no_contacts_permission)
            }
        }
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        toast(when (result) {
            IcsImporter.ImportResult.IMPORT_OK -> R.string.holidays_imported_successfully
            IcsImporter.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_holidays_failed
            else -> R.string.importing_holidays_failed
        }, Toast.LENGTH_LONG)
    }

    private fun addContactEvents(birthdays: Boolean, callback: (Int) -> Unit) {
        var eventsAdded = 0
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Event.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP,
                ContactsContract.CommonDataKinds.Event.START_DATE)

        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val type = if (birthdays) ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY else ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, type.toString())
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val dateFormats = getDateFormats()
                val existingEvents = if (birthdays) dbHelper.getBirthdays() else dbHelper.getAnniversaries()
                val importIDs = existingEvents.map { it.importId }
                val eventTypeId = if (birthdays) getBirthdaysEventTypeId() else getAnniversariesEventTypeId()

                do {
                    val contactId = cursor.getIntValue(ContactsContract.CommonDataKinds.Event.CONTACT_ID).toString()
                    val name = cursor.getStringValue(ContactsContract.Contacts.DISPLAY_NAME)
                    val startDate = cursor.getStringValue(ContactsContract.CommonDataKinds.Event.START_DATE)

                    for (format in dateFormats) {
                        try {
                            val formatter = SimpleDateFormat(format, Locale.getDefault())
                            val date = formatter.parse(startDate)
                            if (date.year < 70)
                                date.year = 70

                            val timestamp = (date.time / 1000).toInt()
                            val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY
                            val lastUpdated = cursor.getLongValue(ContactsContract.CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP)
                            val event = Event(0, timestamp, timestamp, name, importId = contactId, flags = FLAG_ALL_DAY, repeatInterval = YEAR,
                                    eventType = eventTypeId, source = source, lastUpdated = lastUpdated)

                            if (!importIDs.contains(contactId)) {
                                dbHelper.insert(event, false) {
                                    eventsAdded++
                                }
                            }
                            break
                        } catch (e: Exception) {
                        }
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            showErrorToast(e)
        } finally {
            cursor?.close()
        }

        runOnUiThread {
            callback(eventsAdded)
        }
    }

    private fun getBirthdaysEventTypeId(): Int {
        val birthdays = getString(R.string.birthdays)
        var eventTypeId = dbHelper.getEventTypeIdWithTitle(birthdays)
        if (eventTypeId == -1) {
            val eventType = EventType(0, birthdays, resources.getColor(R.color.default_birthdays_color))
            eventTypeId = dbHelper.insertEventType(eventType)
        }
        return eventTypeId
    }

    private fun getAnniversariesEventTypeId(): Int {
        val anniversaries = getString(R.string.anniversaries)
        var eventTypeId = dbHelper.getEventTypeIdWithTitle(anniversaries)
        if (eventTypeId == -1) {
            val eventType = EventType(0, anniversaries, resources.getColor(R.color.default_anniversaries_color))
            eventTypeId = dbHelper.insertEventType(eventType)
        }
        return eventTypeId
    }

    private fun getDateFormats() = arrayListOf(
            "yyyy-MM-dd",
            "yyyyMMdd",
            "yyyy.MM.dd",
            "yy-MM-dd",
            "yyMMdd",
            "yy.MM.dd",
            "yy/MM/dd",
            "MM-dd",
            "--MM-dd",
            "MMdd",
            "MM/dd",
            "MM.dd"
    )

    private fun updateView(view: Int) {
        calendar_fab.beGoneIf(view == YEARLY_VIEW)
        mIsMonthSelected = view == MONTHLY_VIEW
        config.storedView = view
        updateViewPager()
    }

    private fun updateViewPager() {
        resetTitle()
        when {
            config.storedView == YEARLY_VIEW -> fillYearlyViewPager()
            config.storedView == EVENTS_LIST_VIEW -> fillEventsList()
            config.storedView == WEEKLY_VIEW -> fillWeeklyViewPager()
            else -> openMonthlyToday()
        }

        com.phamtuan.calendar.activities.MainActivity.Companion.mWeekScrollY = 0
    }

    private fun openMonthlyToday() {
        val targetDay = DateTime().toString(Formatter.DAYCODE_PATTERN)
        fillMonthlyViewPager(targetDay)
    }

    private fun refreshViewPager() {
        when {
            config.storedView == YEARLY_VIEW && !mIsMonthSelected -> (main_view_pager.adapter as? com.phamtuan.calendar.adapters.MyYearPagerAdapter)?.refreshEvents(main_view_pager.currentItem)
            config.storedView == EVENTS_LIST_VIEW -> fillEventsList()
            config.storedView == WEEKLY_VIEW -> (week_view_view_pager.adapter as? com.phamtuan.calendar.adapters.MyWeekPagerAdapter)?.refreshEvents(week_view_view_pager.currentItem)
            else -> (main_view_pager.adapter as? com.phamtuan.calendar.adapters.MyMonthPagerAdapter)?.refreshEvents(main_view_pager.currentItem)
        }
    }

    private fun tryImportEvents() {
        handlePermission(PERMISSION_READ_STORAGE) {
            if (it) {
                importEvents()
            }
        }
    }

    private fun importEvents() {
        FilePickerDialog(this) {
            importEventsDialog(it)
        }
    }

    private fun tryImportEventsFromFile(uri: Uri) {
        when {
            uri.scheme == "file" -> importEventsDialog(uri.path)
            uri.scheme == "content" -> {
                val tempFile = getTempFile()
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                val inputStream = contentResolver.openInputStream(uri)
                val out = FileOutputStream(tempFile)
                inputStream.copyTo(out)
                importEventsDialog(tempFile.absolutePath)
            }
            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun importEventsDialog(path: String) {
        com.phamtuan.calendar.dialogs.ImportEventsDialog(this, path) {
            if (it) {
                runOnUiThread {
                    updateViewPager()
                }
            }
        }
    }

    private fun tryExportEvents() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                exportEvents()
            }
        }
    }

    private fun exportEvents() {
        FilePickerDialog(this, pickFile = false, showFAB = true) {
            val path = it
            com.phamtuan.calendar.dialogs.ExportEventsDialog(this, path) { exportPastEvents, file, eventTypes ->
                Thread {
                    val events = dbHelper.getEventsToExport(exportPastEvents).filter { eventTypes.contains(it.eventType.toString()) }
                    if (events.isEmpty()) {
                        toast(R.string.no_events_for_exporting)
                    } else {
                        toast(R.string.exporting)
                        IcsExporter().exportEvents(this, file, events as ArrayList<Event>) {
                            toast(when (it) {
                                IcsExporter.ExportResult.EXPORT_OK -> R.string.events_exported_successfully
                                IcsExporter.ExportResult.EXPORT_PARTIAL -> R.string.exporting_some_events_failed
                                else -> R.string.exporting_events_failed
                            })
                        }
                    }
                }.start()
            }
        }
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, com.phamtuan.calendar.activities.SettingsActivity::class.java))
    }


    private fun exit() {
        finish()
    }

    private fun launchAbout() {
        startAboutActivity(R.string.packegename, LICENSE_KOTLIN or LICENSE_JODA or LICENSE_STETHO or LICENSE_MULTISELECT or LICENSE_GSON or
                LICENSE_LEAK_CANARY, BuildConfig.VERSION_NAME)
    }

    private fun resetTitle() {
        supportActionBar?.title = getString(R.string.app_launcher_name)
        supportActionBar?.subtitle = ""
    }

    private fun fillMonthlyViewPager(targetDay: String) {
        main_weekly_scrollview.beGone()
        calendar_fab.beVisible()
        val codes = getMonths(targetDay)
        val monthlyAdapter = MyMonthPagerAdapter(supportFragmentManager, codes, this)
        mDefaultMonthlyPage = codes.size / 2

        main_view_pager.apply {
            adapter = monthlyAdapter
            beVisible()
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                    when (state) {
                        ViewPager.SCROLL_STATE_IDLE -> {
                            showButton(true)
                        }
                        else -> {
                            showButton(false)
                        }
                    }
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    invalidateOptionsMenu()
                    if (config.storedView == YEARLY_VIEW) {
                        val dateTime = Formatter.getDateTimeFromCode(codes[position])
                        supportActionBar?.title = "${getString(R.string.app_launcher_name)} - ${Formatter.getYear(dateTime)}"
                    }
                }
            })
            currentItem = mDefaultMonthlyPage
        }
        calendar_event_list_holder.beGone()
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)))
        }

        return months
    }

    private fun fillWeeklyViewPager() {
        var thisweek = DateTime().withDayOfWeek(1).withTimeAtStartOfDay().minusDays(if (config.isSundayFirst) 1 else 0)
        if (DateTime().minusDays(7).seconds() > thisweek.seconds()) {
            thisweek = thisweek.plusDays(7)
        }
        val weekTSs = getWeekTimestamps(thisweek.seconds())
        val weeklyAdapter = MyWeekPagerAdapter(supportFragmentManager, weekTSs, object : WeekFragment.WeekScrollListener {
            override fun scrollTo(y: Int) {
                week_view_hours_scrollview.scrollY = y
                mWeekScrollY = y
            }
        })
        main_view_pager.beGone()
        calendar_event_list_holder.beGone()
        main_weekly_scrollview.beVisible()

        week_view_hours_holder.removeAllViews()
        val hourDateTime = DateTime().withDate(2000, 1, 1).withTime(0, 0, 0, 0)
        for (i in 1..23) {
            val formattedHours = Formatter.getHours(applicationContext, hourDateTime.withHourOfDay(i))
            (layoutInflater.inflate(R.layout.weekly_view_hour_textview, null, false) as TextView).apply {
                text = formattedHours
                setTextColor(mStoredTextColor)
                week_view_hours_holder.addView(this)
            }
        }

        mDefaultWeeklyPage = weekTSs.size / 2
        week_view_view_pager.apply {
            adapter = weeklyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                    when (state) {
                        ViewPager.SCROLL_STATE_IDLE -> {
                            showButton(true)
                        }
                        else -> {
                            showButton(false)
                        }
                    }
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    invalidateOptionsMenu()
                    setupWeeklyActionbarTitle(weekTSs[position])
                }
            })
            currentItem = mDefaultWeeklyPage
        }

        week_view_hours_scrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                com.phamtuan.calendar.activities.MainActivity.Companion.mWeekScrollY = y
                weeklyAdapter.updateScrollY(week_view_view_pager.currentItem, y)
            }
        })
        week_view_hours_scrollview.setOnTouchListener { view, motionEvent -> true }
    }

    fun updateHoursTopMargin(margin: Int) {
        week_view_hours_divider.layoutParams.height = margin
        week_view_hours_scrollview.requestLayout()
    }

    private fun getWeekTimestamps(targetWeekTS: Int): List<Int> {
        val weekTSs = ArrayList<Int>(PREFILLED_WEEKS)
        for (i in -PREFILLED_WEEKS / 2..PREFILLED_WEEKS / 2) {
            weekTSs.add(Formatter.getDateTimeFromTS(targetWeekTS).plusWeeks(i).seconds())
        }
        return weekTSs
    }

    private fun setupWeeklyActionbarTitle(timestamp: Int) {
        val startDateTime = Formatter.getDateTimeFromTS(timestamp)
        val endDateTime = Formatter.getDateTimeFromTS(timestamp + WEEK_SECONDS)
        val startMonthName = Formatter.getMonthName(applicationContext, startDateTime.monthOfYear)
        if (startDateTime.monthOfYear == endDateTime.monthOfYear) {
            var newTitle = startMonthName
            if (startDateTime.year != DateTime().year)
                newTitle += " - ${startDateTime.year}"
            supportActionBar?.title = newTitle
        } else {
            val endMonthName = Formatter.getMonthName(applicationContext, endDateTime.monthOfYear)
            supportActionBar?.title = "$startMonthName - $endMonthName"
        }
        supportActionBar?.subtitle = "${getString(R.string.week)} ${startDateTime.plusDays(3).weekOfWeekyear}"
    }

    private fun fillYearlyViewPager() {
        main_weekly_scrollview.beGone()
        calendar_fab.beGone()
        val targetYear = DateTime().toString(Formatter.YEAR_PATTERN).toInt()
        val years = getYears(targetYear)
        val yearlyAdapter = com.phamtuan.calendar.adapters.MyYearPagerAdapter(supportFragmentManager, years, this)

        mDefaultYearlyPage = years.size / 2
        main_view_pager.apply {
            adapter = yearlyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    invalidateOptionsMenu()
                    if (position < years.size) {
                        supportActionBar?.title = "${getString(R.string.app_launcher_name)} - ${years[position]}"
                    }
                }
            })
            currentItem = mDefaultYearlyPage
            beVisible()
        }
        supportActionBar?.title = "${getString(R.string.app_launcher_name)} - ${years[years.size / 2]}"
        calendar_event_list_holder.beGone()
    }

    private fun getYears(targetYear: Int): List<Int> {
        val years = ArrayList<Int>(PREFILLED_YEARS)
        years += targetYear - PREFILLED_YEARS / 2..targetYear + PREFILLED_YEARS / 2
        return years
    }

    private fun fillEventsList() {
        main_view_pager.adapter = null
        main_view_pager.beGone()
        main_weekly_scrollview.beGone()
        calendar_event_list_holder.beVisible()
        supportFragmentManager.beginTransaction().replace(R.id.calendar_event_list_holder, EventListFragment(), "").commit()
    }

    override fun goLeft() {
        main_view_pager.currentItem = main_view_pager.currentItem - 1
    }

    override fun goRight() {
        main_view_pager.currentItem = main_view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        fillMonthlyViewPager(Formatter.getDayCodeFromDateTime(dateTime))
        mIsMonthSelected = true
    }

    private fun openDayAt(timestamp: Long) {
        val dayCode = Formatter.getDayCodeFromTS((timestamp / 1000).toInt())
        openDayCode(dayCode)
    }

    private fun openDayCode(dayCode: String) {
        Intent(this, com.phamtuan.calendar.activities.DayActivity::class.java).apply {
            putExtra(DAY_CODE, dayCode)
            startActivity(this)
        }
    }

    private fun getHolidayRadioItems(): ArrayList<RadioItem> {
        val items = ArrayList<RadioItem>()

        LinkedHashMap<String, String>().apply {
            put("Algeria", "algeria.ics")
            put("Argentina", "argentina.ics")
            put("België", "belgium.ics")
            put("Bolivia", "bolivia.ics")
            put("Brasil", "brazil.ics")
            put("Canada", "canada.ics")
            put("Česká republika", "czech.ics")
            put("Deutschland", "germany.ics")
            put("Eesti", "estonia.ics")
            put("España", "spain.ics")
            put("Éire", "ireland.ics")
            put("France", "france.ics")
            put("Hanguk", "southkorea.ics")
            put("Hellas", "greece.ics")
            put("India", "india.ics")
            put("Ísland", "iceland.ics")
            put("Italia", "italy.ics")
            put("Magyarország", "hungary.ics")
            put("Nederland", "netherlands.ics")
            put("日本", "japan.ics")
            put("Norge", "norway.ics")
            put("Österreich", "austria.ics")
            put("Pākistān", "pakistan.ics")
            put("Polska", "poland.ics")
            put("Portugal", "portugal.ics")
            put("Россия", "russia.ics")
            put("Schweiz", "switzerland.ics")
            put("Slovenija", "slovenia.ics")
            put("Slovensko", "slovakia.ics")
            put("Suomi", "finland.ics")
            put("Sverige", "sweden.ics")
            put("United Kingdom", "unitedkingdom.ics")
            put("United States", "unitedstates.ics")

            var i = 0
            for ((country, file) in this) {
                items.add(RadioItem(i++, country, file))
            }
        }

        return items
    }

    // dialog show những cái mới ở phiên bản hiện tại
    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(39, R.string.release_39))
            add(Release(40, R.string.release_40))
            add(Release(42, R.string.release_42))
            add(Release(44, R.string.release_44))
            add(Release(46, R.string.release_46))
            add(Release(48, R.string.release_48))
            add(Release(49, R.string.release_49))
            add(Release(51, R.string.release_51))
            add(Release(52, R.string.release_52))
            add(Release(54, R.string.release_54))
            add(Release(57, R.string.release_57))
            add(Release(59, R.string.release_59))
            add(Release(60, R.string.release_60))
            add(Release(62, R.string.release_62))
            add(Release(67, R.string.release_67))
            add(Release(69, R.string.release_69))
            add(Release(71, R.string.release_71))
            add(Release(73, R.string.release_73))
            add(Release(76, R.string.release_76))
            add(Release(77, R.string.release_77))
            add(Release(80, R.string.release_80))
            add(Release(84, R.string.release_84))
            add(Release(86, R.string.release_86))
            add(Release(88, R.string.release_88))
            add(Release(98, R.string.release_98))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }

    private fun initAds() {
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd?.adUnitId = getString(R.string.Interstitial_admod_id)
        mInterstitialAd?.loadAd(AdRequest.Builder().build())
        mInterstitialAd?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                mInterstitialAd?.show()
            }
        }
    }

    private fun initAdGoogle() {
        mInterstitialAd = null
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd?.adUnitId = getString(R.string.Interstitial_admod_id)
        mInterstitialAd?.loadAd(AdRequest.Builder().build())
        mInterstitialAd?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                mInterstitialAd?.show()
            }

            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
            }
        }
    }

    private fun initMenu() {
        var group1 = GroupMenu()
        group1.name = "Contact"
        group1.show = true
        var sideMenu1 = SideMenuItem(resources.getString(R.string.add_holidays), R.drawable.ic_menu_holiday)
        var sideMenu2 = SideMenuItem(resources.getString(R.string.add_birthdays), R.drawable.ic_menu_contact_birthday)
        var sideMenu3 = SideMenuItem(resources.getString(R.string.add_anniversaries), R.drawable.ic_menu_contact_anniver)
        group1.sideMenus.add(sideMenu1)
        group1.sideMenus.add(sideMenu2)
        group1.sideMenus.add(sideMenu3)

        var group2 = GroupMenu()
        group2.name = "File"
        group2.show = true
        var sideMenu4 = SideMenuItem(resources.getString(R.string.import_events_from_ics), R.drawable.ic_menu_import_file)
        var sideMenu5 = SideMenuItem(resources.getString(R.string.export_events_to_ics), R.drawable.ic_menu_export_file)
        group2.sideMenus.add(sideMenu4)
        group2.sideMenus.add(sideMenu5)

        var group3 = GroupMenu()
        group3.name = "Apps"
        group3.show = true
        var sideMenu6 = SideMenuItem(resources.getString(R.string.rate_app), R.drawable.ic_menu_rate_app)
        var sideMenu7 = SideMenuItem(resources.getString(R.string.more_app), R.drawable.ic_menu_more_app)
        var sideMenu8 = SideMenuItem(resources.getString(R.string.advertisement), R.drawable.ic_menu_advertisement)
        group3.sideMenus.add(sideMenu6)
        group3.sideMenus.add(sideMenu7)
        group3.sideMenus.add(sideMenu8)

        var group4 = GroupMenu()
        group4.name = "Setting"
        group4.show = true
        var sideMenu9 = SideMenuItem(resources.getString(R.string.settings), R.drawable.ic_menu_setting)
        var sideMenu10 = SideMenuItem(resources.getString(R.string.about), R.drawable.ic_menu_detail)
        var sideMenu11 = SideMenuItem(resources.getString(R.string.Exit), R.drawable.ic_menu_exit)
        group4.sideMenus.add(sideMenu9)
        group4.sideMenus.add(sideMenu10)
        group4.sideMenus.add(sideMenu11)

        menus.add(group1)
        menus.add(group2)
        menus.add(group3)
        menus.add(group4)
    }

    private fun initSideMenu() {
        listMenu.choiceMode = ListView.CHOICE_MODE_SINGLE
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val width = Utils.dpToPx(302)

        val rtl = Utils.isRTL()
        val leftPadding = if (rtl) 10 else 50
        val rightPadding = if (rtl) 50 else 10
        val leftWidth = width - Utils.dpToPx(leftPadding)
        val rightWidth = width - Utils.dpToPx(rightPadding)

        if (Utils.hasJellyBeanMR2()) {
            listMenu.setIndicatorBoundsRelative(leftWidth, rightWidth)

        } else {
            listMenu.setIndicatorBounds(leftWidth, rightWidth)
        }
        menuAdapter = MainMenuAdapter(this, menus)
        listMenu.setAdapter(menuAdapter)
        initMenu()
        menuAdapter?.notifyDataSetChanged()
        for (i in 0..menuAdapter!!.groupCount - 1) {
            listMenu.expandGroup(i)
        }
        listMenu.setOnChildClickListener { expandableListView, view, groupPosition, childPosition, l ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menus[groupPosition].sideMenus[childPosition].name) {
                resources.getString(R.string.add_holidays) -> {
                    addHolidays()
                }
                resources.getString(R.string.add_birthdays) -> {
                    tryAddBirthdays()
                }
                resources.getString(R.string.add_anniversaries) -> {
                    tryAddAnniversaries()
                }
                resources.getString(R.string.import_events_from_ics) -> {
                    tryImportEvents()
                }
                resources.getString(R.string.export_events_to_ics) -> {
                    tryExportEvents()
                }
                resources.getString(R.string.rate_app) -> {
                    AppUtil.gotoPlaystore(this)

                }
                resources.getString(R.string.more_app) -> {
                    AppUtil.gotoStore(this)
                }
                resources.getString(R.string.advertisement) -> {
                    showAdsVideos()
                }
                resources.getString(R.string.settings) -> {
                    launchSettings()
                }
                resources.getString(R.string.about) -> {
                    launchAbout()
                }
                resources.getString(R.string.Exit) -> {
                    exit()
                }

            }
            false
        }
    }

    private fun showAdsVideos() {
        startActivity(Intent(this, AdssActivity::class.java))
    }

    private var runnableAnimation = object : Runnable {
        lateinit var weakFab: WeakReference<FloatingActionButton>

        override fun run() {
            weakFab.get()
                    ?.animate()
                    ?.translationX(0f)
                    ?.setInterpolator(FastOutSlowInInterpolator())
                    ?.setDuration(300)
                    ?.start()
        }
    }

    private fun showButton(isShow: Boolean) {
        if (isFabCreateEventShowing == isShow) {
            return
        }

        isFabCreateEventShowing = isShow
        if (isFabCreateEventShowing) {
            handlerFab.postDelayed(runnableAnimation, 300)
        } else {
            handlerFab.removeCallbacks(runnableAnimation)
            calendar_fab.animate()
                    .translationX(widthFabCreate + marginFabCreate)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setDuration(300)
                    .start()
        }
    }
    private fun initFaceAds(){
        interstitialAdFace = com.facebook.ads.InterstitialAd(this, Constance.FACEBOOK_ADS_1)
        interstitialAdFace?.setAdListener(this)
        interstitialAdFace?.loadAd()
    }

    //facebook
    override fun onInterstitialDisplayed(p0: Ad?) {

    }

    override fun onAdClicked(p0: Ad?) {

    }

    override fun onInterstitialDismissed(p0: Ad?) {

    }

    override fun onError(p0: Ad?, p1: AdError?) {

    }

    override fun onAdLoaded(p0: Ad?) {
        interstitialAdFace?.show()
    }

    override fun onLoggingImpression(p0: Ad?) {

    }
}