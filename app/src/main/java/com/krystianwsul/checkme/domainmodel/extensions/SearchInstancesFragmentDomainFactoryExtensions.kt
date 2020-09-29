package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.toExactTimeStamp
import com.krystianwsul.common.utils.ScheduleKey
import com.soywiz.klock.days

@Synchronized
fun DomainFactory.getSearchInstancesData(page: Int): SearchInstancesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    val now = ExactTimeStamp.now

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    var startExactTimeStamp: ExactTimeStamp? = null
    var endExactTimeStamp = now

    val instances = mutableMapOf<ScheduleKey, Instance<*>>()

    var hasMore = true
    while (hasMore) {
        val (newInstances, newHasMore) = getRootInstances(
                startExactTimeStamp,
                endExactTimeStamp,
                now
        )

        if (!newHasMore)
            hasMore = false

        instances += newInstances.associateBy { it.scheduleKey }

        if (instances.size > (page + 1) * 20)
            break

        startExactTimeStamp = endExactTimeStamp

        endExactTimeStamp = endExactTimeStamp.toDateTimeSoy()
                .plus(1.days)
                .toExactTimeStamp()
    }

    val instanceDatas = instances.values.map {
        val task = it.task

        val isRootTask = if (task.current(now)) task.isRootTask(now) else null

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
            null,
            listOf(),
            null,
            instanceDatas,
            null
    )

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    return SearchInstancesViewModel.Data(dataWrapper, hasMore)
}