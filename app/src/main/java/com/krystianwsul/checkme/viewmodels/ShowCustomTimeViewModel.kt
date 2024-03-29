package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getShowCustomTimeData
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey

class ShowCustomTimeViewModel : DomainViewModel<ShowCustomTimeViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getShowCustomTimeData(customTimeKey) }
    }

    private lateinit var customTimeKey: CustomTimeKey

    fun start(customTimeKey: CustomTimeKey) {
        this.customTimeKey = customTimeKey

        internalStart()
    }

    data class Data(
            val key: CustomTimeKey,
            val name: String,
            val hourMinutes: Map<DayOfWeek, HourMinute>,
    ) : DomainData() {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.isNotEmpty())
        }
    }
}