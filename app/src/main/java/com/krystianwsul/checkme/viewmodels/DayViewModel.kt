package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.loaders.FirebaseLevel
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class DayViewModel : DomainViewModel<DayViewModel.DayData>() {

    private var position = 0
    private lateinit var timeRange: MainActivity.TimeRange

    fun start(position: Int, timeRange: MainActivity.TimeRange) {
        this.position = position
        this.timeRange = timeRange

        internalStart(FirebaseLevel.WANT)
    }

    override fun getData(domainFactory: DomainFactory) = domainFactory.getGroupListData(MyApplication.instance, ExactTimeStamp.now, position, timeRange)

    data class DayData(val dataWrapper: GroupListFragment.DataWrapper) : DomainData()
}