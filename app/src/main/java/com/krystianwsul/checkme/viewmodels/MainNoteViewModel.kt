package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getMainNoteData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class MainNoteViewModel : ObservableDomainViewModel<MainNoteViewModel.Data, MainNoteViewModel.Parameters>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getMainNoteData(parameters.showProjects, parameters.searchCriteria, parameters.showDeleted)
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
                SearchCriteria(search, showAssignedToOthers = showAssignedToOthers),
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