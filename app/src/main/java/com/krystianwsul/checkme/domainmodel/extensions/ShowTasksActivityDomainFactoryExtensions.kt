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
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.filterSearchCriteria
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.ExactTimeStamp

fun DomainFactory.getShowTasksData(
    parameters: ShowTasksActivity.Parameters,
    showProjects: Boolean, // this is dynamically from FilterCriteria, not the helper in parameters
    searchCriteria: SearchCriteria,
): ShowTasksViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowTasksData")

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    fun Task.toChildTaskData(): TaskListFragment.ChildTaskData {
        return TaskListFragment.ChildTaskData(
            name,
            getScheduleText(ScheduleText),
            getTaskListChildTaskDatas(this, now, false),
            note,
            taskKey,
            getImage(deviceDbInfo),
            notDeleted,
            isVisible(now),
            canMigrateDescription(now),
            ordinal,
            getProjectInfo(parameters.showProjects),
        )
    }

    val entryDatas: List<TaskListFragment.EntryData>
    val title: String
    val isSharedProject: Boolean?
    val subtitle: String?

    when (parameters) {
        is ShowTasksActivity.Parameters.Unscheduled -> {
            fun Project<*>.getUnscheduledTaskDatas() = getAllDependenciesLoadedTasks()
                .asSequence()
                .filter { it.notDeleted && it.intervalInfo.isUnscheduled() }
                .filterSearchCriteria(searchCriteria, myUserFactory.user)
                .map { it.toChildTaskData() }
                .toList()

            entryDatas = projectsFactory.run {
                if (parameters.projectKey != null) {
                    getProjectForce(parameters.projectKey).getUnscheduledTaskDatas()
                } else {
                    projects.values.flatMap { it.toEntryDatas(it.getUnscheduledTaskDatas(), showProjects) }
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
                .map { it.toChildTaskData() }
                .sorted()

            title = MyApplication.context.getString(R.string.copyingTasksTitle)
            subtitle = null

            isSharedProject = null
        }
        is ShowTasksActivity.Parameters.Project -> {
            val project = projectsFactory.getProjectForce(parameters.projectKey)

            entryDatas = project.getAllDependenciesLoadedTasks()
                .asSequence()
                .filter { it.isTopLevelTask() }
                .map { it.toChildTaskData() }
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