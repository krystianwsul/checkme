package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.filterNotNull
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

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
                .observeOn(Schedulers.single())
                .filterNotNull()
                .switchMap { domainFactory ->
                    domainFactory.domainListenerManager
                            .addListener(this)
                            .map { domainFactory }
                }
                .toFlowable(BackpressureStrategy.LATEST)
                .map { getDataResult(it) }
                .map { it.data!! }
                .filter { data.value != it }
                .doFinally {
                    DomainFactory.nullableInstance
                            ?.domainListenerManager
                            ?.removeListener(this)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data)
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }

    protected open fun getDataResult(domainFactory: DomainFactory): DomainResult<D> = DomainResult.Completed(getData(domainFactory))

    open fun getData(domainFactory: DomainFactory): D = throw NotImplementedError()
}