package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import java.util.*

class EditInstanceLoader(context: Context, private val instanceKey: InstanceKey) : DomainLoader<EditInstanceLoader.DomainData>(context, if (instanceKey.type == TaskKey.Type.REMOTE) FirebaseLevel.NEED else FirebaseLevel.NOTHING) {

    override val name = "EditInstanceLoader, instanceKey: " + instanceKey

    public override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getEditInstanceData(instanceKey)

    data class DomainData(val instanceKey: InstanceKey, val instanceDate: Date, val instanceTimePair: TimePair, val name: String, val customTimeDatas: Map<CustomTimeKey, CustomTimeData>, val done: Boolean, val showHour: Boolean) : com.krystianwsul.checkme.loaders.DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }

    data class CustomTimeData(val customTimeKey: CustomTimeKey, val name: String, val hourMinutes: TreeMap<DayOfWeek, HourMinute>) {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.size == 7)
        }
    }
}
