package com.krystianwsul.checkme.domainmodel

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.viewmodels.DomainListener
import io.reactivex.Observable

class DomainListenerManager {

    private val domainListeners = mutableMapOf<DomainListener<*>, BehaviorRelay<Unit>>()

    fun addListener(domainListener: DomainListener<*>): Observable<Unit> {
        check(!domainListeners.containsKey(domainListener))

        return BehaviorRelay.createDefault(Unit).also { domainListeners[domainListener] = it }
    }

    fun removeListener(domainListener: DomainListener<*>) {
        check(domainListeners.containsKey(domainListener))

        domainListeners.remove(domainListener)
    }

    fun notify(dataId: Int? = null) {
        val eligibleListeners = if (dataId != null && dataId != 0) {
            domainListeners.filter { it.key.data.value?.dataId != dataId }.map { it.value }
        } else {
            domainListeners.values
        }

        eligibleListeners.forEach { it.accept(Unit) }
    }
}