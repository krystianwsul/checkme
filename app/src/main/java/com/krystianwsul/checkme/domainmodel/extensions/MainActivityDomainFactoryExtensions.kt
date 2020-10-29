package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.checkme.viewmodels.MainViewModel
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMilli
import java.util.*

@Synchronized
fun DomainFactory.getMainData(): MainViewModel.Data {
    MyCrashlytics.log("DomainFactory.getMainData")

    val now = ExactTimeStamp.now

    val childTaskDatas = getTasks().map {
        val hierarchyExactTimeStamp = it.getHierarchyExactTimeStamp(now)
        Pair(it, hierarchyExactTimeStamp)
    }
            .filter { (task, hierarchyExactTimeStamp) -> task.isRootTask(hierarchyExactTimeStamp) }
            .map { (task, hierarchyExactTimeStamp) ->
                TaskListFragment.ChildTaskData(
                        task.name,
                        task.getScheduleText(ScheduleText, hierarchyExactTimeStamp),
                        getTaskListChildTaskDatas(task, now, false, hierarchyExactTimeStamp, true),
                        task.note,
                        task.startExactTimeStamp,
                        task.taskKey,
                        null,
                        task.getImage(deviceDbInfo),
                        task.current(now),
                        task.isVisible(now, false),
                        false,
                        task.ordinal
                )
            }
            .sortedDescending()
            .toMutableList()

    return MainViewModel.Data(
            TaskListFragment.TaskData(childTaskDatas, null, true),
            myUserFactory.user.defaultTab
    )
}

@Synchronized
fun DomainFactory.getGroupListData(
        now: ExactTimeStamp,
        position: Int,
        timeRange: MainActivity.TimeRange
): DayViewModel.DayData {
    MyCrashlytics.log("DomainFactory.getGroupListData")

    check(position >= 0)

    val startExactTimeStamp: ExactTimeStamp?
    val endExactTimeStamp: ExactTimeStamp

    if (position == 0) {
        startExactTimeStamp = null
    } else {
        val startCalendar = now.calendar

        when (timeRange) {
            MainActivity.TimeRange.DAY -> startCalendar.add(Calendar.DATE, position)
            MainActivity.TimeRange.WEEK -> {
                startCalendar.add(Calendar.WEEK_OF_YEAR, position)
                startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.firstDayOfWeek)
            }
            MainActivity.TimeRange.MONTH -> {
                startCalendar.add(Calendar.MONTH, position)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        startExactTimeStamp =
                ExactTimeStamp(Date(startCalendar.toDateTimeTz()), HourMilli(0, 0, 0, 0))
    }

    val endCalendar = now.calendar

    when (timeRange) {
        MainActivity.TimeRange.DAY -> endCalendar.add(Calendar.DATE, position + 1)
        MainActivity.TimeRange.WEEK -> {
            endCalendar.add(Calendar.WEEK_OF_YEAR, position + 1)
            endCalendar.set(Calendar.DAY_OF_WEEK, endCalendar.firstDayOfWeek)
        }
        MainActivity.TimeRange.MONTH -> {
            endCalendar.add(Calendar.MONTH, position + 1)
            endCalendar.set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    endExactTimeStamp = ExactTimeStamp(Date(endCalendar.toDateTimeTz()), HourMilli(0, 0, 0, 0))

    val currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now).toList()

    if (position == 0 && timeRange == MainActivity.TimeRange.DAY) {
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
                    it.getImage(deviceDbInfo)
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
                instance.isRepeatingGroupChild(now)
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
            null
    )

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    return DayViewModel.DayData(dataWrapper)
}

private fun DomainFactory.getGroupListChildTaskDatas(
        parentTask: Task<*>,
        now: ExactTimeStamp
): List<GroupListDataWrapper.TaskData> = parentTask.getChildTaskHierarchies(now).map {
    val childTask = it.childTask

    GroupListDataWrapper.TaskData(
            childTask.taskKey,
            childTask.name,
            getGroupListChildTaskDatas(childTask, now),
            childTask.startExactTimeStamp,
            childTask.note,
            childTask.getImage(deviceDbInfo)
    )
}