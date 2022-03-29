package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getShowGroupData
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.criteria.SearchCriteria

class ShowGroupViewModel : DomainViewModel<ShowGroupViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getShowGroupData(parameters.activityParameters, parameters.searchCriteria)
        }
    }

    private lateinit var parameters: Parameters

    fun start(activityParameters: ShowGroupActivity.Parameters) {

        Preferences.showAssignedObservable
            .distinctUntilChanged()
            .subscribe {
                parameters = Parameters(activityParameters, SearchCriteria(showAssignedToOthers = it))

                refresh()
            }
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

    private data class Parameters(val activityParameters: ShowGroupActivity.Parameters, val searchCriteria: SearchCriteria)
}