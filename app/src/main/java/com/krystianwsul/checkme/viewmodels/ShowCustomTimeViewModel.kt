package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey

class ShowCustomTimeViewModel : DomainViewModel<ShowCustomTimeViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowCustomTimeData(customTimeKey)
    }

    private lateinit var customTimeKey: CustomTimeKey.Private

    fun start(customTimeKey: CustomTimeKey.Private) {
        this.customTimeKey = customTimeKey

        internalStart()
    }

    data class Data(
            val key: CustomTimeKey.Private,
            val name: String,
            val hourMinutes: Map<DayOfWeek, HourMinute>
    ) : DomainData() {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.isNotEmpty())
        }
    }
}