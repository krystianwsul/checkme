package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListDataWrapper
import com.krystianwsul.common.time.ExactTimeStamp

class DayViewModel : ViewModel() {

    private val entries = mutableMapOf<Pair<MainActivity.TimeRange, Int>, Entry>()

    fun getEntry(timeRange: MainActivity.TimeRange, position: Int): Entry {
        val key = Pair(timeRange, position)

        if (!entries.containsKey(key))
            entries[key] = Entry(timeRange, position)
        return entries[key]!!
    }

    override fun onCleared() = entries.values.forEach { it.stop() }

    fun refresh() = entries.values.forEach { it.start(true) }

    class Entry(private val timeRange: MainActivity.TimeRange, private val position: Int) : DomainListener<DayData>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getGroupListData(
                ExactTimeStamp.now,
                position,
                timeRange
        )
    }

    data class DayData(val groupListDataWrapper: GroupListDataWrapper) : DomainData()
}