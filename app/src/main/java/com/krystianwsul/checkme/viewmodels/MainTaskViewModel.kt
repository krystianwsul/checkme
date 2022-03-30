package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getMainTaskData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class MainTaskViewModel : ObservableDomainViewModel<MainTaskViewModel.Data, MainTaskViewModel.Parameters>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getMainTaskData(parameters.showProjects, parameters.searchCriteria, parameters.showDeleted)
        }
    }

    val searchRelay = PublishRelay.create<SearchCriteria.Search.Query>()

    init {
        Observable.combineLatest(
            Preferences.showProjectsObservable,
            Preferences.showAssignedObservable,
            Preferences.showDeletedObservable,
            searchRelay,
        ) { showProjects, showAssignedToOthers, showDeleted, search ->
            Parameters(
                showProjects,
                SearchCriteria(search, showAssignedToOthers),
                showDeleted,
            )
        }
            .subscribe(parametersRelay)
            .addTo(clearedDisposable)
    }

    fun start() = internalStart()

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()

    data class Parameters(
        val showProjects: Boolean,
        val searchCriteria: SearchCriteria,
        val showDeleted: Boolean,
    ) : ObservableDomainViewModel.Parameters
}