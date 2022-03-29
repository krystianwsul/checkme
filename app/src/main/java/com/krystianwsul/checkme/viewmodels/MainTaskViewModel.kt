package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getMainTaskData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class MainTaskViewModel : DomainViewModel<MainTaskViewModel.Data>() {

    private lateinit var parameters: Parameters

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getMainTaskData(parameters.showProjects, parameters.searchCriteria, parameters.showDeleted)
        }
    }

    fun start(searchObservable: Observable<SearchCriteria.Search.Query>) {
        if (started) return

        Observable.combineLatest(
            Preferences.showProjectsObservable,
            Preferences.showAssignedObservable,
            Preferences.showDeletedObservable,
            searchObservable,
        ) { showProjects, showAssignedToOthers, showDeleted, search ->
            Parameters(
                showProjects,
                SearchCriteria(search, showAssignedToOthers = showAssignedToOthers),
                showDeleted,
            )
        }
            .distinctUntilChanged()
            .subscribe {
                parameters = it

                refresh()
            }
            .addTo(startedDisposable)
    }

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()

    private data class Parameters(val showProjects: Boolean, val searchCriteria: SearchCriteria, val showDeleted: Boolean)
}