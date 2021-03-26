package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

fun DomainFactory.getShowTaskInstancesData(taskKey: TaskKey, page: Int): ShowTaskInstancesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTaskInstancesData")

    DomainThreadChecker.instance.requireDomainThread()

    val task = getTaskForce(taskKey)
    val now = ExactTimeStamp.Local.now

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val desiredCount = (page + 1) * SEARCH_PAGE_SIZE

    val (instances, hasMore) = task.getInstances(
            null,
            null,
            now
    )
            .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true)) }
            .takeAndHasMore(desiredCount)

    val instanceDatas = instances.map {
        val children = getChildInstanceDatas(it, now)

        GroupListDataWrapper.InstanceData(
                it.done,
                it.instanceKey,
                it.instanceDateTime.getDisplayText(),
                it.name,
                it.instanceDateTime.timeStamp,
                it.instanceDateTime,
                task.current(now),
                it.canAddSubtask(now),
                it.isRootInstance(),
                it.getCreateTaskTimePair(now, projectsFactory.privateProject),
                task.note,
                children,
                it.task.ordinal,
                it.getNotificationShown(localFactory),
                task.getImage(deviceDbInfo),
                it.isAssignedToMe(now, myUserFactory.user),
                it.getProjectInfo(now),
        )
    }

    val dataWrapper = GroupListDataWrapper(
            customTimeDatas,
            task.current(now),
            listOf(),
            null,
            instanceDatas,
            null,
            null
    )

    return ShowTaskInstancesViewModel.Data(dataWrapper, hasMore)
}