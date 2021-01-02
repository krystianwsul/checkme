package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getEditInstancesSearchData
import com.krystianwsul.checkme.gui.edit.dialogs.ParentPickerFragment
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.normalized

class EditInstancesSearchViewModel : DomainViewModel<EditInstancesSearchViewModel.Data>() {

    private lateinit var parameters: Parameters

    override val domainListener = object : DomainListener<Data>() {

        override fun getDataResult(domainFactory: DomainFactory) = domainFactory.getEditInstancesSearchData(
                parameters.searchCriteria,
                parameters.page,
                parameters.projectKey,
        )
    }

    fun start(projectKey: ProjectKey<*>, searchCriteria: SearchCriteria, page: Int) {
        val newParameters = Parameters(projectKey, searchCriteria, page)

        if (!this::parameters.isInitialized || parameters == newParameters) {
            parameters = newParameters
            internalStart()
        } else {
            parameters = newParameters
            refresh()
        }
    }

    data class Data(
            val instanceEntryDatas: List<InstanceEntryData>,
            val showLoader: Boolean,
            val searchCriteria: SearchCriteria,
    ) : DomainData()

    data class InstanceEntryData(
            override val name: String,
            override val childEntryDatas: Collection<InstanceEntryData>,
            override val entryKey: Parcelable,
            override val details: String?,
            override val note: String?,
            override val sortKey: SortKey,
            val instanceTimeStamp: TimeStamp,
            var ordinal: Double,
            val instanceKey: InstanceKey,
    ) : Comparable<InstanceEntryData>, ParentPickerFragment.EntryData {

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        override fun normalize() {
            normalizedFields
        }

        override fun compareTo(other: InstanceEntryData): Int {
            val timeStampComparison = instanceTimeStamp.compareTo(other.instanceTimeStamp)
            if (timeStampComparison != 0) return timeStampComparison

            return ordinal.compareTo(other.ordinal)
        }
    }

    private data class Parameters(
            val projectKey: ProjectKey<*>,
            val searchCriteria: SearchCriteria = SearchCriteria.empty,
            val page: Int = 0,
    )

    data class SortKey(
            val instanceTimeStamp: TimeStamp,
            val ordinal: Double,
    ) : ParentPickerFragment.SortKey {

        override fun compareTo(other: ParentPickerFragment.SortKey) =
                compareValuesBy(this, other, { instanceTimeStamp }, { ordinal })
    }
}