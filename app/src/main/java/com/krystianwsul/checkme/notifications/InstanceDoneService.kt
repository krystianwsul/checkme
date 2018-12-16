package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
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
            val kotlinDomainFactory = DomainFactory.getKotlinDomainFactory()

            if (kotlinDomainFactory.getIsConnected()) {
                if (kotlinDomainFactory.getIsConnectedAndSaved()) {
                    Preferences.logLineHour("InstanceHourService.throttle a")

                    queueFirebase(kotlinDomainFactory, firebaseListener)
                } else {
                    Preferences.logLineHour("InstanceHourService.throttle b")

                    firebaseListener(kotlinDomainFactory)
                }
            } else {
                if (needsFirebase) {
                    Preferences.logLineHour("InstanceHourService.throttle c")

                    queueFirebase(kotlinDomainFactory, firebaseListener)
                } else {
                    Preferences.logLineHour("InstanceHourService.throttle d")

                    firebaseListener(kotlinDomainFactory)
                }
            }
        }

        private fun queueFirebase(domainFactory: DomainFactory, firebaseListener: (DomainFactory) -> Unit) {
            check(!domainFactory.getIsConnected() || domainFactory.getIsConnectedAndSaved())

            if (!domainFactory.getIsConnected()) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                        ?: throw NeedsFirebaseException()

                domainFactory.setUserInfo(SaveService.Source.SERVICE, UserInfo(firebaseUser))
            }

            domainFactory.addFirebaseListener(firebaseListener)
        }
    }

    override fun onHandleIntent(intent: Intent) {
        Preferences.logLineDate("InstanceDoneService.start")

        check(intent.hasExtra(INSTANCE_KEY))
        check(intent.hasExtra(NOTIFICATION_ID_KEY))

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
        check(notificationId != -1)

        val notificationWrapper = NotificationWrapper.instance
        notificationWrapper.cleanGroup(notificationId) // todo uodpornić na podwójne kliknięcie

        throttleFirebase(instanceKey.type == TaskKey.Type.REMOTE) { setInstanceNotificationDone(it, instanceKey) }

        Preferences.logLineHour("InstanceDoneService.done")
    }

    private fun setInstanceNotificationDone(domainFactory: DomainFactory, instanceKey: InstanceKey) = domainFactory.setInstanceNotificationDone(SaveService.Source.SERVICE, instanceKey)

    private class NeedsFirebaseException : RuntimeException()
}
