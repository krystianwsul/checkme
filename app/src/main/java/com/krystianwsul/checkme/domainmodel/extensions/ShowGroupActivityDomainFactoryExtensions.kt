package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.time.toDateTimeSoy
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp
import java.util.*

@Synchronized
fun DomainFactory.getShowGroupData(timeStamp: TimeStamp): ShowGroupViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowGroupData")

    val now = ExactTimeStamp.now

    val date = timeStamp.date
    val dayOfWeek = date.dayOfWeek
    val hourMinute = timeStamp.hourMinute

    val time =
            getCurrentRemoteCustomTimes(now).firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
                    ?: Time.Normal(hourMinute)

    val displayText = DateTime(date, time).getDisplayText()

    return ShowGroupViewModel.Data(displayText, getGroupListData(timeStamp, now))
}

private fun DomainFactory.getGroupListData(
        timeStamp: TimeStamp,
        now: ExactTimeStamp
): GroupListDataWrapper {
    val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
    val endTimeStamp = TimeStamp(endCalendar.toDateTimeSoy())

    val rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now).instances

    val currentInstances = rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(
                it.name,
                it.hourMinutes.toSortedMap()
        )
    }

    val instanceDatas = currentInstances.map { instance ->
        val task = instance.task

        val isRootTask = if (task.current(now)) task.isRootTask(now) else null

        val children = getChildInstanceDatas(instance, now)

        val instanceData = GroupListDataWrapper.InstanceData(
                instance.done,
                instance.instanceKey,
                null,
                instance.name,
                instance.instanceDateTime.timeStamp,
                task.current(now),
                task.isVisible(now, false),
                instance.isRootInstance(now),
                isRootTask,
                instance.exists(),
                instance.getCreateTaskTimePair(ownerKey),
                task.note,
                children,
                task.ordinal,
                instance.getNotificationShown(localFactory),
                task.getImage(deviceDbInfo),
                instance.isRepeatingGroupChild(now)
        )

        children.values.forEach { it.instanceDataParent = instanceData }

        instanceData
    }

    val dataWrapper =
            GroupListDataWrapper(customTimeDatas, null, listOf(), null, instanceDatas, null)

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    return dataWrapper
}