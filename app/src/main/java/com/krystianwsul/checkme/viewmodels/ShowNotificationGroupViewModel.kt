package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.extensions.getShowNotificationGroupData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

class ShowNotificationGroupViewModel :
    ObservableDomainViewModel<ShowNotificationGroupViewModel.Data, ShowNotificationGroupViewModel.Parameters>() {

    private var cachedInstanceKeys = emptySet<InstanceKey>()

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getShowNotificationGroupData(cachedInstanceKeys, parameters.searchCriteria)
        }
    }

    val searchRelay = PublishRelay.create<SearchCriteria.Search.Query>()

    init {
        clearedDisposable += data.subscribe {
            cachedInstanceKeys = it.groupListDataWrapper
                .allInstanceDatas
                .map { it.instanceKey }
                .toSet()
        }

        searchRelay.map { Parameters(SearchCriteria(it)) }
            .subscribe(parametersRelay)
            .addTo(clearedDisposable)
    }

    fun start() = internalStart()

    data class Data(val groupListDataWrapper: GroupListDataWrapper) : DomainData()

    data class Parameters(val searchCriteria: SearchCriteria) : ObservableDomainViewModel.Parameters
}