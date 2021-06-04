package com.krystianwsul.checkme.notifications

import android.os.Parcelable
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Completable
import kotlinx.parcelize.Parcelize

sealed class NotificationAction : Parcelable {

    abstract val requestCode: Int

    @CheckResult
    abstract fun perform(): Completable

    @Parcelize
    data class DeleteGroupNotification(private val projectKey: ProjectKey.Shared? = null) : NotificationAction() {

        override val requestCode get() = 0

        override fun perform() = AndroidDomainUpdater.setInstancesNotifiedService(projectKey)
    }

    @Parcelize
    data class InstanceDone(
        private val instanceKey: InstanceKey,
        private val notificationId: Int,
        private val name: String,
        private val actionId: Int = 2,
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("InstanceDoneService.onHandleIntent")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return AndroidDomainUpdater.setInstanceNotificationDoneService(instanceKey)
        }
    }

    @Parcelize
    data class InstanceHour(
        private val instanceKey: InstanceKey,
        private val notificationId: Int,
        private val name: String,
        private val actionId: Int = 3,
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("InstanceHourService.onHandleIntent")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return AndroidDomainUpdater.setInstanceAddHourService(instanceKey)
        }
    }

    @Parcelize
    data class DeleteInstanceNotification(
        val instanceKey: InstanceKey,
        private val actionId: Int = 4,
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform() =
            AndroidDomainUpdater.setInstanceNotified(DomainListenerManager.NotificationType.All, instanceKey)
    }

    @Parcelize
    data class ProjectDone(
        private val projectKey: ProjectKey.Shared,
        private val notificationId: Int,
        private val actionId: Int = 4,
    ) : NotificationAction() {

        override val requestCode get() = hashCode()

        override fun perform(): Completable {
            Preferences.tickLog.logLineDate("ProjectDone")

            NotificationWrapper.instance.cleanGroup(notificationId)

            return AndroidDomainUpdater.setProjectNotificationDoneService(projectKey)
        }
    }
}