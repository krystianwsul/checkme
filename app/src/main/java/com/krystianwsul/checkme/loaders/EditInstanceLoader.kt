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
import junit.framework.Assert
import java.util.*

class EditInstanceLoader(context: Context, private val instanceKey: InstanceKey) : DomainLoader<EditInstanceLoader.Data>(context, if (instanceKey.type == TaskKey.Type.REMOTE) DomainLoader.FirebaseLevel.NEED else DomainLoader.FirebaseLevel.NOTHING) {

    override fun getName() = "EditInstanceLoader, instanceKey: " + instanceKey

    public override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getEditInstanceData(instanceKey)

    data class Data(val instanceKey: InstanceKey, val instanceDate: Date, val instanceTimePair: TimePair, val name: String, val customTimeDatas: Map<CustomTimeKey, CustomTimeData>, val done: Boolean, val showHour: Boolean) : DomainLoader.Data() {

        init {
            Assert.assertTrue(name.isNotEmpty())
        }
    }

    data class CustomTimeData(val customTimeKey: CustomTimeKey, val name: String, val hourMinutes: TreeMap<DayOfWeek, HourMinute>) {

        init {
            Assert.assertTrue(name.isNotEmpty())
            Assert.assertTrue(hourMinutes.size == 7)
        }
    }
}
