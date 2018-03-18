package com.phamtuan.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.bumptech.glide.Glide
import com.phamtuan.calendar.R
import com.phamtuan.calendar.helpers.DAY_CODE
import com.phamtuan.calendar.helpers.EVENT_ID
import com.phamtuan.calendar.helpers.EVENT_OCCURRENCE_TS
import com.simplemobiletools.commons.activities.BaseSplashActivity
import com.simplemobiletools.commons.extensions.baseConfig
import kotlinx.android.synthetic.main.splash_activity.*

class SplashActivity : BaseSplashActivity() {
    private var mHandler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity)
        var color = baseConfig.primaryColor
        mainview.setBackgroundColor(color)
        Glide.with(this).load(R.drawable.img_splash).into(imgSplash)
    }

    override fun initActivity() {
        mHandler = Handler()
        mHandler?.postDelayed(Runnable {
            when {
                intent.extras?.containsKey(DAY_CODE) == true -> Intent(this, MainActivity::class.java).apply {
                    putExtra(DAY_CODE, intent.getStringExtra(DAY_CODE))
                    startActivity(this)
                }
                intent.extras?.containsKey(EVENT_ID) == true -> Intent(this, MainActivity::class.java).apply {
                    putExtra(EVENT_ID, intent.getIntExtra(EVENT_ID, 0))
                    putExtra(EVENT_OCCURRENCE_TS, intent.getIntExtra(EVENT_OCCURRENCE_TS, 0))
                    startActivity(this)
                }
                else -> startActivity(Intent(this, MainActivity::class.java))
            }
            this.finish()
        }, 500)


    }
}
