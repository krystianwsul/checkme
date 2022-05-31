package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.BehaviorRelay
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables

abstract class ObservableDomainViewModel<D : DomainData, P : ObservableDomainViewModel.Parameters> : DomainViewModel<D>() {

    private val delegate by lazy { Delegate<D, P>(domainListener) }

    protected val parametersRelay get() = delegate.parametersRelay
    protected val parameters get() = delegate.parameters

    override fun internalStart() = delegate.start()

    override fun refresh() = delegate.refresh()

    override fun stop() {
        delegate.dispose()

        super.stop()
    }

    override fun onCleared() {
        delegate.dispose()

        super.onCleared()
    }

    interface Parameters

    private class Refresh

    class Delegate<D : DomainData, P : Parameters>(private val domainListener: DomainListener<D>) {

        val parametersRelay = BehaviorRelay.create<P>()
        val parameters get() = parametersRelay.value!!

        private val refreshRelay = BehaviorRelay.createDefault(Refresh())

        private var disposable: Disposable? = null

        private val started get() = disposable?.isDisposed == false

        fun start() {
            if (started) return

            disposable = Observables.combineLatest(parametersRelay, refreshRelay)
                .distinctUntilChanged()
                .subscribe { domainListener.start(true) }
        }

        fun refresh() {
            refreshRelay.accept(Refresh())

            start()
        }

        fun dispose() {
            domainListener.stop()

            disposable?.dispose()
            disposable = null
        }
    }
}