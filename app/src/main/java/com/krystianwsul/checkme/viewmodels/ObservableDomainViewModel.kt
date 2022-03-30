package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.BehaviorRelay
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables

abstract class ObservableDomainViewModel<D : DomainData, P : ObservableDomainViewModel.Parameters> : DomainViewModel<D>() {

    protected val parametersRelay = BehaviorRelay.create<P>()
    protected val parameters get() = parametersRelay.value!!

    private val refreshRelay = BehaviorRelay.createDefault(Refresh())

    private var disposable: Disposable? = null

    override fun internalStart() {
        val myStarted = disposable?.isDisposed == false
        check(myStarted == started)

        if (myStarted) return

        started = true

        disposable = Observables.combineLatest(parametersRelay, refreshRelay)
            .distinctUntilChanged()
            .subscribe { domainListener.start(true) }
    }

    override fun refresh() {
        refreshRelay.accept(Refresh())

        internalStart()
    }

    private fun dispose() {
        disposable?.dispose()
        disposable = null
    }

    override fun stop() {
        dispose()

        super.stop()
    }

    override fun onCleared() {
        dispose()

        super.onCleared()
    }

    interface Parameters

    private class Refresh
}