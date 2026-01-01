package com.addroid.adware

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.addroid.adware.Constants.Companion.TAG
import com.addroid.adware.Constants.Companion.AD_UNIT_ID
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.MobileAds

class AdblockBypassService : Service() {
    private var interstitialAd: InterstitialAd? = null
    private var adIsLoading: Boolean = false

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        startForegroundServices()
        MobileAds.initialize(this@AdblockBypassService)
        loadAd()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("LocalService", "Received start id " + startId + ": " + intent)
        return START_STICKY
    }

    private fun loadAd() {
        // Request a new ad if one isn't already loaded.
        if (adIsLoading || interstitialAd != null) {
            return
        }
        adIsLoading = true

        // [START load_ad]
        InterstitialAd.load(
            this,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd = ad
                    // [START_EXCLUDE silent]
                    adIsLoading = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        applicationContext.startForegroundService(Intent(this@AdblockBypassService, AdsService::class.java))
                    } else {
                        applicationContext.startService(Intent(this@AdblockBypassService, AdsService::class.java))
                    }
                    // [END_EXCLUDE]
                    applicationContext.stopService(Intent(this@AdblockBypassService,
                        AdblockBypassService::class.java))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    interstitialAd = null
                    // [START_EXCLUDE silent]
                    adIsLoading = false
                    Log.d(TAG, "Ad was not loaded.")
                    loadAd()
                    // [END_EXCLUDE]
                }
            },
        )
        // [END load_ad]
    }

    private fun startForegroundServices() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId )
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(102, notification)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String{
        val channelId = "my_service"
        val channelName = "My Background Service"
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH
        )
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

}
