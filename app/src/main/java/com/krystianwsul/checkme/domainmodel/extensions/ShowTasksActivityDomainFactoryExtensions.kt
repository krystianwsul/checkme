package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.gui.tree.AssignedNode
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
                                false,
                                hierarchyExactTimeStamp,
                                true
                        ),
                        task.note,
                        task.taskKey,
                        null,
                        task.getImage(deviceDbInfo),
                        task.current(now),
                        task.isVisible(now, false),
                        false,
                        task.ordinal,
                        AssignedNode.User.fromProjectUsers(task.getAssignedTo(now)),
                )
            }
            .sorted()
            .toList()

    ShowTasksViewModel.Data(TaskListFragment.TaskData(taskDatas, null, !copying, listOf()))
}