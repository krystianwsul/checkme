package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getEditInstancesSearchData
import com.krystianwsul.checkme.gui.edit.dialogs.ParentPickerFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.normalized

class InstancesEditSearchViewModel : DomainViewModel<InstancesEditSearchViewModel.Data>() {

    private var parameters = Parameters()

    override val domainListener = object : DomainListener<Data>() {

        override fun getDataResult(domainFactory: DomainFactory) = domainFactory.getEditInstancesSearchData(
                parameters.searchCriteria,
                parameters.page
        )
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

    data class InstanceEntryData(
            override val name: String,
            override val childEntryDatas: Collection<InstanceEntryData>,
            override val entryKey: Parcelable,
            override val details: String?,
            override val note: String?,
            override val sortKey: EditViewModel.SortKey,
    ) : ParentPickerFragment.EntryData {

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        override fun normalize() {
            normalizedFields
        }
    }

    private data class Parameters(val searchCriteria: SearchCriteria = SearchCriteria.empty, val page: Int = 0)
}