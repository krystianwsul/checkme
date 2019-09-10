package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import java.util.*

class EditInstancesViewModel : DomainViewModel<EditInstancesViewModel.Data>() {

    private lateinit var instanceKeys: List<InstanceKey>

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getEditInstancesData(instanceKeys)
    }

    fun start(instanceKeys: List<InstanceKey>) {
        check(instanceKeys.isNotEmpty())

        this.instanceKeys = instanceKeys

        internalStart()
    }

    data class Data(
            val instanceDatas: Map<InstanceKey, InstanceData>,
            val customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>,
            val showHour: Boolean) : DomainData() {

        init {
            check(instanceDatas.isNotEmpty())
        }
    }

    data class InstanceData(val instanceDateTime: DateTime, val name: String, val done: Boolean) {

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