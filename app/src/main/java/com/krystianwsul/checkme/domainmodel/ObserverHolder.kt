package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.loaders.DomainLoader
import java.lang.ref.WeakReference
import java.util.*

object ObserverHolder {

    private val observers = ArrayList<WeakReference<DomainLoader<*>.Observer>>()

    @Synchronized
    fun addDomainObserver(observer: DomainLoader<*>.Observer) = observers.add(WeakReference(observer))

    @Synchronized
    fun clear() {
        observers.clear()
    }

    @Synchronized
    fun notifyDomainObservers(dataIds: List<Int>) {
        val remove = ArrayList<WeakReference<DomainLoader<*>.Observer>>()

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
