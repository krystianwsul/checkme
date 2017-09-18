package com.krystianwsul.checkme.notifications

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.commonsware.cwac.wakeful.WakefulIntentService
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import junit.framework.Assert

class TickService : WakefulIntentService("TickService") {

    companion object {

        val MAX_NOTIFICATIONS = 3
        val GROUP_KEY = "group"

        private val SILENT_KEY = "silent"
        private val SOURCE_KEY = "source"

        val TICK_PREFERENCES = "tickPreferences"
        val LAST_TICK_KEY = "lastTick"
        val TICK_LOG = "tickLog"

        // DON'T HOLD STATE IN STATIC VARIABLES

        fun startServiceRegister(context: Context, source: String) {
            WakefulIntentService.sendWakefulWork(context, getIntent(context, true, source))
        }

        fun startServiceTimeChange(context: Context, source: String) {
            WakefulIntentService.sendWakefulWork(context, getIntent(context, true, source))
        }

        fun startServiceDebug(context: Context, source: String) {
            WakefulIntentService.sendWakefulWork(context, getIntent(context, false, source))
        }

        fun getIntent(context: Context, silent: Boolean, source: String): Intent {
            Assert.assertTrue(!TextUtils.isEmpty(source))

            val intent = Intent(context, TickService::class.java)
            intent.putExtra(SILENT_KEY, silent)
            intent.putExtra(SOURCE_KEY, source)
            return intent
        }

        fun tick(intent: Intent) {
            Assert.assertTrue(intent.hasExtra(SILENT_KEY))
            Assert.assertTrue(intent.hasExtra(SOURCE_KEY))

            val silent = intent.getBooleanExtra(SILENT_KEY, false)

            val source = intent.getStringExtra(SOURCE_KEY)
            Assert.assertTrue(!TextUtils.isEmpty(source))

            val domainFactory = DomainFactory.getDomainFactory(MyApplication.instance)

            if (domainFactory.isConnected) {
                if (domainFactory.isSaved) {
                    domainFactory.setFirebaseTickListener(MyApplication.instance, DomainFactory.TickData(silent, source, MyApplication.instance))
                } else {
                    domainFactory.updateNotificationsTick(MyApplication.instance, silent, source)
                }
            } else {
                domainFactory.updateNotificationsTick(MyApplication.instance, silent, source)

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    domainFactory.setUserInfo(MyApplication.instance, UserInfo(firebaseUser))

                    domainFactory.setFirebaseTickListener(MyApplication.instance, DomainFactory.TickData(silent, source, MyApplication.instance))
                }
            }
        }
    }

    override fun doWakefulWork(intent: Intent) {
        tick(intent)
    }
}
