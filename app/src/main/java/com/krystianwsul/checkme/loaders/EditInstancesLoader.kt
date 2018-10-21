package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import java.util.*

class EditInstancesLoader(context: Context, private val instanceKeys: List<InstanceKey>) : DomainLoader<EditInstancesLoader.DomainData>(context, needsFirebase(instanceKeys)) {

    companion object {

        private fun needsFirebase(instanceKeys: List<InstanceKey>): FirebaseLevel {
            return if (instanceKeys.any { it.type == TaskKey.Type.REMOTE })
                FirebaseLevel.NEED
            else
                FirebaseLevel.NOTHING
        }
    }

    init {
        check(instanceKeys.size > 1)
    }

    override val name = "EditInstanceLoader, instanceKeys: " + instanceKeys

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getEditInstancesData(instanceKeys)

    data class DomainData(val instanceDatas: HashMap<InstanceKey, InstanceDomainData>, val customTimeDatas: Map<CustomTimeKey, CustomTimeData>, val showHour: Boolean) : com.krystianwsul.checkme.loaders.DomainData() {

        init {
            check(instanceDatas.size > 1)
        }
    }

    data class InstanceDomainData(val instanceDateTime: DateTime, val name: String) : com.krystianwsul.checkme.loaders.DomainData() {

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
