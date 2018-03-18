package com.phamtuan.calendar.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.phamtuan.calendar.R
import kotlinx.android.synthetic.main.dialog_rate_app.*

/**
 * Created by P.Tuan on 12/29/2017.
 */
class DialogRateApp : DialogFragment() {
    lateinit var mOnRate: (dialog: DialogFragment) -> Unit
    lateinit var mOnCancel: (dialog: DialogFragment) -> Unit
    companion object {
        fun newIntacnce(context: Context, onCancel:(dialog: DialogFragment) -> Unit,onRate:(dialog: DialogFragment) -> Unit): DialogRateApp {
            var dialog = DialogRateApp()
            dialog.mOnCancel = onCancel
            dialog.mOnRate = onRate
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(context)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater?.inflate(R.layout.dialog_rate_app, container, false)
        val displayRectangle = Rect()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window.decorView.getWindowVisibleDisplayFrame(displayRectangle)
        view?.minimumWidth = (displayRectangle.width() * 0.98f).toInt()
        view?.minimumHeight = LinearLayout.LayoutParams.WRAP_CONTENT
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        loadAdMod()
        btnCancel.setOnClickListener { mOnCancel.invoke(this) }
        btnRate.setOnClickListener { mOnRate.invoke(this) }

    }
    fun loadAdMod() {
        val adRequest = AdRequest.Builder()
                .build()
        adView?.loadAd(adRequest)
        adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                adView?.visibility = View.VISIBLE
            }
        }
    }

}