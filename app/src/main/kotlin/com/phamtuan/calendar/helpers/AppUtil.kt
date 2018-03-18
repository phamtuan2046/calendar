package com.phamtuan.calendar.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Created by P.Tuan on 1/8/2018.
 */
object AppUtil {
    fun gotoStore(context: Context) {
        var intentMarketAll = Intent("android.intent.action.VIEW")
        intentMarketAll.data = Uri.parse("https://play.google.com/store/apps/developer?id=ToolGameCorporation")
        context.startActivity(intentMarketAll)
    }

    fun gotoPlaystore(context: Context) {
        val appPackageName = context.packageName // getPackageName() from Context or Activity object
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)))
        } catch (anfe: android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)))
        }
    }
}