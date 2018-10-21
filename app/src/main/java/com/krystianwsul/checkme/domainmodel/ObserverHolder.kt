package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.viewmodels.DomainObserver
import java.lang.ref.WeakReference
import java.util.*

object ObserverHolder {

    private val observers = ArrayList<WeakReference<DomainObserver>>()

    @Synchronized
    fun addDomainObserver(observer: DomainObserver) = observers.add(WeakReference(observer))

    @Synchronized
    fun clear() {
        observers.clear()
    }

    @Synchronized
    fun notifyDomainObservers(dataIds: List<Int>) {
        val remove = ArrayList<WeakReference<DomainObserver>>()

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
