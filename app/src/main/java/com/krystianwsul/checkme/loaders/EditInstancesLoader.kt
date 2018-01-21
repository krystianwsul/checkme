package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import junit.framework.Assert
import java.util.*

class EditInstancesLoader(context: Context, private val instanceKeys: List<InstanceKey>) : DomainLoader<EditInstancesLoader.Data>(context, needsFirebase(instanceKeys)) {

    companion object {

        private fun needsFirebase(instanceKeys: List<InstanceKey>): DomainLoader.FirebaseLevel {
            return if (instanceKeys.any { it.type == TaskKey.Type.REMOTE })
                DomainLoader.FirebaseLevel.NEED
            else
                DomainLoader.FirebaseLevel.NOTHING
        }
    }

    init {
        Assert.assertTrue(instanceKeys.size > 1)
    }

    override fun getName() = "EditInstanceLoader, instanceKeys: " + instanceKeys

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getEditInstancesData(instanceKeys)

    data class Data(val instanceDatas: HashMap<InstanceKey, InstanceData>, val customTimeDatas: Map<CustomTimeKey, CustomTimeData>, val showHour: Boolean) : DomainLoader.Data() {

        init {
            Assert.assertTrue(instanceDatas.size > 1)
        }
    }

    data class InstanceData(val instanceDateTime: DateTime, val name: String) : DomainLoader.Data() {

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
