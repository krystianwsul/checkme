package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

fun DomainFactory.getShowTaskData(requestTaskKey: TaskKey): ShowTaskViewModel.Data = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getShowTaskData")

    val taskKey = copiedTaskKeys[requestTaskKey] ?: requestTaskKey

    val now = ExactTimeStamp.Local.now

    val task = getTaskForce(taskKey)
    val parentHierarchyExactTimeStamp = task.getHierarchyExactTimeStamp(now)

    val childTaskDatas = task.getChildTaskHierarchies(parentHierarchyExactTimeStamp, true)
            .map { taskHierarchy ->
                val childTask = taskHierarchy.childTask

                val childHierarchyExactTimeStamp = childTask.getHierarchyExactTimeStamp(parentHierarchyExactTimeStamp)

                TaskListFragment.ChildTaskData(
                        childTask.name,
                        childTask.getScheduleText(ScheduleText, childHierarchyExactTimeStamp),
                        getTaskListChildTaskDatas(childTask, now, childHierarchyExactTimeStamp),
                        childTask.note,
                        childTask.taskKey,
                        childTask.getImage(deviceDbInfo),
                        childTask.current(now),
                        childTask.isVisible(now),
                        childTask.ordinal,
                        childTask.getProjectInfo(now),
                        childTask.isAssignedToMe(now, myUserFactory.user),
                )
            }
            .sorted()

    var collapseText = listOfNotNull(
            task.getParentName(parentHierarchyExactTimeStamp).takeIf { it.isNotEmpty() },
            task.getScheduleTextMultiline(ScheduleText, parentHierarchyExactTimeStamp)
                    .takeIf { it.isNotEmpty() }
    ).joinToString("\n\n")

    if (debugMode) {
        collapseText += "\n\nproject key: " + taskKey.projectKey
        collapseText += "\ntask id: " + taskKey.taskId
        collapseText += "\nstartTime: " + task.startExactTimeStampOffset
    }

    ShowTaskViewModel.Data(
            task.name,
            collapseText,
            TaskListFragment.TaskData(
                    childTaskDatas.toMutableList(),
                    task.note,
                    task.isVisible(now),
                    task.getProjectInfo(now),
            ),
            task.getImage(deviceDbInfo),
            task.current(now),
            taskKey,
    )
}