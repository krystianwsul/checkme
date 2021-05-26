package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey

fun DomainFactory.getShowNotificationGroupData(instanceKeys: Set<InstanceKey>): ShowNotificationGroupViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowNotificationGroupData")

    DomainThreadChecker.instance.requireDomainThread()

    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val instances = instanceKeys.map { getInstance(it) }
        .filter { it.isRootInstance() }
        .sortedBy { it.instanceDateTime }

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDatas = instances.map { instance ->
        val task = instance.task

        val children = getChildInstanceDatas(instance, now)

        GroupListDataWrapper.InstanceData(
            instance.done,
            instance.instanceKey,
            instance.getDisplayData()?.getDisplayText(),
            instance.name,
            instance.instanceDateTime.timeStamp,
            instance.instanceDateTime,
            task.current(now),
            instance.canAddSubtask(now),
            instance.isRootInstance(),
            instance.getCreateTaskTimePair(now, projectsFactory.privateProject),
            task.note,
            children,
            instance.task.ordinal,
            instance.getNotificationShown(localFactory),
            task.getImage(deviceDbInfo),
            instance.isAssignedToMe(now, myUserFactory.user),
            instance.getProjectInfo(now),
            instance.task
                .project
                .projectKey as? ProjectKey.Shared,
        )
    }

    val dataWrapper = GroupListDataWrapper(
        customTimeDatas,
        null,
        listOf(),
        null,
        instanceDatas,
        null,
        null
    )

    return ShowNotificationGroupViewModel.Data(dataWrapper)
}