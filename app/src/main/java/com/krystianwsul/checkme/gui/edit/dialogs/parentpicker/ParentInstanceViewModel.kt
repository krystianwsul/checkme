package com.krystianwsul.checkme.gui.edit.dialogs.parentpicker

import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.extensions.getEditInstancesSearchData
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainListener
import com.krystianwsul.checkme.viewmodels.DomainViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Ordinal

class ParentInstanceViewModel : DomainViewModel<ParentInstanceViewModel.Data>() {

    private lateinit var parameters: Parameters

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryDomainResult {
            it.getEditInstancesSearchData(parameters.searchCriteria, parameters.page)
        }
    }

    fun start(searchCriteria: SearchCriteria, page: Int) {
        val newParameters = Parameters(searchCriteria, page)

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
        var ordinal: Ordinal,
        val instanceKey: InstanceKey,
        override val matchesSearch: Boolean,
        val instanceDateTimePair: DateTimePair,
    ) : Comparable<InstanceEntryData>, ParentPickerFragment.EntryData {

        override fun compareTo(other: InstanceEntryData): Int {
            val timeStampComparison = instanceTimeStamp.compareTo(other.instanceTimeStamp)
            if (timeStampComparison != 0) return timeStampComparison

            return ordinal.compareTo(other.ordinal)
        }
    }

    private data class Parameters(val searchCriteria: SearchCriteria = SearchCriteria.empty, val page: Int = 0)

    data class SortKey(
        val instanceTimeStamp: TimeStamp,
        val ordinal: Ordinal,
    ) : ParentPickerFragment.SortKey {

        override fun compareTo(other: ParentPickerFragment.SortKey) =
                compareValuesBy(this, other, { instanceTimeStamp }, { ordinal })
    }
}