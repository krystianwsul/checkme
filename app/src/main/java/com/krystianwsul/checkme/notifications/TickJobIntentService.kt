package com.krystianwsul.checkme.notifications

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService


class TickJobIntentService : JobIntentService() {

    companion object {

        private fun start(intent: Intent, source: String) {
            NotificationWrapper.instance.notifyTemporary("TickJobIntentService.start $source")

            val silent = intent.getBooleanExtra(SILENT_KEY, false)

            tick(silent, source) // todo if this works, clean up calls

            //enqueueWork(MyApplication.instance, TickJobIntentService::class.java, 1, intent)
        }

        const val MAX_NOTIFICATIONS = 3
        const val GROUP_KEY = "group"

        private const val SILENT_KEY = "silent"
        private const val SOURCE_KEY = "source"

        // DON'T HOLD STATE IN STATIC VARIABLES

        fun startServiceSilent(context: Context, source: String) {
            Preferences.tickLog.logLineDate("TickJobIntentService.startServiceSilent from $source")
            start(getIntent(context, true, source), source)
        }

        fun startServiceNormal(context: Context, source: String) {
            Preferences.tickLog.logLineDate("TickJobIntentService.startServiceNormal from $source")
            start(getIntent(context, false, source), source)
        }

        private fun getIntent(context: Context, silent: Boolean, source: String): Intent {
            check(source.isNotEmpty())

            return Intent(context, TickJobIntentService::class.java).apply {
                putExtra(SILENT_KEY, silent)
                putExtra(SOURCE_KEY, source)
            }
        }

        // still running?
        private fun tick(silent: Boolean, sourceName: String) {
            Preferences.tickLog.logLineHour("TickJobIntentService.tick from $sourceName")
            Preferences.temporaryNotificationLog.logLineHour("TickJobIntentService.tick from $sourceName")

            if (!MyApplication.instance.hasUserInfo) {
                Preferences.tickLog.logLineHour("TickJobIntentService.tick skipping, no userInfo")

                NotificationWrapper.instance.hideTemporary("TickJobIntentService.tick skipping")
            } else {
                DomainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData.Lock(silent, sourceName))
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        check(intent.hasExtra(SILENT_KEY))
        check(intent.hasExtra(SOURCE_KEY))

        val silent = intent.getBooleanExtra(SILENT_KEY, false)

        val sourceName = intent.getStringExtra(SOURCE_KEY)!!
        check(sourceName.isNotEmpty())

        tick(silent, sourceName)
    }
}