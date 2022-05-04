package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.addTo

class DayViewModel : ViewModel() {

    private val entries = mutableMapOf<Int, Entry>()

    fun getEntry(position: Int): Entry {
        if (!entries.containsKey(position))
            entries[position] = Entry(position)

        return entries[position]!!
    }

    override fun onCleared() = entries.values.forEach { it.stop() }

    fun refresh() = entries.values.forEach { it.refresh() }

    class Entry(private val position: Int) {

        private val domainListener = object : DomainListener<DayData>() {

            override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
                it.getGroupListData(ExactTimeStamp.Local.now, position, delegate.parameters.showAssigned)
            }
        }

        private val delegate: ObservableDomainViewModel.Delegate<DayData, Parameters> =
            ObservableDomainViewModel.Delegate(domainListener)

        val data get() = domainListener.data
        val dataId get() = domainListener.dataId

        private val showAssignedRelay = BehaviorRelay.create<Boolean>()

        private val compositeDisposable = CompositeDisposable()

        init {
            Observables.combineLatest(showAssignedRelay, Preferences.projectFilterObservable)
                .map { (showAssigned, projectFilter) -> Parameters(showAssigned, projectFilter) }
                .subscribe(delegate.parametersRelay)
                .addTo(compositeDisposable)
        }

        fun start(showAssigned: Boolean) {
            showAssignedRelay.accept(showAssigned)

            delegate.start()
        }

        fun refresh() = delegate.refresh()

        fun stop() {
            delegate.dispose()
            compositeDisposable.clear()
        }
    }

    data class DayData(val groupListDataWrapper: GroupListDataWrapper) : DomainData()

    private data class Parameters(val showAssigned: Boolean, val projectFilter: Preferences.ProjectFilter) :
        ObservableDomainViewModel.Parameters
}