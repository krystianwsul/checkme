package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.viewmodels.DomainObserver
import java.lang.ref.WeakReference

object ObserverHolder {

    private val observers = mutableListOf<WeakReference<DomainObserver>>()

    @Synchronized
    fun addDomainObserver(observer: DomainObserver) = observers.add(WeakReference(observer))

    @Synchronized
    fun removeDomainObserver(observer: DomainObserver) {
        val remove = observers.filter { it.get().let { it == null || it == observer } }

        remove.forEach { observers.remove(it) }
    }

    @Synchronized
    fun notifyDomainObservers(dataIds: List<Int>) {
        val remove = observers.filter { it.get() == null }

        remove.forEach { observers.remove(it) }

        for (reference in observers)
            reference.get()!!.onDomainChanged(dataIds)
    }
}
