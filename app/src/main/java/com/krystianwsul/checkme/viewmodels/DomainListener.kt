package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.filterNotNull
import io.reactivex.BackpressureStrategy
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
                .switchMap { domainFactory ->
                    domainFactory.domainListenerManager
                            .addListener(this)
                            .map { domainFactory }
                }
                .observeOn(Schedulers.single())
                .toFlowable(BackpressureStrategy.LATEST)
                .map { getDataResult(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.data!! }
                .filter { data.value != it }
                .doFinally {
                    DomainFactory.nullableInstance
                            ?.domainListenerManager
                            ?.removeListener(this)
                }
                .subscribe(data)
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }

    protected open fun getDataResult(domainFactory: DomainFactory): DomainResult<D> = DomainResult.Completed(getData(domainFactory))

    open fun getData(domainFactory: DomainFactory): D = throw NotImplementedError()
}