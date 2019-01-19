package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
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
        private var observer: Observer? = null

        private val firebaseListener: (DomainFactory) -> Unit = { load(it) }

        fun start() {
            if (observer == null) {
                observer = Observer()
                ObserverHolder.addDomainObserver(observer!!)
            }

            DomainFactory.addFirebaseListener(firebaseListener)
        }

        private val compositeDisposable = CompositeDisposable()

        inner class Observer : DomainObserver {

            override fun onDomainChanged(dataIds: List<Int>) {
                if (data.value?.let { dataIds.contains(it.dataId) } == true)
                    return

                load(DomainFactory.instance)
            }
        }

        fun load(domainFactory: DomainFactory) = Single.fromCallable { getData(domainFactory) }
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { loaded ->
                        if (data.value != loaded)
                            data.accept(loaded)
                    }
                    .addTo(compositeDisposable)

        fun stop() {
            DomainFactory.removeFirebaseListener(firebaseListener)
            observer = null
            compositeDisposable.clear()
        }

        fun getData(domainFactory: DomainFactory) = domainFactory.getGroupListData(ExactTimeStamp.now, position, timeRange)
    }

    data class DayData(val dataWrapper: GroupListFragment.DataWrapper) : DomainData()
}