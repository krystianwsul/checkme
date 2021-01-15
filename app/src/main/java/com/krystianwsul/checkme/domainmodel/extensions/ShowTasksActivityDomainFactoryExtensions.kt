package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp

fun DomainFactory.getShowTasksData(parameters: ShowTasksActivity.Parameters): ShowTasksViewModel.Data = DomainFactory.syncOnDomain {
    MyCrashlytics.log("DomainFactory.getShowTasksData")

    val now = ExactTimeStamp.Local.now

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

    val entryDatas: List<TaskListFragment.EntryData>
    val title: String

    when (parameters) {
        ShowTasksActivity.Parameters.Unscheduled -> {
            entryDatas = projectsFactory.projects
                    .values
                    .map {
                        val childTaskDatas = it.tasks
                                .filter { it.current(now) && it.isUnscheduled(now) }
                                .map { it.toChildTaskData() }

                        it.toProjectData(childTaskDatas)
                    }
                    .filter { it.children.isNotEmpty() }

            title = MyApplication.context.getString(R.string.noReminder)
        }
        is ShowTasksActivity.Parameters.Copy -> {
            entryDatas = parameters.taskKeys
                    .map { getTaskForce(it).toChildTaskData() }
                    .sorted()

            title = MyApplication.context.getString(R.string.copyingTasksTitle)
        }
        is ShowTasksActivity.Parameters.Project -> {
            val project = projectsFactory.getProjectForce(parameters.projectKey)

            entryDatas = project.tasks.map { it.toChildTaskData() }

            title = project.getDisplayName()
        }
    }

    ShowTasksViewModel.Data(
            TaskListFragment.TaskData(entryDatas, null, !parameters.copying, null),
            title,
    )
}