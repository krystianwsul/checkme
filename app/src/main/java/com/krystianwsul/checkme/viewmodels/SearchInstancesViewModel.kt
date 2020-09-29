package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getSearchInstancesData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper

class SearchInstancesViewModel : DomainViewModel<SearchInstancesViewModel.Data>() {

    private var page: Int = 0

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getSearchInstancesData(page)
    }

    fun start(page: Int) {
        if (this.page != page) {
            this.page = page

            refresh()
        } else {
            internalStart()
        }
    }

    data class Data(
            val groupListDataWrapper: GroupListDataWrapper,
            val showLoader: Boolean
    ) : DomainData()
}