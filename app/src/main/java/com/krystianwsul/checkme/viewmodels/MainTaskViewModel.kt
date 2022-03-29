package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getMainTaskData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.kotlin.Observables

class MainTaskViewModel : DomainViewModel<MainTaskViewModel.Data>() {

    private lateinit var parameters: Parameters

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getMainTaskData(parameters.showProjects, parameters.searchCriteria, parameters.showDeleted)
        }
    }

    fun start() {
        if (started) return

        Observables.combineLatest(
            Preferences.showProjectsObservable,
            Preferences.showAssignedObservable,
            Preferences.showDeletedObservable,
        )
            .distinctUntilChanged()
            .subscribe { (showProjects, showAssignedToOthers, showDeleted) ->
                parameters =
                    Parameters(
                        showProjects,
                        SearchCriteria(showAssignedToOthers = showAssignedToOthers),
                        showDeleted,
                    )

                refresh()
            }
    }

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()

    private data class Parameters(val showProjects: Boolean, val searchCriteria: SearchCriteria, val showDeleted: Boolean)
}