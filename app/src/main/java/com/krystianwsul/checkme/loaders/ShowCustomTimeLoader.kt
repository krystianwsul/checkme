package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute

import java.util.*

class ShowCustomTimeLoader(context: Context, private val customTimeId: Int) : DomainLoader<ShowCustomTimeLoader.DomainData>(context, FirebaseLevel.NOTHING) {

    override val name = "ShowCustomTimeLoader, customTimeId: " + customTimeId

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowCustomTimeData(customTimeId)

    data class DomainData(val id: Int, val name: String, val hourMinutes: HashMap<DayOfWeek, HourMinute>) : com.krystianwsul.checkme.loaders.DomainData() {

        init {
            check(name.isNotEmpty())
            check(!hourMinutes.isEmpty())
        }
    }
}
