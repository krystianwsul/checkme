package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

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