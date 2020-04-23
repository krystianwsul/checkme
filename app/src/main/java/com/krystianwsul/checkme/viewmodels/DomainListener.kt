package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.filterNotNull
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

abstract class DomainListener<D : DomainData> {

    val data = BehaviorRelay.create<D>()

    private var disposable: Disposable? = null

    fun start(forced: Boolean = false) {
        if (disposable != null) {
            if (forced)
                stop()
            else
                return
        }

        disposable = DomainFactory.instanceRelay
                .filterNotNull()
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