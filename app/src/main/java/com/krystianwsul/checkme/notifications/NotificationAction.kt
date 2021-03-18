package com.krystianwsul.checkme.notifications

import android.os.Parcelable
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceAddHourService
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotificationDone
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotified
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesNotified
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.core.Completable
import kotlinx.parcelize.Parcelize

sealed class NotificationAction : Parcelable {

    abstract val requestCode: Int

    @CheckResult
    abstract fun perform(): Completable

    @Parcelize
    data class DeleteGroupNotification(private val instanceKeys: List<InstanceKey>) : NotificationAction() {

        override val requestCode get() = 0

        override fun perform(): Completable {
            check(instanceKeys.isNotEmpty())

            return DomainFactory.onReady()
                    .doOnSuccess { it.setInstancesNotified(SaveService.Source.SERVICE, instanceKeys) }
                    .ignoreElement()
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

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("InstanceDoneService.onHandleIntent")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return DomainFactory.onReady()
                    .doOnSuccess { it.setInstanceNotificationDone(SaveService.Source.SERVICE, instanceKey) }
                    .ignoreElement()
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

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("InstanceHourService.onHandleIntent")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return DomainFactory.onReady()
                    .doOnSuccess { it.setInstanceAddHourService(SaveService.Source.SERVICE, instanceKey) }
                    .ignoreElement()
        }
    }

    @Parcelize
    data class DeleteInstanceNotification(
            val instanceKey: InstanceKey,
            private val actionId: Int = 4,
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform() = DomainFactory.onReady().flatMapCompletable {
            it.setInstanceNotified(0, SaveService.Source.SERVICE, instanceKey)
        }!!
    }
}