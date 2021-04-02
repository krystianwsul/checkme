package com.krystianwsul.checkme.viewmodels

import android.util.Log
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.filterNotNull
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.disposables.Disposable

abstract class DomainListener<D : DomainData> {

    companion object {

        private var dataId = 1

        private val nextId get() = dataId++
    }

    val dataId = DataId(nextId)
    val data = BehaviorRelay.create<D>()!!

    private var disposable: Disposable? = null

    fun start(forced: Boolean = false) {
        Log.e("asdf", "magic start $this") // todo paper

        if (disposable != null) {
            if (forced)
                stop()
            else
                return
        }

        var listenerAdded = false

        disposable = DomainFactory.instanceRelay
                .observeOnDomain()
                .filterNotNull()
                .switchMap { domainFactory ->
                    /**
                     * What's expected here: the DomainListener may be initialized before the DomainFactory is
                     * available.  And, it can stick around after the DomainFactory is destroyed, such as on logout.
                     * But after logout, all DomainListeners should be destroyed, so we should never see more than
                     * one event get this far.
                     */
                    check(!listenerAdded)

                    listenerAdded = true

                    domainFactory.domainListenerManager
                            .addListener(this)
                            .map { domainFactory }
                }
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOnDomain()
                .map { getDataResult(it).data!! }
                .filter { data.value != it }
                .doFinally {
                    if (listenerAdded) {
                        DomainFactory.nullableInstance
                                ?.domainListenerManager
                                ?.removeListener(this)
                    }
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