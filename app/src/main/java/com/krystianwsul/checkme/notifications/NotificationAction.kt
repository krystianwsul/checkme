package com.krystianwsul.checkme.notifications

import android.os.Parcelable
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.DomainUpdater
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceAddHourService
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotificationDoneService
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotified
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesNotifiedService
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
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
                    .doOnSuccess { it.setInstancesNotifiedService(instanceKeys) }
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

            return DomainUpdater().setInstanceNotificationDoneService(instanceKey)
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

            return DomainUpdater().setInstanceAddHourService(instanceKey)
        }
    }

    @Parcelize
    data class DeleteInstanceNotification(
            val instanceKey: InstanceKey,
            private val actionId: Int = 4,
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform() = DomainFactory.onReady().flatMapCompletable {
            it.setInstanceNotified(DomainListenerManager.NotificationType.All, instanceKey)
        }!!
    }
}