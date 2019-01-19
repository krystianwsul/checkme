package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory

class ShowCustomTimesViewModel : DomainViewModel<ShowCustomTimesViewModel.Data>() {

    fun start() = internalStart()

    override fun getData(domainFactory: DomainFactory) = domainFactory.getShowCustomTimesData()

    data class Data(val entries: MutableList<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: Int, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}