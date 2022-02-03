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
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.ExactTimeStamp

fun DomainFactory.getShowTasksData(parameters: ShowTasksActivity.Parameters): ShowTasksViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTasksData")

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    fun Task.toChildTaskData(hierarchyExactTimeStamp: ExactTimeStamp): TaskListFragment.ChildTaskData {
        return TaskListFragment.ChildTaskData(
            name,
            getScheduleText(ScheduleText),
            getTaskListChildTaskDatas(
                this,
                now,
                hierarchyExactTimeStamp,
                false,
            ),
            note,
            taskKey,
            getImage(deviceDbInfo),
            notDeleted,
            isVisible(now),
            canMigrateDescription(now),
            ordinal,
            getProjectInfo(now, parameters.showProjects),
            isAssignedToMe(now, myUserFactory.user),
        )
    }

    val entryDatas: List<TaskListFragment.EntryData>
    val title: String
    val isSharedProject: Boolean?
    val subtitle: String?

    when (parameters) {
        is ShowTasksActivity.Parameters.Unscheduled -> {
            fun Project<*>.getUnscheduledTaskDatas() = getAllDependenciesLoadedTasks().filter {
                it.notDeleted && it.intervalInfo.isUnscheduled()
            }
                .map { it.toChildTaskData(it.getHierarchyExactTimeStamp(now)) }

            entryDatas = projectsFactory.run {
                if (parameters.projectKey != null) {
                    getProjectForce(parameters.projectKey).getUnscheduledTaskDatas()
                } else {
                    projects.values
                        .map { it.toProjectData(it.getUnscheduledTaskDatas()) }
                        .filter { it.children.isNotEmpty() }
                }
            }

            title = MyApplication.context.getString(R.string.notes)

            subtitle = parameters.projectKey?.let {
                projectsFactory.getProjectForce(it).getDisplayName()
            }

            isSharedProject = null
        }
        is ShowTasksActivity.Parameters.Copy -> {
            entryDatas = parameters.taskKeys
                .map(::getTaskForce)
                .map { it.toChildTaskData(it.getHierarchyExactTimeStamp(now)) }
                .sorted()

            title = MyApplication.context.getString(R.string.copyingTasksTitle)
            subtitle = null

            isSharedProject = null
        }
        is ShowTasksActivity.Parameters.Project -> {
            val project = projectsFactory.getProjectForce(parameters.projectKey)

            entryDatas = project.getAllDependenciesLoadedTasks()
                .asSequence()
                .map { Pair(it, it.getHierarchyExactTimeStamp(now)) }
                .filter { (task, _) -> task.isTopLevelTask() }
                .map { (task, hierarchyExactTimeStamp) -> task.toChildTaskData(hierarchyExactTimeStamp) }
                .toList()

            title = project.getDisplayName()
            subtitle = null

            isSharedProject = project is SharedProject
        }
    }

    return ShowTasksViewModel.Data(
        TaskListFragment.TaskData(entryDatas, null, !parameters.copying, null),
        title,
        subtitle,
        isSharedProject,
    )
}