package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getShowTasksData
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.addTo

class ShowTasksViewModel : DomainViewModel<ShowTasksViewModel.Data>() {

    private lateinit var parameters: Parameters

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getShowTasksData(
                parameters.activityParameters,
                parameters.showProjects,
                parameters.searchCriteria,
                parameters.showDeleted,
            )
        }
    }

    fun start(activityParameters: ShowTasksActivity.Parameters) {
        if (started) return

        Observables.combineLatest(
            Preferences.showProjectsObservable,
            Preferences.showAssignedObservable,
            Preferences.showDeletedObservable,
        )
            .distinctUntilChanged()
            .subscribe { (showProjects, showAssignedToOthers, showDeleted) ->
                parameters = Parameters(
                    activityParameters,
                    showProjects,
                    SearchCriteria(showAssignedToOthers = showAssignedToOthers),
                    showDeleted,
                )

                refresh()
            }
            .addTo(startedDisposable)
    }

    data class Data(
        val taskData: TaskListFragment.TaskData,
        val title: String,
        val subtitle: String?,
        val isSharedProject: Boolean?,
    ) : DomainData()

    data class Parameters(
        val activityParameters: ShowTasksActivity.Parameters,
        val showProjects: Boolean,
        val searchCriteria: SearchCriteria,
        val showDeleted: Boolean,
    )
}