package com.simplemobiletools.commons.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.isBlackAndWhiteTheme
import com.simplemobiletools.commons.extensions.launchViewIntent
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.APP_LICENSES
import com.simplemobiletools.commons.helpers.APP_NAME
import com.simplemobiletools.commons.helpers.APP_VERSION_NAME
import kotlinx.android.synthetic.main.activity_about.*
import java.util.*

class AboutActivity : BaseSimpleActivity() {
    private var appName = ""
    private var linkColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        appName = intent.getStringExtra(APP_NAME) ?: ""
        linkColor = if (isBlackAndWhiteTheme()) Color.WHITE else baseConfig.primaryColor
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(about_holder)

//        setupWebsite()
        setupEmail()
        setupMoreApps()
        setupRateUs()
        setupInvite()
        setupLicense()
//        setupDonate()
        setupFacebook()
        setupGPlus()
        setupCopyright()
    }

    private fun setupWebsite() {
        val websiteText = String.format(getString(R.string.two_string_placeholder), getString(R.string.website_label), getString(R.string.my_website))
        about_website.text = websiteText
    }

    private fun setupEmail() {
        val label = getString(R.string.email_label)
        val email = getString(R.string.my_email)

        val appVersion = String.format(getString(R.string.app_version, intent.getStringExtra(APP_VERSION_NAME)))
        val deviceOS = String.format(getString(R.string.device_os), Build.VERSION.RELEASE)
        val newline = "%0D%0A"
        val separator = "___________________"
        val body = "$newline$newline$newline$separator$newline${getString(R.string.additional_info)}:$newline$appVersion$newline$deviceOS"
        val href = "$label<br><a href=\"mailto:$email?subject=$appName&body=$body\">$email</a>"
        about_email.text = Html.fromHtml(href)
        about_email.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupMoreApps() {
        about_more_apps.setOnClickListener {
            var intentMarketAll = Intent("android.intent.action.VIEW")
            intentMarketAll.data = Uri.parse("https://play.google.com/store/apps/developer?id=ToolGameCorporation")
            startActivity(intentMarketAll)
        }
        about_more_apps.setTextColor(linkColor)
    }

    private fun setupInvite() {
        about_invite.setOnClickListener {
            val text = String.format(getString(R.string.share_text), appName, getStoreUrl())
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, appName)
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                startActivity(Intent.createChooser(this, getString(R.string.invite_via)))
            }
        }
        about_invite.setTextColor(linkColor)
    }

    private fun setupRateUs() {
        if (baseConfig.appRunCount < 5) {
            about_rate_us.visibility = View.GONE
        } else {
            about_rate_us.setOnClickListener {
                try {
                    launchViewIntent("market://details?id=$packageName")
                } catch (ignored: ActivityNotFoundException) {
                    launchViewIntent(getStoreUrl())
                }
            }
        }
        about_rate_us.setTextColor(linkColor)
    }

    private fun setupLicense() {
        about_license.setOnClickListener {
            Intent(applicationContext, LicenseActivity::class.java).apply {
                putExtra(APP_LICENSES, intent.getIntExtra(APP_LICENSES, 0))
                startActivity(this)
            }
        }
        about_license.setTextColor(linkColor)
    }

    private fun setupDonate() {
        about_donate.setOnClickListener {
            launchViewIntent("https://simplemobiletools.github.io/donate/")
        }
        about_donate.setTextColor(linkColor)
    }

    private fun setupFacebook() {
        about_facebook.setOnClickListener {
            var link = "https://www.facebook.com/Android-Learning-Share-1948571468789880/"
            try {
                packageManager.getPackageInfo("com.facebook.katana", 0)
                link = "fb://page/1948571468789880"
            } catch (ignored: Exception) {
            }

            launchViewIntent(link)
        }
    }

    private fun setupGPlus() {
        about_gplus.setOnClickListener {
            launchViewIntent("https://plus.google.com/u/0/101563919795060381540")
        }
    }

    private fun setupCopyright() {
        val versionName = intent.getStringExtra(APP_VERSION_NAME) ?: ""
        val year = Calendar.getInstance().get(Calendar.YEAR)
        about_copyright.text = String.format(getString(R.string.copyright), versionName, year)
    }

    private fun getStoreUrl() = "https://play.google.com/store/apps/details?id=$packageName"
}
