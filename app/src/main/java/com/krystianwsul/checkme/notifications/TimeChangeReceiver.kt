package com.krystianwsul.checkme.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        TickJobIntentService.start(TickJobIntentService.getIntent(context, true, "TimeChangeReceiver"))
    }
}
