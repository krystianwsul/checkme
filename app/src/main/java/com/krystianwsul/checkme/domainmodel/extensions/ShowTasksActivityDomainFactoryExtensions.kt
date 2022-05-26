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
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.search.SearchContext
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

    fun Task.toChildTaskData(
        childSearchContext: SearchContext,
        matchesSearch: Boolean,
        includeProjectDetails: Boolean,
    ): TaskListFragment.ChildTaskData {
        return TaskListFragment.ChildTaskData(
            name,
            getScheduleText(ScheduleText),
            getTaskListChildTaskDatas(this, now, childSearchContext, false),
            note,
            taskKey,
            getImage(deviceDbInfo),
            notDeleted,
            isVisible(now),
            canMigrateDescription(now),
            ordinal,
            getProjectInfo(includeProjectDetails),
            matchesSearch,
        )
    }

    val searchContext = SearchContext.startSearch(searchCriteria, now, myUserFactory.user)

    val entryDatas: List<TaskListFragment.EntryData>
    val title: String
    val isSharedProject: Boolean?
    val subtitle: String?

    when (parameters) {
        is ShowTasksActivity.Parameters.Unscheduled -> {
            fun OwnedProject<*>.getUnscheduledTaskDatas(searchContext: SearchContext) = searchContext.search {
                getAllDependenciesLoadedTasks()
                    .asSequence()
                    .filter { it.notDeleted && it.intervalInfo.isUnscheduled() }
                    .filterSearchCriteria()
                    .map { (task, filterResult) ->
                        task.toChildTaskData(
                            getChildrenSearchContext(filterResult),
                            filterResult.matchesSearch,
                            !showProjects,
                        )
                    }
                    .toList()
            }

            entryDatas = projectsFactory.run {
                if (parameters.projectKey != null) {
                    getProjectForce(parameters.projectKey).getUnscheduledTaskDatas(searchContext)
                } else {
                    projects.values.flatMap {
                        it.toEntryDatas(it.getUnscheduledTaskDatas(searchContext), showProjects)
                    }
                }
            }

            title = MyApplication.context.getString(R.string.notes)

            // can't use method reference because of runtime casting error
            subtitle = parameters.projectKey
                ?.let { projectsFactory.getProjectForce(it) }
                ?.getDisplayName()

            isSharedProject = null
        }
        is ShowTasksActivity.Parameters.Copy -> {
            entryDatas = parameters.taskKeys
                .map(::getTaskForce)
                .map { it.toChildTaskData(searchContext, true, true) }
                .sorted()

            title = MyApplication.context.getString(R.string.copyingTasksTitle)
            subtitle = null

            isSharedProject = null
        }
        is ShowTasksActivity.Parameters.Project -> {
            val project = projectsFactory.getProjectForce(parameters.projectKey)

            entryDatas = searchContext.search {
                project.getAllDependenciesLoadedTasks()
                    .asSequence()
                    .filter { it.isTopLevelTask() }
                    .filterSearchCriteria()
                    .map { (task, filterResult) ->
                        task.toChildTaskData(
                            getChildrenSearchContext(filterResult),
                            filterResult.matchesSearch,
                            false,
                        )
                    }
                    .toList()
            }

            title = project.getDisplayName()
            subtitle = null

            isSharedProject = project is SharedProject
        }
    }

    return ShowTasksViewModel.Data(
        TaskListFragment.TaskData(entryDatas, null, !parameters.copying, null, searchCriteria),
        title,
        subtitle,
        isSharedProject,
    )
}