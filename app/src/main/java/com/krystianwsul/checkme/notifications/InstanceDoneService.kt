package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import junit.framework.Assert

class InstanceDoneService : IntentService("InstanceDoneService") {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"
        private const val NOTIFICATION_ID_KEY = "notificationId"

        fun getIntent(context: Context, instanceKey: InstanceKey, notificationId: Int) = Intent(context, InstanceDoneService::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            putExtra(NOTIFICATION_ID_KEY, notificationId)
        }

        fun throttleFirebase(context: Context, needsFirebase: Boolean, firebaseListener: DomainFactory.FirebaseListener) {
            val domainFactory = DomainFactory.getDomainFactory()

            if (domainFactory.isConnected) {
                if (domainFactory.isSaved) {
                    queueFirebase(domainFactory, context, firebaseListener)
                } else {
                    firebaseListener.onFirebaseResult(domainFactory)
                }
            } else {
                if (needsFirebase) {
                    queueFirebase(domainFactory, context, firebaseListener)
                } else {
                    firebaseListener.onFirebaseResult(domainFactory)
                }
            }
        }

        private fun queueFirebase(domainFactory: DomainFactory, context: Context, firebaseListener: DomainFactory.FirebaseListener) {
            Assert.assertTrue(!domainFactory.isConnected || domainFactory.isSaved)

            if (!domainFactory.isConnected) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                        ?: throw NeedsFirebaseException()

                domainFactory.setUserInfo(context.applicationContext, SaveService.Source.SERVICE, UserInfo(firebaseUser))
            }

            domainFactory.addFirebaseListener(firebaseListener)
        }
    }

    override fun onHandleIntent(intent: Intent) {
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY))
        Assert.assertTrue(intent.hasExtra(NOTIFICATION_ID_KEY))

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
        Assert.assertTrue(notificationId != -1)

        val notificationWrapper = NotificationWrapper.instance
        notificationWrapper.cleanGroup(notificationId) // todo uodpornić na podwójne kliknięcie

        throttleFirebase(this, instanceKey.type == TaskKey.Type.REMOTE, DomainFactory.FirebaseListener { setInstanceNotificationDone(it, instanceKey) })
    }

    private fun setInstanceNotificationDone(domainFactory: DomainFactory, instanceKey: InstanceKey) {
        domainFactory.setInstanceNotificationDone(this, SaveService.Source.SERVICE, instanceKey)
    }

    private class NeedsFirebaseException() : RuntimeException()
}
