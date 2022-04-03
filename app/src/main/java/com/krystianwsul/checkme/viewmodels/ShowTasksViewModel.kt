package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getShowTasksData
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class ShowTasksViewModel : ObservableDomainViewModel<ShowTasksViewModel.Data, ShowTasksViewModel.Parameters>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getShowTasksData(
                parameters.activityParameters,
                parameters.showProjects,
                parameters.searchCriteria,
            )
        }
    }

    private val activityParametersRelay = PublishRelay.create<ShowTasksActivity.Parameters>()

    val searchRelay = PublishRelay.create<SearchCriteria.Search.Query>()

    init {
        Observable.combineLatest(
            Preferences.showProjectsObservable,
            Preferences.showAssignedObservable,
            Preferences.showDeletedObservable,
            activityParametersRelay,
            searchRelay,
        ) { showProjects, showAssignedToOthers, showDeleted, activityParameters, search ->
            Parameters(
                activityParameters,
                showProjects,
                SearchCriteria(search, showAssignedToOthers, showDeleted = showDeleted),
            )
        }
            .subscribe(parametersRelay)
            .addTo(clearedDisposable)
    }

    fun start(activityParameters: ShowTasksActivity.Parameters) {
        activityParametersRelay.accept(activityParameters)

        internalStart()
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
    ) : ObservableDomainViewModel.Parameters
}