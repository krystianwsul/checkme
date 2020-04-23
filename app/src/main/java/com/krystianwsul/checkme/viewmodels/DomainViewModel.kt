package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    protected abstract val domainListener: DomainListener<D>

    val data by lazy { domainListener.data }

    fun stop() = domainListener.stop()

    override fun onCleared() = stop()

    protected fun internalStart() = domainListener.start()

    fun refresh() = domainListener.start(true)
}