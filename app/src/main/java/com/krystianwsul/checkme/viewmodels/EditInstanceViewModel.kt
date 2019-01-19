package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import java.util.*

class EditInstanceViewModel : DomainViewModel<EditInstanceViewModel.Data>() {

    private lateinit var instanceKey: InstanceKey

    fun start(instanceKey: InstanceKey) {
        this.instanceKey = instanceKey

        internalStart(if (instanceKey.type == TaskKey.Type.REMOTE) FirebaseLevel.NEED else FirebaseLevel.NOTHING)
    }

    override fun getData() = domainFactory.getEditInstanceData(instanceKey)

    data class Data(
            val instanceKey: InstanceKey,
            val instanceDate: Date,
            val instanceTimePair: TimePair,
            val name: String,
            val customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
            val done: Boolean,
            val showHour: Boolean) : DomainData() {

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