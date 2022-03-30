package com.krystianwsul.checkme.viewmodels

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    protected abstract val domainListener: DomainListener<D>

    protected val clearedDisposable = CompositeDisposable()

    val data get() = domainListener.data
    val dataId get() = domainListener.dataId

    @CallSuper
    open fun stop() = domainListener.stop()

    @CallSuper
    override fun onCleared() {
        stop()

        clearedDisposable.dispose()
    }

    protected open fun internalStart() = domainListener.start()

    open fun refresh() = domainListener.start(true)
}