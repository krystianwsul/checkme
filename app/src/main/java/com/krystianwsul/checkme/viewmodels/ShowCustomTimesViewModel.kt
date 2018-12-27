package com.krystianwsul.checkme.viewmodels

class ShowCustomTimesViewModel : DomainViewModel<ShowCustomTimesViewModel.Data>() {

    fun start() = internalStart(FirebaseLevel.NOTHING)

    override fun getData() = kotlinDomainFactory.getShowCustomTimesData()

    data class Data(val entries: MutableList<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: Int, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}