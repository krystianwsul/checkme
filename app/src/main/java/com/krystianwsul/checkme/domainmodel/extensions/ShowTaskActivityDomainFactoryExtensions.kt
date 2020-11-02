package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

fun DomainFactory.getShowTaskData(taskKey: TaskKey): ShowTaskViewModel.Data = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getShowTaskData")

    val now = ExactTimeStamp.now

    val task = getTaskForce(taskKey)
    val hierarchyTimeStamp = task.getHierarchyExactTimeStamp(now)

    val childTaskDatas = task.getChildTaskHierarchies(hierarchyTimeStamp, true)
            .map { taskHierarchy ->
                val childTask = taskHierarchy.childTask

                TaskListFragment.ChildTaskData(
                        childTask.name,
                        childTask.getScheduleText(ScheduleText, hierarchyTimeStamp),
                        getTaskListChildTaskDatas(childTask, now, true, hierarchyTimeStamp),
                        childTask.note,
                        childTask.startExactTimeStamp,
                        childTask.taskKey,
                        taskHierarchy.taskHierarchyKey,
                        childTask.getImage(deviceDbInfo),
                        childTask.current(now),
                        childTask.isVisible(now, false),
                        true,
                        childTask.ordinal
                )
            }
            .sorted()

    var collapseText = listOfNotNull(
            task.getParentName(hierarchyTimeStamp).takeIf { it.isNotEmpty() },
            task.getScheduleTextMultiline(ScheduleText, hierarchyTimeStamp)
                    .takeIf { it.isNotEmpty() }
    ).joinToString("\n\n")

    if (debugMode) {
        collapseText += "\n\nproject key: " + taskKey.projectKey
        collapseText += "\ntask id: " + taskKey.taskId
    }

    ShowTaskViewModel.Data(
            task.name,
            collapseText,
            TaskListFragment.TaskData(childTaskDatas.toMutableList(), task.note, task.isVisible(now, false)),
            task.getImage(deviceDbInfo),
            task.current(now)
    )
}