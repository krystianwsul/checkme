package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

fun DomainFactory.getShowTasksData(taskKeys: List<TaskKey>?): ShowTasksViewModel.Data = DomainFactory.syncOnDomain {
    MyCrashlytics.log("DomainFactory.getShowTasksData")

    val now = ExactTimeStamp.Local.now

    val copying = taskKeys != null

    fun Task<*>.toChildTaskData(): TaskListFragment.ChildTaskData {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now)

        return TaskListFragment.ChildTaskData(
                name,
                getScheduleText(ScheduleText, hierarchyExactTimeStamp),
                getTaskListChildTaskDatas(
                        this,
                        now,
                        hierarchyExactTimeStamp,
                ),
                note,
                taskKey,
                getImage(deviceDbInfo),
                current(now),
                isVisible(now),
                ordinal,
                getProjectInfo(now),
                isAssignedToMe(now, myUserFactory.user),
        )
    }

    val entryDatas = taskKeys?.map { getTaskForce(it).toChildTaskData() }
            ?.sorted()
            ?.toList()
            ?: projectsFactory.projects
                    .values
                    .map {
                        val childTaskDatas = it.tasks
                                .filter { it.current(now) && it.isUnscheduled(now) }
                                .map { it.toChildTaskData() }

                        it.toProjectData(childTaskDatas)
                    }
                    .filter { it.children.isNotEmpty() }

    ShowTasksViewModel.Data(TaskListFragment.TaskData(entryDatas, null, !copying, null))
}