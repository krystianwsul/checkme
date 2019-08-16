package com.krystianwsul.checkme.notifications

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.JobIntentService
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.persistencemodel.SaveService


class TickJobIntentService : JobIntentService() {

    companion object {

        fun start(intent: Intent) = enqueueWork(MyApplication.instance, TickJobIntentService::class.java, 1, intent)

        const val MAX_NOTIFICATIONS = 3
        const val GROUP_KEY = "group"

        private const val SILENT_KEY = "silent"
        private const val SOURCE_KEY = "source"

        // DON'T HOLD STATE IN STATIC VARIABLES

        fun startServiceSilent(context: Context, source: String) {
            Preferences.logLineDate("TickJobIntentService.startServiceSilent from $source")
            start(getIntent(context, true, source))
        }

        fun startServiceNormal(context: Context, source: String) {
            Preferences.logLineDate("TickJobIntentService.startServiceNormal from $source")
            start(getIntent(context, false, source))
        }

        private fun getIntent(context: Context, silent: Boolean, source: String): Intent {
            check(!TextUtils.isEmpty(source))

            return Intent(context, TickJobIntentService::class.java).apply {
                putExtra(SILENT_KEY, silent)
                putExtra(SOURCE_KEY, source)
            }
        }

        // still running?
        fun tick(silent: Boolean, sourceName: String, listener: (() -> Unit)? = null) {
            Preferences.logLineHour("TickJobIntentService.tick from $sourceName")

            if (!MyApplication.instance.hasUserInfo) {
                Preferences.logLineHour("TickJobIntentService.tick skipping, no userInfo")
                listener?.invoke()
            } else {
                val listeners = listOfNotNull(listener)

                DomainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData.Lock(silent, sourceName, listeners))
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        check(intent.hasExtra(SILENT_KEY))
        check(intent.hasExtra(SOURCE_KEY))

        val silent = intent.getBooleanExtra(SILENT_KEY, false)

        val sourceName = intent.getStringExtra(SOURCE_KEY)!!
        check(!TextUtils.isEmpty(sourceName))

        tick(silent, sourceName)
    }
}