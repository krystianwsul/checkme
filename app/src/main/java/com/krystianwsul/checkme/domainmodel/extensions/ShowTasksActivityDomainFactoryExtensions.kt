package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

@Synchronized
fun DomainFactory.getShowTasksData(taskKeys: List<TaskKey>?): ShowTasksViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTasksData")

    val now = ExactTimeStamp.now

    val tasks = taskKeys?.map { getTaskForce(it) }
            ?.asSequence()
            ?: getUnscheduledTasks(now)

    val taskDatas = tasks.map {
        val hierarchyExactTimeStamp = it.getHierarchyExactTimeStamp(now)
        Pair(it, hierarchyExactTimeStamp)
    }
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
                        false,
                        task.ordinal
                )
            }
            .sortedDescending()
            .toList()

    return ShowTasksViewModel.Data(TaskListFragment.TaskData(taskDatas, null, true))
}