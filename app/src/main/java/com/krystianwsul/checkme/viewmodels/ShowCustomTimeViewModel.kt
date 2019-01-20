package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute

class ShowCustomTimeViewModel : DomainViewModel<ShowCustomTimeViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowCustomTimeData(customTimeId)
    }

    private var customTimeId = -1

    fun start(customTimeId: Int) {
        this.customTimeId = customTimeId

        internalStart()
    }

    data class Data(val id: Int, val name: String, val hourMinutes: Map<DayOfWeek, HourMinute>) : DomainData() {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.isNotEmpty())
        }
    }
}