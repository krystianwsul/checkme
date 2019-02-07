package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.RemoteCustomTimeId

class ShowCustomTimesViewModel : DomainViewModel<ShowCustomTimesViewModel.Data>() {

    override val domainListener = object : DomainListener<ShowCustomTimesViewModel.Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowCustomTimesData()
    }

    fun start() = internalStart()

    data class Data(val entries: MutableList<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: RemoteCustomTimeId.Private, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}