package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory

import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.RemoteCustomTimeId

class ShowCustomTimeViewModel : DomainViewModel<ShowCustomTimeViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowCustomTimeData(customTimeId)
    }

    private lateinit var customTimeId: RemoteCustomTimeId.Private

    fun start(customTimeId: RemoteCustomTimeId.Private) {
        this.customTimeId = customTimeId

        internalStart()
    }

    data class Data(
            val id: RemoteCustomTimeId.Private,
            val name: String,
            val hourMinutes: Map<DayOfWeek, HourMinute>) : DomainData() {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.isNotEmpty())
        }
    }
}