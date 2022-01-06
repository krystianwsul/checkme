package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
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

    val now = ExactTimeStamp.Local.now

    val queriedInstances = instanceKeys.map { getInstance(it) }.filter { it.isRootInstance() }
    val notificationInstances = Notifier.getNotificationInstances(this, now)

    val instances = (queriedInstances + notificationInstances).distinct().sortedBy { it.instanceDateTime }

    val customTimeDatas = getCurrentRemoteCustomTimes().map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDescriptors = instances.map { instance ->
        val task = instance.task

        val (notDoneChildInstanceDescriptors, doneChildInstanceDescriptors) =
            getChildInstanceDatas(instance, now).splitDone()

        val instanceData = GroupListDataWrapper.InstanceData(
            instance.done,
            instance.instanceKey,
            instance.getDisplayData()?.getDisplayText(),
            instance.name,
            instance.instanceDateTime.timeStamp,
            instance.instanceDate,
            task.notDeleted,
            instance.canAddSubtask(now),
            instance.canMigrateDescription(now),
            instance.isRootInstance(),
            instance.getCreateTaskTimePair(projectsFactory.privateProject, myUserFactory.user),
            task.note,
            newMixedInstanceDataCollection(notDoneChildInstanceDescriptors),
            doneChildInstanceDescriptors.toInstanceDatas(),
            instance.task.ordinal,
            instance.getNotificationShown(shownFactory),
            task.getImage(deviceDbInfo),
            instance.isAssignedToMe(now, myUserFactory.user),
            instance.getProjectInfo(now),
            instance.getProject().projectKey as? ProjectKey.Shared,
        )

        GroupTypeFactory.InstanceDescriptor(instanceData, instance.instanceDateTime.toDateTimePair())
    }

    val (mixedInstanceDatas, doneInstanceDatas) = instanceDescriptors.splitDone()

    val dataWrapper = GroupListDataWrapper(
        customTimeDatas,
        null,
        listOf(),
        null,
        newMixedInstanceDataCollection(mixedInstanceDatas, GroupType.GroupingMode.Projects),
        doneInstanceDatas.toInstanceDatas(),
        null,
        null
    )

    return ShowNotificationGroupViewModel.Data(dataWrapper)
}