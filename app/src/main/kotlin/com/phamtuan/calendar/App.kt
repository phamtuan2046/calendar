package com.phamtuan.calendar

import android.support.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.google.android.gms.ads.MobileAds
import com.simplemobiletools.commons.extensions.checkUseEnglish

//import com.squareup.leakcanary.LeakCanary

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (com.phamtuan.calendar.BuildConfig.DEBUG) {
//            if (LeakCanary.isInAnalyzerProcess(this)) {
//                return
//            }
//            LeakCanary.install(this)
            Stetho.initializeWithDefaults(this)
        }

        checkUseEnglish()
    }
}
