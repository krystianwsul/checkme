package com.krystianwsul.checkme.viewmodels

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    protected abstract val domainListener: DomainListener<D>

    protected val clearedDisposable = CompositeDisposable()
    protected val startedDisposable = CompositeDisposable() // todo observable

    protected var started = false // todo observable

    val data get() = domainListener.data
    val dataId get() = domainListener.dataId

    @CallSuper
    open fun stop() {
        started = false
        startedDisposable.clear()
        domainListener.stop()
    }

    override fun onCleared() {
        stop()

        clearedDisposable.dispose()
    }

    protected open fun internalStart() { // todo observable
        started = true
        domainListener.start()
    }

    open fun refresh() { // todo observable
        started = true
        domainListener.start(true)
    }
}