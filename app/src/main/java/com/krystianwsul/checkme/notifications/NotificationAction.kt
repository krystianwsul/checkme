package com.krystianwsul.checkme.notifications

import android.os.Parcelable
import androidx.annotation.CheckResult
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.firebase.database.DomainFactoryInitializationDelayProvider
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapper
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapperQueue
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Completable
import kotlinx.parcelize.Parcelize

sealed class NotificationAction : Parcelable, TaskPriorityMapperQueue.Provider {

    val requestCode get() = javaClass.hashCode() + hashCode()

    override fun newTaskPriorityMapper(): TaskPriorityMapper? = null

    @CheckResult
    abstract fun perform(): Completable

    @Parcelize
    object DeleteGroupNotification : NotificationAction() {

        override fun newDelayProvider(): DomainFactoryInitializationDelayProvider? = null

        override fun perform() = AndroidDomainUpdater.setInstancesNotifiedService()
    }

    sealed class Instance : NotificationAction() {

        protected abstract val instanceKey: InstanceKey

        override fun newDelayProvider() = DomainFactoryInitializationDelayProvider.Task.fromTaskKey(instanceKey.taskKey)

        override fun newTaskPriorityMapper(): TaskPriorityMapper? = null

        @Parcelize
        data class Done(override val instanceKey: InstanceKey, private val notificationId: Int) : Instance() {

            override fun perform(): Completable {
                Preferences.tickLog.logLineDate("InstanceDoneService.onHandleIntent")

                NotificationWrapper.instance.cleanGroup(notificationId)

                return AndroidDomainUpdater.setInstanceNotificationDoneService(instanceKey)
            }
        }

        @Parcelize
        data class Hour(override val instanceKey: InstanceKey, private val notificationId: Int) : Instance() {

            override fun perform(): Completable {
                Preferences.tickLog.logLineDate("InstanceHourService.onHandleIntent")

                NotificationWrapper.instance.cleanGroup(notificationId)

                return AndroidDomainUpdater.setInstanceAddHourService(instanceKey)
            }
        }

        @Parcelize
        data class Delete(override val instanceKey: InstanceKey) : Instance() {

            override fun perform() =
                AndroidDomainUpdater.setInstanceNotified(DomainListenerManager.NotificationType.All, instanceKey)
        }
    }

    sealed class Project : NotificationAction() {

        override fun newDelayProvider(): DomainFactoryInitializationDelayProvider? = null

        @Parcelize
        data class Done(
            private val projectKey: ProjectKey.Shared,
            private val timeStamp: TimeStamp,
            private val notificationId: Int,
        ) : Project() {

            override fun perform(): Completable {
                Preferences.tickLog.logLineDate("ProjectDone")

                NotificationWrapper.instance.cleanGroup(notificationId)

                return AndroidDomainUpdater.setProjectNotificationDoneService(projectKey, timeStamp)
            }
        }

        @Parcelize
        data class Hour(
            private val projectKey: ProjectKey.Shared,
            private val timeStamp: TimeStamp,
            private val notificationId: Int,
        ) : Project() {

            override fun perform(): Completable {
                Preferences.tickLog.logLineDate("ProjectHour")

                NotificationWrapper.instance.cleanGroup(notificationId)

                return AndroidDomainUpdater.setProjectAddHourService(projectKey, timeStamp)
            }
        }

        @Parcelize
        data class Delete(private val projectKey: ProjectKey.Shared, private val timeStamp: TimeStamp) : Project() {

            override fun perform() = AndroidDomainUpdater.setInstancesNotifiedService(projectKey, timeStamp)
        }
    }
}