package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey

class InstanceDoneService : IntentService("InstanceDoneService") {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"
        private const val NOTIFICATION_ID_KEY = "notificationId"

        fun getIntent(context: Context, instanceKey: InstanceKey, notificationId: Int) = Intent(context, InstanceDoneService::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            putExtra(NOTIFICATION_ID_KEY, notificationId)
        }

        fun throttleFirebase(needsFirebase: Boolean, firebaseListener: (DomainFactory) -> Unit) {
            val kotlinDomainFactory = KotlinDomainFactory.getKotlinDomainFactory()
            val domainFactory = kotlinDomainFactory.domainFactory

            if (kotlinDomainFactory.isConnected) {
                if (kotlinDomainFactory.isConnectedAndSaved) {
                    queueFirebase(kotlinDomainFactory, firebaseListener)
                } else {
                    firebaseListener(domainFactory)
                }
            } else {
                if (needsFirebase) {
                    queueFirebase(kotlinDomainFactory, firebaseListener)
                } else {
                    firebaseListener(domainFactory)
                }
            }
        }

        private fun queueFirebase(kotlinDomainFactory: KotlinDomainFactory, firebaseListener: (DomainFactory) -> Unit) {
            check(!kotlinDomainFactory.isConnected || kotlinDomainFactory.isConnectedAndSaved)

            if (!kotlinDomainFactory.isConnected) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                        ?: throw NeedsFirebaseException()

                kotlinDomainFactory.setUserInfo(SaveService.Source.SERVICE, UserInfo(firebaseUser))
            }

            kotlinDomainFactory.addFirebaseListener(firebaseListener)
        }
    }

    override fun onHandleIntent(intent: Intent) {
        check(intent.hasExtra(INSTANCE_KEY))
        check(intent.hasExtra(NOTIFICATION_ID_KEY))

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
        check(notificationId != -1)

        val notificationWrapper = NotificationWrapper.instance
        notificationWrapper.cleanGroup(notificationId) // todo uodpornić na podwójne kliknięcie

        throttleFirebase(instanceKey.type == TaskKey.Type.REMOTE) { setInstanceNotificationDone(it, instanceKey) }
    }

    private fun setInstanceNotificationDone(domainFactory: DomainFactory, instanceKey: InstanceKey) = domainFactory.setInstanceNotificationDone(SaveService.Source.SERVICE, instanceKey)

    private class NeedsFirebaseException : RuntimeException()
}
