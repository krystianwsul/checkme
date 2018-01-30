package com.krystianwsul.checkme.notifications

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService
import junit.framework.Assert

class TickJobIntentService : JobIntentService() {

    companion object {

        fun start(intent: Intent) {
            JobIntentService.enqueueWork(MyApplication.instance, TickJobIntentService::class.java, 1, intent)
        }

        val MAX_NOTIFICATIONS = 3
        val GROUP_KEY = "group"

        private val SILENT_KEY = "silent"
        private val SOURCE_KEY = "source"

        val TICK_PREFERENCES = "tickPreferences"
        val LAST_TICK_KEY = "lastTick"
        val TICK_LOG = "tickLog"

        // DON'T HOLD STATE IN STATIC VARIABLES

        fun startServiceRegister(context: Context, source: String) {
            TickJobIntentService.start(getIntent(context, true, source))
        }

        fun startServiceDebug(context: Context, source: String) {
            TickJobIntentService.start(getIntent(context, false, source))
        }

        fun getIntent(context: Context, silent: Boolean, source: String): Intent {
            Assert.assertTrue(!TextUtils.isEmpty(source))

            val intent = Intent(context, TickJobIntentService::class.java)
            intent.putExtra(SILENT_KEY, silent)
            intent.putExtra(SOURCE_KEY, source)
            return intent
        }

        fun tick(intent: Intent) {
            Assert.assertTrue(intent.hasExtra(SILENT_KEY))
            Assert.assertTrue(intent.hasExtra(SOURCE_KEY))

            val silent = intent.getBooleanExtra(SILENT_KEY, false)

            val sourceName = intent.getStringExtra(SOURCE_KEY)
            Assert.assertTrue(!TextUtils.isEmpty(sourceName))

            tick(silent, sourceName)
        }

        fun tick(silent: Boolean, sourceName: String) {
            val domainFactory = DomainFactory.getDomainFactory()

            if (domainFactory.isConnected) {
                if (domainFactory.isSaved) {
                    domainFactory.setFirebaseTickListener(MyApplication.instance, SaveService.Source.SERVICE, DomainFactory.TickData(silent, sourceName, MyApplication.instance))
                } else {
                    domainFactory.updateNotificationsTick(MyApplication.instance, SaveService.Source.SERVICE, silent, sourceName)
                }
            } else {
                domainFactory.updateNotificationsTick(MyApplication.instance, SaveService.Source.SERVICE, silent, sourceName)

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    domainFactory.setUserInfo(MyApplication.instance, SaveService.Source.SERVICE, UserInfo(firebaseUser))

                    domainFactory.setFirebaseTickListener(MyApplication.instance, SaveService.Source.SERVICE, DomainFactory.TickData(silent, sourceName, MyApplication.instance))
                }
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        tick(intent)
    }
}