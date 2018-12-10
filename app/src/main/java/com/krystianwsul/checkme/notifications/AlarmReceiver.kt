package com.krystianwsul.checkme.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        TickJobIntentService.start(TickJobIntentService.getIntent(context, false, "AlarmReceiver"))
    }
}
