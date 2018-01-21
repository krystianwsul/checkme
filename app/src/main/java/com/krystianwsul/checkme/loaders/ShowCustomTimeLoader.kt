package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import junit.framework.Assert
import java.util.*

class ShowCustomTimeLoader(context: Context, private val customTimeId: Int) : DomainLoader<ShowCustomTimeLoader.Data>(context, DomainLoader.FirebaseLevel.NOTHING) {

    override fun getName() = "ShowCustomTimeLoader, customTimeId: " + customTimeId

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowCustomTimeData(customTimeId)

    data class Data(val id: Int, val name: String, val hourMinutes: HashMap<DayOfWeek, HourMinute>) : DomainLoader.Data() {

        init {
            Assert.assertTrue(name.isNotEmpty())
            Assert.assertTrue(!hourMinutes.isEmpty())
        }
    }
}
