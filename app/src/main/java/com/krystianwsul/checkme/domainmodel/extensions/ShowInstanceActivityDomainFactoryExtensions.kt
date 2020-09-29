package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
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

private fun DomainFactory.getGroupListData(
        instance: Instance<*>,
        task: Task<*>,
        now: ExactTimeStamp
): GroupListDataWrapper {
    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDatas = instance.getChildInstances(now).map { (childInstance, _) ->
        val childTask = childInstance.task

        val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

        val children = getChildInstanceDatas(childInstance, now)

        val instanceData = GroupListDataWrapper.InstanceData(
                childInstance.done,
                childInstance.instanceKey,
                null,
                childInstance.name,
                childInstance.instanceDateTime.timeStamp,
                childTask.current(now),
                childTask.isVisible(now, false),
                childInstance.isRootInstance(now),
                isRootTask,
                childInstance.exists(),
                childInstance.getCreateTaskTimePair(ownerKey),
                childTask.note,
                children,
                childTask.ordinal,
                childInstance.getNotificationShown(localFactory),
                childTask.getImage(deviceDbInfo),
                childInstance.isRepeatingGroupChild(now)
        )

        children.values.forEach { it.instanceDataParent = instanceData }

        instanceData
    }

    val dataWrapper = GroupListDataWrapper(
            customTimeDatas,
            task.isVisible(now, false),
            listOf(),
            task.note,
            instanceDatas,
            task.getImage(deviceDbInfo)
    )

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    return dataWrapper
}