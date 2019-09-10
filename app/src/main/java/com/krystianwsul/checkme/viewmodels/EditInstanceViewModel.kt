package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import java.util.*

class EditInstanceViewModel : DomainViewModel<EditInstanceViewModel.Data>() {

    private lateinit var instanceKey: InstanceKey

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getEditInstanceData(instanceKey)
    }

    fun start(instanceKey: InstanceKey) {
        this.instanceKey = instanceKey

        internalStart()
    }

    data class Data(
            val instanceKey: InstanceKey,
            val instanceDate: Date,
            val instanceTimePair: TimePair,
            val name: String,
            val customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>,
            val done: Boolean,
            val showHour: Boolean) : DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }

    data class CustomTimeData(
            val customTimeKey: CustomTimeKey<*>,
            val name: String,
            val hourMinutes: SortedMap<DayOfWeek, HourMinute>) {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.size == 7)
        }
    }
}