package com.krystianwsul.checkme.notifications

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService


class TickJobIntentService : JobIntentService() {

    companion object {

        fun start(intent: Intent) = JobIntentService.enqueueWork(MyApplication.instance, TickJobIntentService::class.java, 1, intent)

        const val MAX_NOTIFICATIONS = 3
        const val GROUP_KEY = "group"

        private const val SILENT_KEY = "silent"
        private const val SOURCE_KEY = "source"

        const val TICK_PREFERENCES = "tickPreferences"
        const val LAST_TICK_KEY = "lastTick"
        const val TICK_LOG = "tickLog"

        // DON'T HOLD STATE IN STATIC VARIABLES

        fun startServiceRegister(context: Context, source: String) {
            TickJobIntentService.start(getIntent(context, true, source))
        }

        fun startServiceDebug(context: Context, source: String) {
            TickJobIntentService.start(getIntent(context, false, source))
        }

        fun getIntent(context: Context, silent: Boolean, source: String): Intent {
            check(!TextUtils.isEmpty(source))

            val intent = Intent(context, TickJobIntentService::class.java)
            intent.putExtra(SILENT_KEY, silent)
            intent.putExtra(SOURCE_KEY, source)
            return intent
        }

        fun tick(intent: Intent) {
            check(intent.hasExtra(SILENT_KEY))
            check(intent.hasExtra(SOURCE_KEY))

            val silent = intent.getBooleanExtra(SILENT_KEY, false)

            val sourceName = intent.getStringExtra(SOURCE_KEY)
            check(!TextUtils.isEmpty(sourceName))

            tick(silent, sourceName)
        }

        // still running?
        fun tick(silent: Boolean, sourceName: String, listener: (() -> Unit)? = null): Boolean {
            val domainFactory = KotlinDomainFactory.getKotlinDomainFactory().domainFactory

            val listeners = listOfNotNull(listener)

            if (domainFactory.isConnected) {
                return if (domainFactory.isConnectedAndSaved) {
                    domainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData(silent, sourceName, listeners))
                    true
                } else {
                    domainFactory.updateNotificationsTick(SaveService.Source.SERVICE, silent, sourceName)
                    false
                }
            } else {
                domainFactory.updateNotificationsTick(SaveService.Source.SERVICE, silent, sourceName)

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                return if (firebaseUser != null) {
                    domainFactory.setUserInfo(SaveService.Source.SERVICE, UserInfo(firebaseUser))

                    domainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData(silent, sourceName, listeners))

                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onHandleWork(intent: Intent) = tick(intent)
}