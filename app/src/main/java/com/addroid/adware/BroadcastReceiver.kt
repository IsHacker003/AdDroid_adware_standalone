package com.addroid.adware

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.os.Build

class ShowAds : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        val i : Intent = Intent(context, MainActivity::class.java)
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent?.getAction())) {
            i.putExtra("BootReceived","1")
        }
        context?.startActivity(i)
    }
}
