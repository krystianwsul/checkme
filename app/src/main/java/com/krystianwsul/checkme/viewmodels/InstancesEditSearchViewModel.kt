package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getEditInstancesSearchData
import com.krystianwsul.checkme.gui.edit.dialogs.ParentPickerFragment
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
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

    private data class Parameters(val searchCriteria: SearchCriteria = SearchCriteria.empty, val page: Int = 0)

    data class SortKey(val startExactTimeStamp: ExactTimeStamp.Local) : ParentPickerFragment.SortKey {

        override fun compareTo(other: ParentPickerFragment.SortKey): Int = compareTo(other as SortKey)
    }
}