package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.extensions.getEditInstancesData
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import kotlinx.parcelize.Parcelize
import java.util.*

class EditInstancesViewModel : DomainViewModel<EditInstancesViewModel.Data>() {

    private lateinit var instanceKeys: Set<InstanceKey>

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getEditInstancesData(instanceKeys) }
    }

    fun start(instanceKeys: Set<InstanceKey>) {
        check(instanceKeys.isNotEmpty())

        this.instanceKeys = instanceKeys

        internalStart()
    }

    data class Data(
            val parentInstanceData: ParentInstanceData?,
            val dateTime: DateTime,
            val customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
            val singleProjectKey: ProjectKey<*>?,
    ) : DomainData()

    data class CustomTimeData(
            val customTimeKey: CustomTimeKey,
            val name: String,
            val hourMinutes: SortedMap<DayOfWeek, HourMinute>,
            val isMine: Boolean,
    ) {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.size == 7)
        }
    }

    @Parcelize
    data class ParentInstanceData(val instanceKey: InstanceKey, val name: String) : Parcelable
}