package com.krystianwsul.checkme.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krystianwsul.checkme.MyApplication

class AlarmReceiver : BroadcastReceiver() {

    companion object {

        private const val SOURCE_KEY = "source"

        fun newIntent(source: String) = Intent(MyApplication.instance, AlarmReceiver::class.java).apply {
            putExtra(SOURCE_KEY, source)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val source = intent.getStringExtra(SOURCE_KEY)

        TickJobIntentService.start(TickJobIntentService.getIntent(context, false, "AlarmReceiver: $source"))
    }
}
