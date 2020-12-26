package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

fun DomainFactory.getShowTasksData(taskKeys: List<TaskKey>?): ShowTasksViewModel.Data = DomainFactory.syncOnDomain {
    MyCrashlytics.log("DomainFactory.getShowTasksData")

    val now = ExactTimeStamp.Local.now

    val copying = taskKeys != null

    val tasks = taskKeys?.map { getTaskForce(it) }
            ?.asSequence()
            ?: getUnscheduledTasks(now)

    val taskDatas = tasks.map { Pair(it, it.getHierarchyExactTimeStamp(now)) }
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
                        task.current(now),
                        task.canAddSubtask(now),
                        task.ordinal,
                        task.getProjectInfo(now),
                        task.isAssignedToMe(now, myUserFactory.user),
                )
            }
            .sorted()
            .toList()

    ShowTasksViewModel.Data(TaskListFragment.TaskData(taskDatas, null, !copying, null))
}