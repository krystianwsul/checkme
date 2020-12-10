package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AssignedNode
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey

fun DomainFactory.getShowNotificationGroupData(instanceKeys: Set<InstanceKey>): ShowNotificationGroupViewModel.Data = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getShowNotificationGroupData")

    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val instances = instanceKeys.map { getInstance(it) }
            .filter { it.isRootInstance(now) }
            .sortedBy { it.instanceDateTime }

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDatas = instances.map { instance ->
        val task = instance.task

        val isRootTask = if (task.current(now)) task.isRootTask(now) else null

        val children = getChildInstanceDatas(instance, now)

        val instanceData = GroupListDataWrapper.InstanceData(
                instance.done,
                instance.instanceKey,
                instance.getDisplayData(now)?.getDisplayText(),
                instance.name,
                instance.instanceDateTime.timeStamp,
                instance.instanceDateTime,
                task.current(now),
                task.isVisible(now, false),
                instance.isRootInstance(now),
                isRootTask,
                instance.exists(),
                instance.getCreateTaskTimePair(ownerKey),
                task.note,
                children,
                instance.task.ordinal,
                instance.getNotificationShown(localFactory),
                task.getImage(deviceDbInfo),
                instance.isRepeatingGroupChild(now),
                instance.isAssignedToMe(now, myUserFactory.user),
                AssignedNode.User.fromProjectUsers(instance.getAssignedTo(now)),
        )

        children.values.forEach { it.instanceDataParent = instanceData }

        instanceData
    }

    val dataWrapper = GroupListDataWrapper(
            customTimeDatas,
            null,
            listOf(),
            null,
            instanceDatas,
            null,
            listOf()
    )

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    ShowNotificationGroupViewModel.Data(dataWrapper)
}