package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getShowCustomTimesData
import com.krystianwsul.common.utils.CustomTimeKey


class ShowCustomTimesViewModel : DomainViewModel<ShowCustomTimesViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getShowCustomTimesData() }
    }

    fun start() = internalStart()

    data class Data(val entries: List<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: CustomTimeKey, val name: String, val details: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}