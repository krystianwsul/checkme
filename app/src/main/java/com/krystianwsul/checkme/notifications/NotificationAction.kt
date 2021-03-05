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
import kotlinx.parcelize.Parcelize

sealed class NotificationAction : Parcelable {

    abstract val requestCode: Int

    abstract fun perform(callback: (() -> Unit)? = null)

    @Parcelize
    data class DeleteGroupNotification(private val instanceKeys: List<InstanceKey>) : NotificationAction() {

        override val requestCode get() = 0

        override fun perform(callback: (() -> Unit)?) {
            check(instanceKeys.isNotEmpty())

            DomainFactory.addFirebaseListener {
                it.setInstancesNotified(SaveService.Source.SERVICE, instanceKeys)
                callback?.invoke()
            }
        }
    }

    @Parcelize
    data class InstanceDone(
            private val instanceKey: InstanceKey,
            private val notificationId: Int,
            private val name: String,
            private val actionId: Int = 2
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform(callback: (() -> Unit)?) {
            Preferences.tickLog.logLineDate("InstanceDoneService.onHandleIntent")

            val notificationWrapper = NotificationWrapper.instance
            notificationWrapper.cleanGroup(notificationId)

            DomainFactory.addFirebaseListener("InstanceDoneService $name") {
                it.setInstanceNotificationDone(SaveService.Source.SERVICE, instanceKey)
                callback?.invoke()
            }
        }
    }

    @Parcelize
    data class InstanceHour(
            private val instanceKey: InstanceKey,
            private val notificationId: Int,
            private val name: String,
            private val actionId: Int = 3
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform(callback: (() -> Unit)?) {
            Preferences.tickLog.logLineDate("InstanceHourService.onHandleIntent")

            val notificationWrapper = NotificationWrapper.instance
            notificationWrapper.cleanGroup(notificationId)

            DomainFactory.addFirebaseListener("InstanceHourService $name") {
                it.setInstanceAddHourService(SaveService.Source.SERVICE, instanceKey)
                callback?.invoke()
            }
        }
    }

    @Parcelize
    data class DeleteInstanceNotification(
            val instanceKey: InstanceKey,
            private val actionId: Int = 4
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform(callback: (() -> Unit)?) {
            DomainFactory.addFirebaseListener {
                it.throwIfSaved()
                it.setInstanceNotified(0, SaveService.Source.SERVICE, instanceKey)
                callback?.invoke()
            }
        }
    }
}