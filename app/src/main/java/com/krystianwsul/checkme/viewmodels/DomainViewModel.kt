package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    val data = BehaviorRelay.create<D>()

    private var disposable: Disposable? = null

    protected fun internalStart() {
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

    protected abstract fun getData(domainFactory: DomainFactory): D

    override fun onCleared() = stop()
}