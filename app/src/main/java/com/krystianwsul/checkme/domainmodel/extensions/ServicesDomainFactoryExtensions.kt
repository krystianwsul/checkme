package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.Notifier
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import java.util.*

fun DomainUpdater.setInstanceAddHourService(instanceKey: InstanceKey): Completable =
        CompletableDomainUpdate.create("setInstanceAddHourService") { now ->
            val instance = getInstance(instanceKey)
            Preferences.tickLog.logLineHour("DomainFactory: adding hour to ${instance.name}")

            val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

            val date = Date(calendar.toDateTimeTz())
            val hourMinute = HourMinute(calendar.toDateTimeTz())

            instance.setInstanceDateTime(
                    localFactory,
                    DateTime(date, Time.Normal(hourMinute)),
                    this,
                    now,
            )

            instance.setNotificationShown(localFactory, false)

            DomainUpdater.Params(
                    Notifier.Params("setInstanceAddHourService ${instance.name}"),
                    DomainFactory.SaveParams(DomainListenerManager.NotificationType.All),
                    DomainFactory.CloudParams(instance.task.project),
            )
        }.perform(this)

fun DomainUpdater.setInstanceNotificationDoneService(instanceKey: InstanceKey): Completable =
        CompletableDomainUpdate.create("setInstanceNotificationDone") { now ->
            val instance = getInstance(instanceKey)

            setNotificationDone(instance, now)

            DomainUpdater.Params(
                Notifier.Params("setInstanceNotificationDone ${instance.name}"),
                DomainFactory.SaveParams(DomainListenerManager.NotificationType.All),
                DomainFactory.CloudParams(instance.task.project),
            )
        }.perform(this)

private fun DomainFactory.setNotificationDone(instance: Instance, now: ExactTimeStamp.Local) {
    instance.apply {
        Preferences.tickLog.logLineHour("DomainFactory: setting $name done")

        setDone(localFactory, true, now)
        setNotificationShown(localFactory, false)
    }
}

fun DomainUpdater.setProjectNotificationDoneService(projectKey: ProjectKey.Shared): Completable =
    CompletableDomainUpdate.create("setInstanceNotificationDone") { now ->
        val project = projectsFactory.getProjectForce(projectKey)
        val instances = Notifier.getNotificationInstances(this, now, projectKey)

        instances.forEach { setNotificationDone(it, now) }

        DomainUpdater.Params(
            Notifier.Params("setInstanceNotificationDone ${project.name}"),
            DomainFactory.SaveParams(DomainListenerManager.NotificationType.All),
            DomainFactory.CloudParams(project),
        )
    }.perform(this)

fun DomainUpdater.setInstancesNotifiedService(projectKey: ProjectKey.Shared?): Completable =
    CompletableDomainUpdate.create("setInstancesNotified") { now ->
        Notifier.getNotificationInstances(this, now, projectKey)
            .also { check(it.isNotEmpty()) }
            .forEach(::setInstanceNotified)

        DomainUpdater.Params(false, DomainListenerManager.NotificationType.All)
    }.perform(this)

fun DomainUpdater.setTaskImageUploadedService(
        taskKey: TaskKey,
        imageUuid: String,
): Completable = CompletableDomainUpdate.create("clearProjectEndTimeStamps") {
    val task = rootTasksFactory.getRootTaskIfPresent(taskKey as TaskKey.Root)
    if (task?.getImage(deviceDbInfo) != ImageState.Local(imageUuid)) {
        DomainUpdater.Params()
    } else {
        task.setImage(deviceDbInfo, ImageState.Remote(imageUuid))

        DomainUpdater.Params(
                false,
                DomainListenerManager.NotificationType.All,
                DomainFactory.CloudParams(task.project),
        )
    }
}.perform(this)