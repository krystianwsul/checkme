package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getMainNoteData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.kotlin.Observables

class MainNoteViewModel : DomainViewModel<MainNoteViewModel.Data>() {

    private lateinit var parameters: Parameters

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher =
            DomainResultFetcher.DomainFactoryData { it.getMainNoteData(parameters.showProjects, parameters.searchCriteria) }
    }

    fun start() {
        if (started) return

        Observables.combineLatest(
            Preferences.showProjectsObservable,
            Preferences.showAssignedObservable,
        )
            .distinctUntilChanged()
            .subscribe { (showProjects, showAssignedToOthers) ->
                parameters =
                    Parameters(showProjects, SearchCriteria(showAssignedToOthers = showAssignedToOthers)) // todo show done

                refresh()
            }
    }

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()

    private data class Parameters(val showProjects: Boolean, val searchCriteria: SearchCriteria)
}