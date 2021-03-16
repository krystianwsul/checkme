package com.krystianwsul.checkme.ticks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krystianwsul.checkme.MyApplication

class AlarmReceiver : BroadcastReceiver() {

    companion object {

        fun newIntent() = Intent(MyApplication.instance, AlarmReceiver::class.java)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Ticker.tick("AlarmReceiver").subscribe()
    }
}
