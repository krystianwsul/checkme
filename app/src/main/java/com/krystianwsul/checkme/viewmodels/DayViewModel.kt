package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

class DayViewModel : ViewModel() {

    private val entries = mutableMapOf<Pair<MainActivity.TimeRange, Int>, Entry>()

    private val kotlinDomainFactory = DomainFactory.getInstance()

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

        private val firebaseListener = { _: DomainFactory ->
            check(kotlinDomainFactory.getIsConnected())

            load()
        }

        fun start() {
            if (observer == null) {
                observer = Observer()
                ObserverHolder.addDomainObserver(observer!!)
            }

            load()

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null && !kotlinDomainFactory.getIsConnected()) {
                val userInfo = UserInfo(firebaseUser)

                kotlinDomainFactory.setUserInfo(SaveService.Source.GUI, userInfo)
                kotlinDomainFactory.addFirebaseListener(firebaseListener)
            }
        }

        private val compositeDisposable = CompositeDisposable()

        inner class Observer : DomainObserver {

            override fun onDomainChanged(dataIds: List<Int>) {
                if (data.value?.let { dataIds.contains(it.dataId) } == true)
                    return

                load()
            }
        }

        fun load() {
            Preferences.logLineDate("DayViewModel.load")
            Single.fromCallable {
                Preferences.logLineHour("DayViewModel.getData")
                getData()
            }
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { loaded ->
                        Preferences.logLineHour("DayViewModel.subscribe, continuing? " + (data.value != loaded))
                        if (data.value != loaded)
                            data.accept(loaded)
                    }
                    .addTo(compositeDisposable)
        }

        fun stop() {
            kotlinDomainFactory.removeFirebaseListener(firebaseListener)
            observer = null
            compositeDisposable.clear()
        }

        fun getData() = kotlinDomainFactory.getGroupListData(ExactTimeStamp.now, position, timeRange)
    }

    data class DayData(val dataWrapper: GroupListFragment.DataWrapper) : DomainData()
}