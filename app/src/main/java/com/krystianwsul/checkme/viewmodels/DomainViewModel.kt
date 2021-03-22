package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    protected abstract val domainListener: DomainListener<D>

    protected val clearedDisposable = CompositeDisposable()

    val data get() = domainListener.data
    val dataId get() = domainListener.dataId

    fun stop() = domainListener.stop()

    override fun onCleared() {
        stop()

        clearedDisposable.dispose()
    }

    protected fun internalStart() = domainListener.start()

    fun refresh() = domainListener.start(true)
}