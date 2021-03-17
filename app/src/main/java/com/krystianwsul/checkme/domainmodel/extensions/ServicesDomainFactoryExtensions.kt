package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import java.util.*

fun DomainFactory.setInstanceAddHourService(source: SaveService.Source, instanceKey: InstanceKey) {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourService")

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

    if (projectsFactory.isSaved) throw SavedFactoryException()

    val instance = getInstance(instanceKey)
    Preferences.tickLog.logLineHour("DomainFactory: adding hour to ${instance.name}")

    val now = ExactTimeStamp.Local.now
    val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

    val date = Date(calendar.toDateTimeTz())
    val hourMinute = HourMinute(calendar.toDateTimeTz())

    instance.setInstanceDateTime(
            localFactory,
            ownerKey,
            DateTime(date, Time.Normal(hourMinute)),
    )
    instance.setNotificationShown(localFactory, false)

    updateNotifications(now, sourceName = "setInstanceAddHourService ${instance.name}")

    save(0, source)

    notifyCloud(instance.task.project)
}

fun DomainFactory.setInstanceNotificationDone(source: SaveService.Source, instanceKey: InstanceKey) {
    MyCrashlytics.log("DomainFactory.setInstanceNotificationDone")

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

    if (projectsFactory.isSaved) throw SavedFactoryException()

    val instance = getInstance(instanceKey)
    Preferences.tickLog.logLineHour("DomainFactory: setting ${instance.name} done")

    val now = ExactTimeStamp.Local.now

    instance.setDone(localFactory, true, now)
    instance.setNotificationShown(localFactory, false)

    updateNotifications(now, sourceName = "setInstanceNotificationDone ${instance.name}")

    save(0, source)

    notifyCloud(instance.task.project)
}

fun DomainFactory.setInstancesNotified(source: SaveService.Source, instanceKeys: List<InstanceKey>) {
    MyCrashlytics.log("DomainFactory.setInstancesNotified")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

    check(instanceKeys.isNotEmpty())

    for (instanceKey in instanceKeys)
        setInstanceNotified(instanceKey)

    save(0, source)
}

fun DomainFactory.setTaskImageUploaded(
        source: SaveService.Source,
        taskKey: TaskKey,
        imageUuid: String,
) {
    MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

    if (projectsFactory.isSaved) throw SavedFactoryException()

    val task = getTaskIfPresent(taskKey)
    if (task?.getImage(deviceDbInfo) != ImageState.Local(imageUuid)) return

    task.setImage(deviceDbInfo, ImageState.Remote(imageUuid))

    save(0, source)

    notifyCloud(task.project)
}