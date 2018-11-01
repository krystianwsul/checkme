package com.krystianwsul.checkme.viewmodels

class ShowCustomTimesViewModel : DomainViewModel<ShowCustomTimesViewModel.Data>() {

    fun start() = internalStart(FirebaseLevel.NOTHING)

    override fun getData() = domainFactory.showCustomTimesData

    data class Data(val entries: List<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: Int, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}