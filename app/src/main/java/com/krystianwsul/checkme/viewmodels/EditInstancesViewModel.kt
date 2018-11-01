package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import java.util.*

class EditInstancesViewModel : DomainViewModel<EditInstancesViewModel.Data>() {

    private lateinit var instanceKeys: List<InstanceKey>

    fun start(instanceKeys: List<InstanceKey>) {
        check(instanceKeys.size > 1)

        this.instanceKeys = instanceKeys

        val firebaseLevel = if (instanceKeys.any { it.type == TaskKey.Type.REMOTE })
            FirebaseLevel.NEED
        else
            FirebaseLevel.NOTHING

        internalStart(firebaseLevel)
    }

    override fun getData() = domainFactory.getEditInstancesData(instanceKeys)

    data class Data(
            val instanceDatas: Map<InstanceKey, InstanceData>,
            val customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
            val showHour: Boolean) : DomainData() {

        init {
            check(instanceDatas.size > 1)
        }
    }

    data class InstanceData(val instanceDateTime: DateTime, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }

    data class CustomTimeData(
            val customTimeKey: CustomTimeKey,
            val name: String,
            val hourMinutes: TreeMap<DayOfWeek, HourMinute>) {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.size == 7)
        }
    }
}