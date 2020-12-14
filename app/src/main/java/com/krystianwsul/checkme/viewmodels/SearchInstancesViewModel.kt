package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getSearchInstancesData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.utils.SearchData

class SearchInstancesViewModel : DomainViewModel<SearchInstancesViewModel.Data>() {

    private var parameters = Parameters()

    override val domainListener = object : DomainListener<Data>() {

        override fun getDataResult(domainFactory: DomainFactory) = domainFactory.getSearchInstancesData(
                parameters.query,
                parameters.page
        )
    }

    fun start(searchData: SearchData, page: Int) {
        val newParameters = Parameters(searchData, page)

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
            val searchData: SearchData,
    ) : DomainData()

    private data class Parameters(val query: SearchData = SearchData(), val page: Int = 0)
}