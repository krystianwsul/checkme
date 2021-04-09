package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getEditInstancesData
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey
import kotlinx.parcelize.Parcelize
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
            val instanceKeys: Set<InstanceKey>,
            val parentInstanceData: ParentInstanceData?,
            val dateTime: DateTime,
            val customTimeDatas: Map<CustomTimeKey.Project<*>, CustomTimeData>,
    ) : DomainData()

    data class InstanceData(val instanceDateTime: DateTime, val done: Boolean)

    data class CustomTimeData(
            val customTimeKey: CustomTimeKey.Project<*>,
            val name: String,
            val hourMinutes: SortedMap<DayOfWeek, HourMinute>,
    ) {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.size == 7)
        }
    }

    @Parcelize
    data class ParentInstanceData(val instanceKey: InstanceKey, val name: String) : Parcelable
}