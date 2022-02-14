package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

fun DomainFactory.getShowTaskData(requestTaskKey: TaskKey): ShowTaskViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTaskData")

    DomainThreadChecker.instance.requireDomainThread()

    val taskKey = copiedTaskKeys[requestTaskKey] ?: requestTaskKey

    val now = ExactTimeStamp.Local.now

    val task = getTaskForce(taskKey)

    val childTaskDatas = task.getChildTasks()
        .map { childTask ->
            TaskListFragment.ChildTaskData(
                childTask.name,
                childTask.getScheduleText(ScheduleText),
                getTaskListChildTaskDatas(childTask, now),
                childTask.note,
                childTask.taskKey,
                childTask.getImage(deviceDbInfo),
                childTask.notDeleted,
                childTask.isVisible(now),
                childTask.canMigrateDescription(now),
                childTask.ordinal,
                childTask.getProjectInfo(),
                childTask.isAssignedToMe(myUserFactory.user),
            )
        }
        .sorted()

    var collapseText = listOfNotNull(
        task.parentTask?.name,
        task.getScheduleTextMultiline(ScheduleText).takeIf { it.isNotEmpty() },
    ).joinToString("\n\n")

    if (debugMode) {
        collapseText += "\n\ntaskKey: $taskKey"
        collapseText += "\nstartTime: " + task.startExactTimeStampOffset
        collapseText += "\nendTime:" + task.endData?.exactTimeStampOffset
        collapseText += "\nisVisible: " + task.isVisibleHelper(now).let { it.first.toString() + ", " + it.second }
    }

    return ShowTaskViewModel.Data(
        task.name,
        collapseText,
        TaskListFragment.TaskData(
            childTaskDatas.toMutableList(),
            task.note,
            task.isVisible(now),
            task.getProjectInfo(),
        ),
        task.getImage(deviceDbInfo),
        task.notDeleted,
        task.canMigrateDescription(now),
        taskKey,
    )
}