package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute

class ShowCustomTimeViewModel : DomainViewModel<ShowCustomTimeViewModel.Data>() {

    private var customTimeId = -1

    fun start(customTimeId: Int) {
        this.customTimeId = customTimeId

        internalStart(FirebaseLevel.NOTHING)
    }

    override fun getData() = domainFactory.getShowCustomTimeData(customTimeId)

    data class Data(val id: Int, val name: String, val hourMinutes: Map<DayOfWeek, HourMinute>) : DomainData() {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.isNotEmpty())
        }
    }
}