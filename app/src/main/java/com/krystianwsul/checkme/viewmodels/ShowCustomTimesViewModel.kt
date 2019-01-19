package com.krystianwsul.checkme.viewmodels

class ShowCustomTimesViewModel : DomainViewModel<ShowCustomTimesViewModel.Data>() {

    fun start() = internalStart()

    override fun getData() = domainFactory.getShowCustomTimesData()

    data class Data(val entries: MutableList<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: Int, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}