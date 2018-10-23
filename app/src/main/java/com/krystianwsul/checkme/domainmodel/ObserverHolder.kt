package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.viewmodels.DomainViewModel
import java.lang.ref.WeakReference

object ObserverHolder {

    private val observers = mutableListOf<WeakReference<DomainViewModel<*>.Observer>>()

    @Synchronized
    fun addDomainObserver(observer: DomainViewModel<*>.Observer) = observers.add(WeakReference(observer))

    @Synchronized
    fun clear() = observers.clear()

    @Synchronized
    fun notifyDomainObservers(dataIds: List<Int>) {
        val remove = mutableListOf<WeakReference<DomainViewModel<*>.Observer>>()

        for (reference in observers) {
            val observer = reference.get()
            if (observer == null)
                remove.add(reference)
            else
                observer.onDomainChanged(dataIds)
        }

        remove.forEach { observers.remove(it) }
    }
}
