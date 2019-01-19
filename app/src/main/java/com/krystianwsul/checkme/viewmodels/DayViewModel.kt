package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class DayViewModel : ViewModel() {

    private val entries = mutableMapOf<Pair<MainActivity.TimeRange, Int>, Entry>()

    fun getEntry(timeRange: MainActivity.TimeRange, position: Int): Entry {
        val key = Pair(timeRange, position)

        if (!entries.containsKey(key))
            entries[key] = Entry(timeRange, position)
        return entries[key]!!
    }

    override fun onCleared() = entries.values.forEach { it.stop() }

    inner class Entry(private val timeRange: MainActivity.TimeRange, private val position: Int) {

        val data = BehaviorRelay.create<DayData>()

        private var disposable: Disposable? = null

        fun start() {
            if (disposable != null)
                return

            disposable = DomainFactory.instanceRelay
                    .filter { it.value != null }
                    .map { it.value }
                    .switchMap { domainFactory -> domainFactory.domainChanged.map { Pair(domainFactory, it) } }
                    .filter { (_, dataIds) -> !(data.value?.let { dataIds.contains(it.dataId) } == true) }
                    .subscribeOn(Schedulers.single())
                    .map { (domainFactory, _) -> getData(domainFactory) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .filter { data.value != it }
                    .subscribe(data)
        }

        fun stop() {
            disposable?.dispose()
            disposable = null
        }

        fun getData(domainFactory: DomainFactory) = domainFactory.getGroupListData(ExactTimeStamp.now, position, timeRange)
    }

    data class DayData(val dataWrapper: GroupListFragment.DataWrapper) : DomainData()
}