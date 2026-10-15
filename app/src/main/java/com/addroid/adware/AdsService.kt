package com.addroid.adware

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.google.android.gms.ads.MobileAds

class AdsService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
    override fun onCreate() {
        startForegroundServices()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW)
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        intentFilter.addAction(Intent.ACTION_USER_UNLOCKED)
        intentFilter.addDataScheme("package")
        val rec = ShowAds()
        registerReceiver(rec, intentFilter)
        MobileAds.initialize(this@AdsService)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("LocalService", "Received start id " + startId + ": " + intent)
        return START_STICKY
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
        startForeground(101, notification)
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
