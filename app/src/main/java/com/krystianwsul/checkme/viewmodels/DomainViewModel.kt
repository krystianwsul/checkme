package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    protected abstract val domainListener: DomainListener<D>

    protected val clearedDisposable = CompositeDisposable()
    protected val startedDisposable = CompositeDisposable()

    protected var started = false
        private set

    val data get() = domainListener.data
    val dataId get() = domainListener.dataId

    fun stop() {
        started = false
        startedDisposable.clear()
        domainListener.stop()
    }

    override fun onCleared() {
        stop()

        clearedDisposable.dispose()
    }

    protected fun internalStart() {
        started = true
        domainListener.start()
    }

    fun refresh() {
        started = true
        domainListener.start(true)
    }
}