package com.phamtuan.calendar.activities

import android.content.Intent
import android.content.res.Resources
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import com.facebook.ads.*
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.phamtuan.calendar.R
import com.phamtuan.calendar.dialogs.CustomEventReminderDialog
import com.phamtuan.calendar.dialogs.SelectCalendarsDialog
import com.phamtuan.calendar.dialogs.SnoozePickerDialog
import com.phamtuan.calendar.extensions.*
import com.phamtuan.calendar.helpers.CalDAVHandler
import com.phamtuan.calendar.helpers.FONT_SIZE_LARGE
import com.phamtuan.calendar.helpers.FONT_SIZE_MEDIUM
import com.phamtuan.calendar.helpers.FONT_SIZE_SMALL
import com.phamtuan.calendar.models.EventType
import com.phamtuan.calendar.util.Constance
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CALENDAR
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CALENDAR
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.item_ads_facebook.*
import java.util.*

class SettingsActivity : com.phamtuan.calendar.activities.SimpleActivity(), InterstitialAdListener {
    private val GET_RINGTONE_URI = 1

    lateinit var res: Resources
    private var mStoredPrimaryColor = 0

    // QUẢNG CÁO FACEBOOK
    private var interstitialAdFace: com.facebook.ads.InterstitialAd? = null

    private var nativeAd:NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        res = resources
        mStoredPrimaryColor = config.primaryColor
        initAdsFaceNative()
        setupCaldavSync()
    }

    private fun initFaceAds() {
        interstitialAdFace = com.facebook.ads.InterstitialAd(this, Constance.FACEBOOK_ADS_1)
        interstitialAdFace?.setAdListener(this)
        interstitialAdFace?.loadAd()
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupUseEnglish()
        setupManageEventTypes()
        setupHourFormat()
        setupSundayFirst()
        setupReplaceDescription()
        setupWeekNumbers()
        setupWeeklyStart()
        setupWeeklyEnd()
        setupVibrate()
        setupReminderSound()
        setupSnoozeDelay()
        setupEventReminder()
        setupDisplayPastEvents()
        setupFontSize()
        updateTextColors(settings_holder)
        checkPrimaryColor()
    }

    override fun onPause() {
        super.onPause()
        mStoredPrimaryColor = config.primaryColor
    }

    private fun checkPrimaryColor() {
        if (config.primaryColor != mStoredPrimaryColor) {
            dbHelper.getEventTypes {
                if (it.filter { it.caldavCalendarId == 0 }.size == 1) {
                    val eventType = it.first { it.caldavCalendarId == 0 }
                    eventType.color = config.primaryColor
                    dbHelper.updateEventType(eventType)
                }
            }
        }
    }
    fun initAdsFaceNative() {
        nativeAd = NativeAd(this, Constance.FACEBOOK_NATIVE_ID_1)
        nativeAd?.setAdListener(object : AdListener {

            override fun onAdClicked(p0: Ad?) {

            }

            override fun onError(p0: Ad?, p1: AdError?) {
            }

            override fun onAdLoaded(p0: Ad?) {
                nativeAdTitle.text = nativeAd?.adTitle
                nativeAdSocialContext.text = nativeAd?.adSocialContext
                nativeAdBody.text = nativeAd?.adBody
                nativeAdCallToAction.text = nativeAd?.adCallToAction

                // Download and display the ad icon.
                val adIcon = nativeAd?.adIcon
                NativeAd.downloadAndDisplayImage(adIcon, nativeAdIcon)

                // Download and display the cover image.
                nativeAdMedia.setNativeAd(nativeAd)


                val adChoicesView = AdChoicesView(this@SettingsActivity, nativeAd, true)
                adChoicesContainer.addView(adChoicesView)

                // Register the Title and CTA button to listen for clicks.
                val clickableViews = ArrayList<View>()
                clickableViews.add(nativeAdTitle)
                clickableViews.add(nativeAdCallToAction)
                nativeAd?.registerViewForInteraction(nativeAdContainer, clickableViews)
                nativeAdContainer.visibility = View.VISIBLE

            }

            override fun onLoggingImpression(p0: Ad?) {
            }
        })

        // Request an ad
        nativeAd?.loadAd()
    }


    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            useEnglishToggled()
        }
    }

    private fun setupManageEventTypes() {
        settings_manage_event_types_holder.setOnClickListener {
            startActivity(Intent(this, com.phamtuan.calendar.activities.ManageEventTypesActivity::class.java))
        }
    }

    private fun setupHourFormat() {
        settings_hour_format.isChecked = config.use24hourFormat
        settings_hour_format_holder.setOnClickListener {
            settings_hour_format.toggle()
            config.use24hourFormat = settings_hour_format.isChecked
        }
    }

    private fun setupCaldavSync() {
        settings_caldav_sync.isChecked = config.caldavSync
        settings_caldav_sync_holder.setOnClickListener {
            if (config.caldavSync) {
                toggleCaldavSync(false)
            } else {
                handlePermission(PERMISSION_WRITE_CALENDAR) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CALENDAR) {
                            if (it) {
                                toggleCaldavSync(true)
                            }
                        }
                    }
                }
            }
        }

        settings_manage_synced_calendars_holder.beVisibleIf(config.caldavSync)
        settings_manage_synced_calendars_holder.setOnClickListener {
            showCalendarPicker()
        }
    }

    private fun toggleCaldavSync(enable: Boolean) {
        if (enable) {
            showCalendarPicker()
        } else {
            settings_caldav_sync.isChecked = false
            config.caldavSync = false
            settings_manage_synced_calendars_holder.beGone()
            config.getSyncedCalendarIdsAsList().forEach {
                CalDAVHandler(applicationContext).deleteCalDAVCalendarEvents(it.toLong())
            }
            dbHelper.deleteEventTypesWithCalendarId(config.caldavSyncedCalendarIDs)
        }
    }

    private fun showCalendarPicker() {
        val oldCalendarIds = config.getSyncedCalendarIdsAsList()

        com.phamtuan.calendar.dialogs.SelectCalendarsDialog(this) {
            val newCalendarIds = config.getSyncedCalendarIdsAsList()
            settings_manage_synced_calendars_holder.beVisibleIf(newCalendarIds.isNotEmpty())
            settings_caldav_sync.isChecked = newCalendarIds.isNotEmpty()
            config.caldavSync = newCalendarIds.isNotEmpty()
            toast(R.string.syncing)

            Thread {
                if (newCalendarIds.isNotEmpty()) {
                    val existingEventTypeNames = dbHelper.fetchEventTypes().map { it.getDisplayTitle().toLowerCase() } as ArrayList<String>
                    getSyncedCalDAVCalendars().forEach {
                        val calendarTitle = it.getFullTitle()
                        if (!existingEventTypeNames.contains(calendarTitle.toLowerCase())) {
                            val eventType = EventType(0, it.displayName, it.color, it.id, it.displayName, it.accountName)
                            existingEventTypeNames.add(calendarTitle.toLowerCase())
                            dbHelper.insertEventType(eventType)
                        }
                    }
                    CalDAVHandler(applicationContext).refreshCalendars(this) {}
                }

                val removedCalendarIds = oldCalendarIds.filter { !newCalendarIds.contains(it) }
                removedCalendarIds.forEach {
                    CalDAVHandler(applicationContext).deleteCalDAVCalendarEvents(it.toLong())
                    dbHelper.getEventTypeWithCalDAVCalendarId(it.toInt())?.apply {
                        dbHelper.deleteEventTypes(arrayListOf(this), true) {}
                    }
                }
                dbHelper.deleteEventTypesWithCalendarId(TextUtils.join(",", removedCalendarIds))
                toast(R.string.synchronization_completed)
            }.start()
        }
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = config.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            config.isSundayFirst = settings_sunday_first.isChecked
        }
    }

    private fun setupReplaceDescription() {
        settings_replace_description.isChecked = config.replaceDescription
        settings_replace_description_holder.setOnClickListener {
            settings_replace_description.toggle()
            config.replaceDescription = settings_replace_description.isChecked
        }
    }

    private fun setupWeeklyStart() {
        settings_start_weekly_at.text = getHoursString(config.startWeeklyAt)
        settings_start_weekly_at_holder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..24).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this@SettingsActivity, items, config.startWeeklyAt) {
                if (it as Int >= config.endWeeklyAt) {
                    toast(R.string.day_end_before_start)
                } else {
                    config.startWeeklyAt = it
                    settings_start_weekly_at.text = getHoursString(it)
                }
            }
        }
    }

    private fun setupWeeklyEnd() {
        settings_end_weekly_at.text = getHoursString(config.endWeeklyAt)
        settings_end_weekly_at_holder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..24).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this@SettingsActivity, items, config.endWeeklyAt) {
                if (it as Int <= config.startWeeklyAt) {
                    toast(R.string.day_end_before_start)
                } else {
                    config.endWeeklyAt = it
                    settings_end_weekly_at.text = getHoursString(it)
                }
            }
        }
    }

    private fun setupWeekNumbers() {
        settings_week_numbers.isChecked = config.displayWeekNumbers
        settings_week_numbers_holder.setOnClickListener {
            settings_week_numbers.toggle()
            config.displayWeekNumbers = settings_week_numbers.isChecked
        }
    }

    private fun setupReminderSound() {
        val noRingtone = res.getString(R.string.no_ringtone_selected)
        if (config.reminderSound.isEmpty()) {
            settings_reminder_sound.text = noRingtone
        } else {
            settings_reminder_sound.text = RingtoneManager.getRingtone(this, Uri.parse(config.reminderSound))?.getTitle(this) ?: noRingtone
        }
        settings_reminder_sound_holder.setOnClickListener {
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, res.getString(R.string.reminder_sound))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(config.reminderSound))

                if (resolveActivity(packageManager) != null)
                    startActivityForResult(this, GET_RINGTONE_URI)
                else {
                    toast(R.string.no_ringtone_picker)
                }
            }
        }
    }

    private fun setupVibrate() {
        settings_vibrate.isChecked = config.vibrateOnReminder
        settings_vibrate_holder.setOnClickListener {
            settings_vibrate.toggle()
            config.vibrateOnReminder = settings_vibrate.isChecked
        }
    }

    private fun setupSnoozeDelay() {
        updateSnoozeText()
        settings_snooze_delay_holder.setOnClickListener {
            com.phamtuan.calendar.dialogs.SnoozePickerDialog(this, config.snoozeDelay) {
                config.snoozeDelay = it
                updateSnoozeText()
            }
        }
    }

    private fun updateSnoozeText() {
        settings_snooze_delay.text = res.getQuantityString(R.plurals.by_minutes, config.snoozeDelay, config.snoozeDelay)
    }

    private fun setupEventReminder() {
        var reminderMinutes = config.defaultReminderMinutes
        settings_default_reminder.text = getFormattedMinutes(reminderMinutes)
        settings_default_reminder_holder.setOnClickListener {
            showEventReminderDialog(reminderMinutes) {
                config.defaultReminderMinutes = it
                reminderMinutes = it
                settings_default_reminder.text = getFormattedMinutes(it)
            }
        }
    }

    private fun getHoursString(hours: Int): String {
        return if (hours < 10) {
            "0$hours:00"
        } else {
            "$hours:00"
        }
    }

    private fun setupDisplayPastEvents() {
        var displayPastEvents = config.displayPastEvents
        updatePastEventsText(displayPastEvents)
        settings_display_past_events_holder.setOnClickListener {
            com.phamtuan.calendar.dialogs.CustomEventReminderDialog(this, displayPastEvents) {
                displayPastEvents = it
                config.displayPastEvents = it
                updatePastEventsText(it)
            }
        }
    }

    private fun updatePastEventsText(displayPastEvents: Int) {
        settings_display_past_events.text = getDisplayPastEventsText(displayPastEvents)
    }

    private fun getDisplayPastEventsText(displayPastEvents: Int): String {
        return if (displayPastEvents == 0)
            getString(R.string.never)
        else
            getFormattedMinutes(displayPastEvents, false)
    }

    private fun setupFontSize() {
        settings_font_size.text = getFontSizeText()
        settings_font_size_holder.setOnClickListener {
            val items = arrayListOf(
                    RadioItem(FONT_SIZE_SMALL, res.getString(R.string.small)),
                    RadioItem(FONT_SIZE_MEDIUM, res.getString(R.string.medium)),
                    RadioItem(FONT_SIZE_LARGE, res.getString(R.string.large)))

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settings_font_size.text = getFontSizeText()
                updateWidgets()
                updateListWidget()
            }
        }
    }

    private fun getFontSizeText() = getString(when (config.fontSize) {
        FONT_SIZE_SMALL -> R.string.small
        FONT_SIZE_MEDIUM -> R.string.medium
        else -> R.string.large
    })

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == GET_RINGTONE_URI) {
                val uri = data?.getParcelableExtra<Parcelable>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri == null) {
                    config.reminderSound = ""
                } else {
                    settings_reminder_sound.text = RingtoneManager.getRingtone(this, uri as Uri)?.getTitle(this)
                    config.reminderSound = uri.toString()
                }
            }
        }
    }

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
