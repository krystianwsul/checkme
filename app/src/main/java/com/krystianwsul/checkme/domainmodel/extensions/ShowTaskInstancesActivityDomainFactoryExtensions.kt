package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current

fun DomainFactory.getShowTaskInstancesData(
        parameters: ShowTaskInstancesActivity.Parameters,
        page: Int,
): ShowTaskInstancesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTaskInstancesData")

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val desiredCount = (page + 1) * SEARCH_PAGE_SIZE

    val parent: Current
    val instanceSequence: Sequence<Instance>
    when (parameters) {
        is ShowTaskInstancesActivity.Parameters.Task -> {
            val task = getTaskForce(parameters.taskKey)

            parent = task
            instanceSequence = task.getInstances(null, null, now)
        }
        is ShowTaskInstancesActivity.Parameters.Project -> {
            val project = projectsFactory.getProjectForce(parameters.projectKey)

            parent = project
            instanceSequence = project.getRootInstances(null, null, now)
        }
    }

    val (instances, hasMore) = instanceSequence.filter {
        it.isVisible(now, Instance.VisibilityOptions(hack24 = true))
    }.takeAndHasMore(desiredCount)

    val instanceDatas = instances.map {
        val children = getChildInstanceDatas(it, now, includeProjectInfo = parameters.includeProjectInfo)

        GroupListDataWrapper.InstanceData(
                it.done,
                it.instanceKey,
                it.instanceDateTime.getDisplayText(),
                it.name,
                it.instanceDateTime.timeStamp,
                it.instanceDateTime,
            it.task.current(now),
            it.canAddSubtask(now),
            it.isRootInstance(),
            it.getCreateTaskTimePair(now, projectsFactory.privateProject),
            it.task.note,
            children,
            it.task.ordinal,
            it.getNotificationShown(localFactory),
            it.task.getImage(deviceDbInfo),
            it.isAssignedToMe(now, myUserFactory.user),
            it.takeIf { parameters.includeProjectInfo }?.getProjectInfo(now),
        )
    }

    val dataWrapper = GroupListDataWrapper(
            customTimeDatas,
            parent.current(now),
            listOf(),
            null,
            instanceDatas,
            null,
            null
    )

    return ShowTaskInstancesViewModel.Data(dataWrapper, hasMore)
}