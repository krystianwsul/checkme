package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowCustomTimesData
import com.krystianwsul.common.utils.CustomTimeKey


class ShowCustomTimesViewModel : DomainViewModel<ShowCustomTimesViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowCustomTimesData()
    }

    fun start() = internalStart()

    data class Data(val entries: List<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: CustomTimeKey.Private, val name: String, val details: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}