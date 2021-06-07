package com.krystianwsul.checkme.notifications

import android.os.Parcelable
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Completable
import kotlinx.parcelize.Parcelize

sealed class NotificationAction : Parcelable {

    val requestCode get() = javaClass.hashCode() + hashCode()

    @CheckResult
    abstract fun perform(): Completable

    @Parcelize
    object DeleteGroupNotification : NotificationAction() {

        override fun perform() = AndroidDomainUpdater.setInstancesNotifiedService()
    }

    @Parcelize
    data class InstanceDone(private val instanceKey: InstanceKey, private val notificationId: Int) : NotificationAction() {

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("InstanceDoneService.onHandleIntent")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return AndroidDomainUpdater.setInstanceNotificationDoneService(instanceKey)
        }
    }

    @Parcelize
    data class InstanceHour(private val instanceKey: InstanceKey, private val notificationId: Int) : NotificationAction() {

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("InstanceHourService.onHandleIntent")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return AndroidDomainUpdater.setInstanceAddHourService(instanceKey)
        }
    }

    @Parcelize
    data class DeleteInstanceNotification(private val instanceKey: InstanceKey) : NotificationAction() {

        override fun perform() =
            AndroidDomainUpdater.setInstanceNotified(DomainListenerManager.NotificationType.All, instanceKey)
    }

    @Parcelize
    data class ProjectDone(
        private val projectKey: ProjectKey.Shared,
        private val timeStamp: TimeStamp,
        private val notificationId: Int,
    ) :
        NotificationAction() {

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("ProjectDone")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return AndroidDomainUpdater.setProjectNotificationDoneService(projectKey) // todo group
        }
    }

    @Parcelize
    data class ProjectHour(
        private val projectKey: ProjectKey.Shared,
        private val timeStamp: TimeStamp,
        private val notificationId: Int,
    ) : NotificationAction() {

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("ProjectHour")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return AndroidDomainUpdater.setProjectAddHourService(projectKey) // todo group TIMESTAMP
        }
    }

    @Parcelize
    data class DeleteProjectNotification(private val projectKey: ProjectKey.Shared, private val timeStamp: TimeStamp) :
        NotificationAction() {

        override fun perform() = AndroidDomainUpdater.setInstancesNotifiedService(projectKey, timeStamp)
    }
}