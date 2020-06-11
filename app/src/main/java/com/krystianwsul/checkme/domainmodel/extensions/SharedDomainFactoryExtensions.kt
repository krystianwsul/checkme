package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import java.util.*

@Synchronized
fun DomainFactory.setTaskEndTimeStamps(
        source: SaveService.Source,
        taskKeys: Set<TaskKey>,
        deleteInstances: Boolean
): TaskUndoData {
    MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    return setTaskEndTimeStamps(source, taskKeys, deleteInstances, ExactTimeStamp.now)
}

@Synchronized
fun DomainFactory.clearTaskEndTimeStamps(source: SaveService.Source, taskUndoData: TaskUndoData) {
    MyCrashlytics.log("DomainFactory.clearTaskEndTimeStamps")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    processTaskUndoData(taskUndoData, now)

    updateNotifications(now)

    save(0, source)

    val remoteProjects = taskUndoData.taskKeys
        .map { getTaskForce(it).project }
        .toSet()

    notifyCloud(remoteProjects)
}

@Synchronized
fun DomainFactory.setOrdinal(dataId: Int, taskKey: TaskKey, ordinal: Double) {
    MyCrashlytics.log("DomainFactory.setOrdinal")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    val task = getTaskForce(taskKey)

    task.ordinal = ordinal

    updateNotifications(now)

    save(dataId, SaveService.Source.GUI)

    notifyCloud(task.project)
}

@Synchronized
fun DomainFactory.setInstancesNotNotified(
    dataId: Int,
    source: SaveService.Source,
    instanceKeys: List<InstanceKey>
) {
    MyCrashlytics.log("DomainFactory.setInstancesNotNotified")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    instanceKeys.forEach {
        val instance = getInstance(it)
        check(instance.done == null)
        check(instance.instanceDateTime.timeStamp.toExactTimeStamp() <= now)
        check(!instance.getNotificationShown(localFactory))
        check(instance.isRootInstance(now))

        instance.setNotified(localFactory, false)
        instance.setNotificationShown(localFactory, false)
    }

    updateNotifications(now)

    save(dataId, source)
}

@Synchronized
fun DomainFactory.removeFromParent(source: SaveService.Source, instanceKeys: List<InstanceKey>) {
    MyCrashlytics.log("DomainFactory.setInstancesNotNotified")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    instanceKeys.forEach {
        getInstance(it).getParentInstance(now)!!
            .third!!
            .setEndExactTimeStamp(now)
    }

    updateNotifications(now)

    save(0, source)
}

@Synchronized
fun DomainFactory.setInstancesAddHourActivity(
    dataId: Int,
    source: SaveService.Source,
    instanceKeys: Collection<InstanceKey>
): DomainFactory.HourUndoData {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now
    val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

    val date = Date(calendar.toDateTimeTz())
    val hourMinute = HourMinute(calendar.toDateTimeTz())

    val instances = instanceKeys.map(this::getInstance)

    val instanceDateTimes = instances.associate { it.instanceKey to it.instanceDateTime }

    instances.forEach {
        it.setInstanceDateTime(
            localFactory,
            ownerKey,
            DateTime(date, Time.Normal(hourMinute)),
            now
        )
    }

    updateNotifications(now)

    save(dataId, source)

    val remoteProjects = instances.map { it.project }.toSet()

    notifyCloud(remoteProjects)

    return DomainFactory.HourUndoData(instanceDateTimes)
}

@Synchronized
fun DomainFactory.undoInstancesAddHour(
    dataId: Int,
    source: SaveService.Source,
    hourUndoData: DomainFactory.HourUndoData
) {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    val pairs = hourUndoData.instanceDateTimes.map { (instanceKey, instanceDateTime) ->
        Pair(
            getInstance(instanceKey), instanceDateTime
        )
    }

    pairs.forEach { (instance, instanceDateTime) ->
        instance.setInstanceDateTime(localFactory, ownerKey, instanceDateTime, now)
    }

    updateNotifications(now)

    save(dataId, source)

    val remoteProjects = pairs.map { it.first.project }.toSet()

    notifyCloud(remoteProjects)
}

@Synchronized
fun DomainFactory.setInstanceDone(
    dataId: Int,
    source: SaveService.Source,
    instanceKey: InstanceKey,
    done: Boolean
): ExactTimeStamp? {
    MyCrashlytics.log("DomainFactory.setInstanceDone")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    val instance = getInstance(instanceKey)

    instance.setDone(localFactory, done, now)

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(instance.project)

    return instance.done
}

@Synchronized
fun DomainFactory.setInstanceNotified(
    dataId: Int,
    source: SaveService.Source,
    instanceKey: InstanceKey
) {
    MyCrashlytics.log("DomainFactory.setInstanceNotified")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val instance = getInstance(instanceKey)

    Preferences.tickLog.logLineHour("DomainFactory: setting notified: ${instance.name}")
    setInstanceNotified(instanceKey)

    save(dataId, source)
}