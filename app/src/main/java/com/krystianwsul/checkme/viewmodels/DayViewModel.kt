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
                it.getGroupListData(ExactTimeStamp.Local.now, position, timeRange, delegate.parameters.showAssigned)
            }
        }

        private val delegate: ObservableDomainViewModel.Delegate<DayData, Parameters> =
            ObservableDomainViewModel.Delegate(domainListener)

        val data get() = domainListener.data
        val dataId get() = domainListener.dataId

        fun start(showAssigned: Boolean) {
            delegate.parametersRelay.accept(Parameters(showAssigned))

            delegate.start()
        }

        fun refresh() = delegate.refresh()

        fun stop() = delegate.dispose()
    }

    data class DayData(val groupListDataWrapper: GroupListDataWrapper) : DomainData()

    private data class Parameters(val showAssigned: Boolean) : ObservableDomainViewModel.Parameters
}