package com.krystianwsul.checkme.notifications

import android.os.Parcelable
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceAddHourService
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotificationDone
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotified
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesNotified
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey
import kotlinx.android.parcel.Parcelize

sealed class NotificationAction : Parcelable {

    protected abstract val actionId: Int // to generate unique hashCode

    abstract fun perform()

    @Parcelize
    data class DeleteGroupNotification(
            private val instanceKeys: List<InstanceKey>,
            override val actionId: Int = 1
    ) : NotificationAction() {

        override fun perform() {
            check(instanceKeys.isNotEmpty())

            DomainFactory.addFirebaseListener {
                it.setInstancesNotified(SaveService.Source.SERVICE, instanceKeys)
            }
        }
    }

    @Parcelize
    data class InstanceDone(
            private val instanceKey: InstanceKey,
            private val notificationId: Int,
            private val name: String,
            override val actionId: Int = 2
    ) : NotificationAction() {

        override fun perform() {
            Preferences.tickLog.logLineDate("InstanceDoneService.onHandleIntent")

            val notificationWrapper = NotificationWrapper.instance
            notificationWrapper.cleanGroup(notificationId)

            DomainFactory.addFirebaseListener("InstanceDoneService $name") {
                it.setInstanceNotificationDone(SaveService.Source.SERVICE, instanceKey)
            }
        }
    }

    @Parcelize
    data class InstanceHour(
            private val instanceKey: InstanceKey,
            private val notificationId: Int,
            private val name: String,
            override val actionId: Int = 3
    ) : NotificationAction() {

        override fun perform() {
            Preferences.tickLog.logLineDate("InstanceHourService.onHandleIntent")

            val notificationWrapper = NotificationWrapper.instance
            notificationWrapper.cleanGroup(notificationId)

            DomainFactory.addFirebaseListener("InstanceHourService $name") {
                it.setInstanceAddHourService(SaveService.Source.SERVICE, instanceKey)
            }
        }
    }

    @Parcelize
    data class DeleteInstanceNotification(
            val instanceKey: InstanceKey,
            override val actionId: Int = 4
    ) : NotificationAction() {

        override fun perform() {
            DomainFactory.addFirebaseListener {
                it.checkSave()
                it.setInstanceNotified(0, SaveService.Source.SERVICE, instanceKey)
            }
        }
    }
}