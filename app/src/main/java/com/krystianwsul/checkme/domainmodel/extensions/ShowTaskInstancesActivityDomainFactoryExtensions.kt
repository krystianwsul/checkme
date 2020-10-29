package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

@Synchronized
fun DomainFactory.getShowTaskInstancesData(
        taskKey: TaskKey,
        page: Int
): ShowTaskInstancesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTaskInstancesData")

    val task = getTaskForce(taskKey)
    val now = ExactTimeStamp.now

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val isRootTask = if (task.current(now)) task.isRootTask(now) else null

    val desiredCount = (page + 1) * PAGE_SIZE

    val instancesPlusExtra = task.getInstances(null, null, now)
            .instances
            .take(desiredCount + 1)
            .toList()

    val instances = instancesPlusExtra.take(desiredCount)

    val hasMore = instances.size < instancesPlusExtra.size

    val instanceDatas = instances.map {
        val children = getChildInstanceDatas(it, now)

        val instanceData = GroupListDataWrapper.InstanceData(
                it.done,
                it.instanceKey,
                it.instanceDateTime.getDisplayText(),
                it.name,
                it.instanceDateTime.timeStamp,
                task.current(now),
                task.isVisible(now, false),
                it.isRootInstance(now),
                isRootTask,
                it.exists(),
                it.getCreateTaskTimePair(ownerKey),
                task.note,
                children,
                it.task.ordinal,
                it.getNotificationShown(localFactory),
                task.getImage(deviceDbInfo),
                it.isRepeatingGroupChild(now)
        )

        children.values.forEach { it.instanceDataParent = instanceData }

        instanceData
    }

    val dataWrapper = GroupListDataWrapper(
            customTimeDatas,
            task.current(now),
            listOf(),
            null,
            instanceDatas,
            null
    )

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    return ShowTaskInstancesViewModel.Data(dataWrapper, hasMore)
}