package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.time.ExactTimeStamp

class DayViewModel : ViewModel() {

    private val entries = mutableMapOf<Pair<Preferences.TimeRange, Int>, Entry>()

    fun getEntry(timeRange: Preferences.TimeRange, position: Int): Entry {
        val key = Pair(timeRange, position)

        if (!entries.containsKey(key))
            entries[key] = Entry(timeRange, position)
        return entries[key]!!
    }

    override fun onCleared() = entries.values.forEach { it.stop() }

    fun refresh() = entries.values.forEach { it.refresh() }

    class Entry(private val timeRange: Preferences.TimeRange, private val position: Int) {

        private val domainListener = object : DomainListener<DayData>() {

            override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
                it.getGroupListData(ExactTimeStamp.Local.now, position, timeRange)
            }
        }

        val data get() = domainListener.data
        val dataId get() = domainListener.dataId

        fun start() = domainListener.start()

        fun refresh() = domainListener.start(true)

        fun stop() = domainListener.stop()
    }

    data class DayData(val groupListDataWrapper: GroupListDataWrapper) : DomainData()
}