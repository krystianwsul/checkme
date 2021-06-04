package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowTaskInstancesData
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.criteria.SearchCriteria

class ShowTaskInstancesViewModel : DomainViewModel<ShowTaskInstancesViewModel.Data>() {

    private lateinit var parameters: Parameters

    override val domainListener = object : DomainListener<Data>() {

        override fun getDataResult(domainFactory: DomainFactory) = domainFactory.getShowTaskInstancesData(
            parameters.parameters,
            parameters.page,
            parameters.searchCriteria,
        )
    }

    fun start(parameters: ShowTaskInstancesActivity.Parameters, page: Int, searchCriteria: SearchCriteria) {
        val newParameters = Parameters(parameters, page, searchCriteria)

        if (this::parameters.isInitialized) {
            if (this.parameters == newParameters) {
                internalStart()
            } else {
                this.parameters = newParameters

                refresh()
            }
        } else {
            this.parameters = newParameters

            internalStart()
        }
    }

    data class Data(
        val title: String?,
        val groupListDataWrapper: GroupListDataWrapper,
        val showLoader: Boolean,
        val searchCriteria: SearchCriteria,
    ) : DomainData()

    private data class Parameters(
        val parameters: ShowTaskInstancesActivity.Parameters,
        val page: Int,
        val searchCriteria: SearchCriteria,
    )
}