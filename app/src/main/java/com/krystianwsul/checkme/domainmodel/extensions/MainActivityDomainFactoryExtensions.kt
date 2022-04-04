package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.checkme.viewmodels.MainNoteViewModel
import com.krystianwsul.checkme.viewmodels.MainTaskViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.search.SearchContext
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import java.util.*

fun DomainFactory.getMainNoteData(
    showProjects: Boolean,
    searchCriteria: SearchCriteria,
    now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): MainNoteViewModel.Data {
    MyCrashlytics.log("DomainFactory.getMainNoteData")

    DomainThreadChecker.instance.requireDomainThread()

    return MainNoteViewModel.Data(
        TaskListFragment.TaskData(
            getMainData(now, showProjects, searchCriteria) { it.intervalInfo.isUnscheduled() },
            null,
            true,
            null,
        )
    )
}

fun DomainFactory.getMainTaskData(
    showProjects: Boolean,
    searchCriteria: SearchCriteria,
    now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): MainTaskViewModel.Data {
    MyCrashlytics.log("DomainFactory.getMainTaskData")

    DomainThreadChecker.instance.requireDomainThread()

    return MainTaskViewModel.Data(
        TaskListFragment.TaskData(getMainData(now, showProjects, searchCriteria), null, true, null)
    )
}

private fun DomainFactory.getMainData(
    now: ExactTimeStamp.Local,
    showProjects: Boolean,
    searchCriteria: SearchCriteria,
    filter: (Task) -> Boolean = { true },
): List<TaskListFragment.EntryData> {
    val searchContext = SearchContext.startSearch(searchCriteria)

    fun Collection<Task>.toChildTaskDatas(searchContext: SearchContext) = searchContext.search {
        asSequence()
            .filter(filter)
            .filter { it.isTopLevelTask() }
            .filterSearchCriteria(myUserFactory.user, now)
            .map { (task, filterResult) ->
                TaskListFragment.ChildTaskData(
                    task.name,
                    task.getScheduleText(ScheduleText),
                    getTaskListChildTaskDatas(task, now, getChildrenSearchContext(filterResult), true),
                    task.note,
                    task.taskKey,
                    task.getImage(deviceDbInfo),
                    task.notDeleted,
                    task.isVisible(now),
                    task.canMigrateDescription(now),
                    task.ordinal,
                    task.getProjectInfo(!showProjects),
                    filterResult.matchesSearch,
                )
            }
            .sortedDescending()
            .toList()
    }

    return if (debugMode) {
        rootTasksFactory.rootTasks
            .values
            .groupBy { it.projectId }
            .flatMap { (projectId, tasks) ->
                projectsFactory.getProjectForce(projectId).toEntryDatas(tasks.toChildTaskDatas(searchContext), showProjects)
            }
    } else {
        projectsFactory.projects
            .values
            .flatMap { it.toEntryDatas(it.getAllDependenciesLoadedTasks().toChildTaskDatas(searchContext), showProjects) }
    }
}

fun DomainFactory.getGroupListData(
    now: ExactTimeStamp.Local,
    position: Int,
    timeRange: Preferences.TimeRange,
    showAssigned: Boolean,
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

    val currentInstances = getRootInstances(
        startExactTimeStamp,
        endExactTimeStamp,
        now,
        SearchContext.startSearch(SearchCriteria(showAssignedToOthers = showAssigned)),
    ).map { it.first }.toList()

    if (position == 0 && timeRange == Preferences.TimeRange.DAY) {
        instanceInfo = currentInstances.count { it.exists() }.let { existingInstanceCount ->
            Pair(existingInstanceCount, currentInstances.size - existingInstanceCount)
        }
    }

    val customTimeDatas = getCurrentRemoteCustomTimes().map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDescriptors = currentInstances.map { instance ->
        val (notDoneChildInstanceDescriptors, doneChildInstanceDescriptors) = getChildInstanceDatas(instance, now)

        val instanceData = GroupListDataWrapper.InstanceData.fromInstance(
            instance,
            now,
            this,
            notDoneChildInstanceDescriptors,
            doneChildInstanceDescriptors,
            false,
        )

        GroupTypeFactory.InstanceDescriptor(
            instanceData,
            instance.instanceDateTime.toDateTimePair(),
            instance.groupByProject,
            instance,
        )
    }

    val (mixedInstanceDescriptors, doneInstanceDescriptors) = instanceDescriptors.splitDone()

    val dataWrapper = GroupListDataWrapper(
        customTimeDatas,
        null,
        listOf(),
        null,
        newMixedInstanceDataCollection(
            mixedInstanceDescriptors,
            GroupTypeFactory.SingleBridge.CompareBy.TIMESTAMP,
            GroupType.GroupingMode.Time(),
        ),
        doneInstanceDescriptors.toDoneSingleBridges(),
        null,
        null,
        DropParent.TopLevel(false),
    )

    return DayViewModel.DayData(dataWrapper)
}