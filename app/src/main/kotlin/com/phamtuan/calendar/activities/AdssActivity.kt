package com.phamtuan.calendar.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdLoader.*
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.NativeAppInstallAd
import com.google.android.gms.ads.formats.NativeContentAd
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.phamtuan.calendar.R
import com.phamtuan.calendar.util.Constance
import kotlinx.android.synthetic.main.activity_ads.*

/**
 * Created by P.Tuan on 12/28/2017.
 */
class AdssActivity : AppCompatActivity(),RewardedVideoAdListener {
    private lateinit var mRewardedVideoAd: RewardedVideoAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ads)
        MobileAds.initialize(this,
                resources.getString(R.string.app_admod_id))

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.rewardedVideoAdListener = this
        initAds()
    }

    override fun onRewardedVideoAdClosed() {
        finish()
    }

    override fun onRewardedVideoAdLeftApplication() {
        Log.d("AdssActivity","onRewardedVideoAdLeftApplication")
    }

    override fun onRewardedVideoAdLoaded() {
        Log.d("AdssActivity","onRewardedVideoAdLoaded")
        progressBar.visibility = View.GONE
        mRewardedVideoAd.show()

    }

    override fun onRewardedVideoAdOpened() {
        Log.d("AdssActivity","onRewardedVideoAdOpened")
    }

    override fun onRewarded(p0: RewardItem?) {
        Log.d("AdssActivity","onRewarded")
    }

    override fun onRewardedVideoStarted() {
        Log.d("AdssActivity","onRewardedVideoStarted")
    }

    override fun onRewardedVideoAdFailedToLoad(p0: Int) {
       Log.d("AdssActivity","onRewardedVideoAdFailedToLoad")
        tvNotifi.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun initAds(){
        mRewardedVideoAd.loadAd(Constance.REWARDED_Ads,
                AdRequest.Builder().build())
    }

    override fun onPause() {
        super.onPause()
        mRewardedVideoAd.pause(this)
    }

    override fun onResume() {
        super.onResume()
        mRewardedVideoAd.resume(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mRewardedVideoAd.destroy(this)
    }
}