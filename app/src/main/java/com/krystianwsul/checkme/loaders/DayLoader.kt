package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class DayLoader(context: Context, private val position: Int, private val timeRange: MainActivity.TimeRange) : DomainLoader<DayLoader.Data>(context, DomainLoader.FirebaseLevel.WANT) {

    override val name = "DayLoader, position: $position, timeRange: $timeRange"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getGroupListData(context, ExactTimeStamp.now, position, timeRange)

    data class Data(val dataWrapper: GroupListFragment.DataWrapper) : DomainLoader.Data()
}
