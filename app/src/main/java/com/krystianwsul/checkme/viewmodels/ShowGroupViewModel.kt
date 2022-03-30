package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getShowGroupData
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class ShowGroupViewModel : ObservableDomainViewModel<ShowGroupViewModel.Data, ShowGroupViewModel.Parameters>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getShowGroupData(parameters.activityParameters, parameters.searchCriteria)
        }
    }

    private val activityParametersRelay = PublishRelay.create<ShowGroupActivity.Parameters>()

    init {
        Observable.combineLatest(
            Preferences.showAssignedObservable,
            activityParametersRelay,
        ) { showAssigned, activityParameters ->
            Parameters(activityParameters, SearchCriteria(showAssignedToOthers = showAssigned))
        }
            .subscribe(parametersRelay)
            .addTo(clearedDisposable)
    }

    fun start(activityParameters: ShowGroupActivity.Parameters) {
        activityParametersRelay.accept(activityParameters)

        internalStart()
    }

    data class Data(
        val title: String,
        val subtitle: String?,
        val groupListDataWrapper: GroupListDataWrapper?,
    ) : DomainData() {

        init {
            check(title.isNotEmpty())
        }
    }

    data class Parameters(
        val activityParameters: ShowGroupActivity.Parameters,
        val searchCriteria: SearchCriteria,
    ) : ObservableDomainViewModel.Parameters
}