package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getSearchInstancesData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.criteria.SearchCriteria

class SearchInstancesViewModel : DomainViewModel<SearchInstancesViewModel.Data>() {

    private var parameters = Parameters()

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryDomainResult {
            it.getSearchInstancesData(parameters.searchCriteria, parameters.page)
        }
    }

    fun start(searchCriteria: SearchCriteria, page: Int) {
        val newParameters = Parameters(searchCriteria, page)

        if (parameters != newParameters) {
            parameters = newParameters

            refresh()
        } else {
            internalStart()
        }
    }

    data class Data(
            val groupListDataWrapper: GroupListDataWrapper,
            val showLoader: Boolean,
            val searchCriteria: SearchCriteria,
    ) : DomainData()

    private data class Parameters(val searchCriteria: SearchCriteria = SearchCriteria.empty, val page: Int = 0)
}