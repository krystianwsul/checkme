package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.checkme.viewmodels.MainViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import java.util.*

fun DomainFactory.getMainData(): MainViewModel.Data = DomainFactory.syncOnDomain {
    MyCrashlytics.log("DomainFactory.getMainData")

    val now = ExactTimeStamp.Local.now

    val childTaskDatas = getTasks().map {
        val hierarchyDateTime = it.getHierarchyExactTimeStamp(now)
        Pair(it, hierarchyDateTime)
    }
            .filter { (task, hierarchyExactTimeStamp) -> task.isRootTask(hierarchyExactTimeStamp) }
            .map { (task, hierarchyExactTimeStamp) ->
                TaskListFragment.ChildTaskData(
                        task.name,
                        task.getScheduleText(ScheduleText, hierarchyExactTimeStamp),
                        getTaskListChildTaskDatas(
                                task,
                                now,
                                false,
                                hierarchyExactTimeStamp,
                                groups = true
                        ),
                        task.note,
                        task.taskKey,
                        null,
                        task.getImage(deviceDbInfo),
                        task.current(now),
                        task.isVisible(now, false),
                        false,
                        task.ordinal,
                        task.getProjectInfo(now),
                        task.isAssignedToMe(now, myUserFactory.user),
                )
            }
            .sortedDescending()
            .toMutableList()

    MainViewModel.Data(
            TaskListFragment.TaskData(childTaskDatas, null, true, null),
            myUserFactory.user.defaultTab
    )
}

fun DomainFactory.getGroupListData(
        now: ExactTimeStamp.Local,
        position: Int,
        timeRange: Preferences.TimeRange,
): DayViewModel.DayData = DomainFactory.syncOnDomain {
    MyCrashlytics.log("DomainFactory.getGroupListData")

    check(position >= 0)

    val startExactTimeStamp: ExactTimeStamp.Offset?
    val endExactTimeStamp: ExactTimeStamp.Offset

    if (position == 0) {
        startExactTimeStamp = null
    } else {
        val startCalendar = now.calendar

        when (timeRange) {
            Preferences.TimeRange.DAY -> startCalendar.add(Calendar.DATE, position)
            Preferences.TimeRange.WEEK -> {
                startCalendar.add(Calendar.WEEK_OF_YEAR, position)
                startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.firstDayOfWeek)
            }
            Preferences.TimeRange.MONTH -> {
                startCalendar.add(Calendar.MONTH, position)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        startExactTimeStamp = Date(startCalendar.toDateTimeTz()).toMidnightExactTimeStamp().toOffset()
    }

    val endCalendar = now.calendar

    when (timeRange) {
        Preferences.TimeRange.DAY -> endCalendar.add(Calendar.DATE, position + 1)
        Preferences.TimeRange.WEEK -> {
            endCalendar.add(Calendar.WEEK_OF_YEAR, position + 1)
            endCalendar.set(Calendar.DAY_OF_WEEK, endCalendar.firstDayOfWeek)
        }
        Preferences.TimeRange.MONTH -> {
            endCalendar.add(Calendar.MONTH, position + 1)
            endCalendar.set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    endExactTimeStamp = Date(endCalendar.toDateTimeTz()).toMidnightExactTimeStamp().toOffset()

    val currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now).toList()

    if (position == 0 && timeRange == Preferences.TimeRange.DAY) {
        instanceInfo = currentInstances.count { it.exists() }.let { existingInstanceCount ->
            Pair(existingInstanceCount, currentInstances.size - existingInstanceCount)
        }
    }

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val taskDatas = if (position == 0) {
        getUnscheduledTasks(now).map {
            GroupListDataWrapper.TaskData(
                    it.taskKey,
                    it.name,
                    getGroupListChildTaskDatas(it, now),
                    it.startExactTimeStamp,
                    it.note,
                    it.getImage(deviceDbInfo),
                    it.isAssignedToMe(now, myUserFactory.user),
            )
        }.toList()
    } else {
        listOf()
    }

    val instanceDatas = currentInstances.map { instance ->
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
                instance.getProjectInfo(now),
        )

        children.values.forEach { it.instanceDataParent = instanceData }

        instanceData
    }

    val dataWrapper = GroupListDataWrapper(
            customTimeDatas,
            null,
            taskDatas,
            null,
            instanceDatas,
            null,
            null
    )

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    DayViewModel.DayData(dataWrapper)
}