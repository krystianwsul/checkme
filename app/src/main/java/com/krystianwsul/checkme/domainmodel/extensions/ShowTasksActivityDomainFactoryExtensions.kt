package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.common.time.ExactTimeStamp

@Synchronized
fun DomainFactory.getShowTasksData(): ShowTasksViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTasksData")

    val now = ExactTimeStamp.now

    val taskDatas = getUnscheduledTasks(now).map {
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