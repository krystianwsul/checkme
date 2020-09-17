package com.krystianwsul.checkme.ticks

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

        const val TICK_NOTIFICATION_ID = 1

        private fun start(intent: Intent, source: String) {
            NotificationWrapper.instance.notifyTemporary(TICK_NOTIFICATION_ID, "TickJobIntentService.start $source")

            tick(source) // todo if this works, clean up calls

            //enqueueWork(MyApplication.instance, TickJobIntentService::class.java, 1, intent)
        }

        private const val SOURCE_KEY = "source"

        // DON'T HOLD STATE IN STATIC VARIABLES

        fun startServiceNormal(context: Context, source: String) {
            check(source.isNotEmpty())

            Preferences.tickLog.logLineDate("TickJobIntentService.startServiceNormal from $source")

            start(
                    Intent(
                            context,
                            TickJobIntentService::class.java
                    ).apply { putExtra(SOURCE_KEY, source) },
                    source
            )
        }

        // still running?
        private fun tick(sourceName: String) {
            Preferences.tickLog.logLineHour("TickJobIntentService.tick from $sourceName")
            Preferences.temporaryNotificationLog.logLineHour("TickJobIntentService.tick from $sourceName")

            if (!MyApplication.instance.hasUserInfo) {
                Preferences.tickLog.logLineHour("TickJobIntentService.tick skipping, no userInfo")

                NotificationWrapper.instance.hideTemporary(
                        TICK_NOTIFICATION_ID,
                        "TickJobIntentService.tick skipping"
                )
            } else {
                DomainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData.Lock(false, sourceName))
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        check(intent.hasExtra(SOURCE_KEY))

        val sourceName = intent.getStringExtra(SOURCE_KEY)!!
        check(sourceName.isNotEmpty())

        tick(sourceName)
    }
}