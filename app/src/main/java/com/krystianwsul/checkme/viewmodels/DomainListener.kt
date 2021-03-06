package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

abstract class DomainListener<D : DomainData> {

    val data = BehaviorRelay.create<D>()

    private var disposable: Disposable? = null

    fun start() {
        if (disposable != null)
            return

        disposable = DomainFactory.instanceRelay
                .filter { it.value != null }
                .map { it.value }
                .switchMap { domainFactory -> domainFactory.domainChanged.map { Pair(domainFactory, it) } }
                .filter { (_, dataIds) -> !dataIds.contains(data.value?.dataId) }
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
}