package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey

@Synchronized
fun DomainFactory.getShowInstanceData(instanceKey: InstanceKey): ShowInstanceViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowInstanceData")

    val task = getTaskForce(instanceKey.taskKey)

    val now = ExactTimeStamp.now

    val instance = getInstance(instanceKey)
    val instanceDateTime = instance.instanceDateTime
    val parentInstance = instance.getParentInstance(now)

    val displayText = listOfNotNull(
        instance.getParentName(now).takeIf { it.isNotEmpty() },
        instanceDateTime.getDisplayText().takeIf { instance.isRootInstance(now) }
    ).joinToString("\n\n")

    return ShowInstanceViewModel.Data(
        instance.name,
        instanceDateTime,
        instance.done != null,
        task.current(now),
        parentInstance == null,
        instance.exists(),
        getGroupListData(instance, task, now),
        instance.getNotificationShown(localFactory),
        displayText,
        task.taskKey,
        instance.isRepeatingGroupChild(now)
    )
}

@Synchronized
fun DomainFactory.setTaskEndTimeStamps(
    source: SaveService.Source,
    taskKeys: Set<TaskKey>,
    deleteInstances: Boolean,
    instanceKey: InstanceKey
): Pair<TaskUndoData, Boolean> {
    MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    val taskUndoData = setTaskEndTimeStamps(source, taskKeys, deleteInstances, now)

    val instance = getInstance(instanceKey)
    val task = instance.task

    val instanceExactTimeStamp by lazy {
        instance.instanceDateTime
            .timeStamp
            .toExactTimeStamp()
    }

    val visible =
        task.notDeleted(now) || (instance.done != null || instanceExactTimeStamp <= now) || (!deleteInstances && instance.exists())

    return Pair(taskUndoData, visible)
}