package com.krystianwsul.checkme.notifications

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        TickJobIntentService.start(TickJobIntentService.getIntent(context, true, "BootReceiver"))
    }
}
