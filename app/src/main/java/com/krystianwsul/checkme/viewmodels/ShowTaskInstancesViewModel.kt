package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowTaskInstancesData
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper

class ShowTaskInstancesViewModel : DomainViewModel<ShowTaskInstancesViewModel.Data>() {

    private lateinit var parameters: ShowTaskInstancesActivity.Parameters
    private var page: Int = 0

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowTaskInstancesData(parameters, page)
    }

    fun start(parameters: ShowTaskInstancesActivity.Parameters, page: Int) {
        this.parameters = parameters

        if (this.page != page) {
            this.page = page

            refresh()
        } else {
            internalStart()
        }
    }

    data class Data(
        val title: String?,
        val groupListDataWrapper: GroupListDataWrapper,
        val showLoader: Boolean,
    ) : DomainData()
}