package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.extensions.getShowInstanceData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class ShowInstanceViewModel : ObservableDomainViewModel<ShowInstanceViewModel.Data, ShowInstanceViewModel.Parameters>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getShowInstanceData(parameters.instanceKey, parameters.searchCriteria)
        }
    }

    val searchRelay = PublishRelay.create<SearchCriteria.Search.Query>()

    private val instanceKeyRelay = PublishRelay.create<InstanceKey>()

    init {
        Observable.combineLatest(searchRelay, instanceKeyRelay) { search, instanceKey ->
            Parameters(instanceKey, SearchCriteria(search))
        }
            .subscribe(parametersRelay)
            .addTo(clearedDisposable)
    }

    fun start(instanceKey: InstanceKey) {
        instanceKeyRelay.accept(instanceKey)

        internalStart()
    }

    data class Data(
        val name: String,
        val instanceDateTime: DateTime,
        val done: Boolean,
        val taskCurrent: Boolean,
        val canMigrateDescription: Boolean,
        val isRootInstance: Boolean,
        val groupListDataWrapper: GroupListDataWrapper,
        val displayText: String,
        val taskKey: TaskKey,
        val isVisible: Boolean,
        val newInstanceKey: InstanceKey,
        val taskHasOtherVisibleInstances: Boolean,
    ) : DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }

    data class Parameters(
        val instanceKey: InstanceKey,
        val searchCriteria: SearchCriteria,
    ) : ObservableDomainViewModel.Parameters
}