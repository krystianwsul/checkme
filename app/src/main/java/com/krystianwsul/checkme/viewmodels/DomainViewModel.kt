package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    protected abstract val domainListener: DomainListener<D>

    val data get() = domainListener.data
    val dataId get() = domainListener.dataId

    fun stop() = domainListener.stop()

    override fun onCleared() = stop()

    protected fun internalStart() = domainListener.start()

    fun refresh() = domainListener.start(true)
}