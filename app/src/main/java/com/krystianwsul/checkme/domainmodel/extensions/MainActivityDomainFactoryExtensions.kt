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
import com.krystianwsul.checkme.viewmodels.MainNoteViewModel
import com.krystianwsul.checkme.viewmodels.MainTaskViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import java.util.*

fun DomainFactory.getMainNoteData(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now): MainNoteViewModel.Data {
    MyCrashlytics.log("DomainFactory.getMainNoteData")

    DomainThreadChecker.instance.requireDomainThread()

    return MainNoteViewModel.Data(
        TaskListFragment.TaskData(
            getMainData(now) { it.intervalInfo.isUnscheduled() },
            null,
            true,
            null,
        )
    )
}

fun DomainFactory.getMainTaskData(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now): MainTaskViewModel.Data {
    MyCrashlytics.log("DomainFactory.getMainTaskData")

    DomainThreadChecker.instance.requireDomainThread()

    return MainTaskViewModel.Data(TaskListFragment.TaskData(getMainData(now), null, true, null))
}

private fun DomainFactory.getMainData(
    now: ExactTimeStamp.Local,
    filter: (Task) -> Boolean = { true },
): List<TaskListFragment.ProjectData> {
    fun Collection<Task>.toChildTaskDatas() = asSequence().filter(filter)
        .map { Pair(it, it.getHierarchyExactTimeStamp(now)) }
        .filter { (task, hierarchyExactTimeStamp) -> task.isTopLevelTask(hierarchyExactTimeStamp) }
        .map { (task, hierarchyExactTimeStamp) ->
            TaskListFragment.ChildTaskData(
                task.name,
                task.getScheduleText(ScheduleText, hierarchyExactTimeStamp),
                getTaskListChildTaskDatas(
                    task,
                    now,
                    hierarchyExactTimeStamp,
                ),
                task.note,
                task.taskKey,
                task.getImage(deviceDbInfo),
                task.notDeleted,
                task.isVisible(now),
                task.ordinal,
                task.getProjectInfo(now),
                task.isAssignedToMe(now, myUserFactory.user),
            )
        }
        .sortedDescending()
        .toList()

    return if (debugMode) {
        rootTasksFactory.rootTasks
            .values
            .groupBy { it.projectId }
            .map { (projectId, tasks) -> projectsFactory.getProjectForce(projectId).toProjectData(tasks.toChildTaskDatas()) }
    } else {
        projectsFactory.projects
            .values
            .map { it.toProjectData(it.getAllTasks().toChildTaskDatas()) }
    }.filter { it.children.isNotEmpty() }
}

fun DomainFactory.getGroupListData(
    now: ExactTimeStamp.Local,
    position: Int,
    timeRange: Preferences.TimeRange,
): DayViewModel.DayData {
    MyCrashlytics.log("DomainFactory.getGroupListData")

    DomainThreadChecker.instance.requireDomainThread()

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

    val customTimeDatas = getCurrentRemoteCustomTimes().map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDatas = currentInstances.map { instance ->
        val task = instance.task

        val children = getChildInstanceDatas(instance, now)

        GroupListDataWrapper.InstanceData(
            instance.done,
            instance.instanceKey,
            instance.getDisplayData()?.getDisplayText(),
            instance.name,
            instance.instanceDateTime.timeStamp,
            instance.instanceDateTime,
            task.notDeleted,
            instance.canAddSubtask(now),
            instance.isRootInstance(),
            instance.getCreateTaskTimePair(projectsFactory.privateProject),
            task.note,
            children,
            instance.task.ordinal,
            instance.getNotificationShown(shownFactory),
            task.getImage(deviceDbInfo),
            instance.isAssignedToMe(now, myUserFactory.user),
            instance.getProjectInfo(now),
            instance.getProject().projectKey as? ProjectKey.Shared,
        )
    }

    val dataWrapper = GroupListDataWrapper(
        customTimeDatas,
        null,
        listOf(),
        null,
        instanceDatas,
        null,
        null,
    )

    return DayViewModel.DayData(dataWrapper)
}