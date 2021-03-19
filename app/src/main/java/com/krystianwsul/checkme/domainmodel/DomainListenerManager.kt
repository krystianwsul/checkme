package com.krystianwsul.checkme.domainmodel

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.DomainListener
import io.reactivex.rxjava3.core.Observable

class DomainListenerManager {

    private val domainListeners = mutableMapOf<DomainListener<*>, BehaviorRelay<Unit>>()

    @Synchronized
    fun addListener(domainListener: DomainListener<*>): Observable<Unit> {
        check(!domainListeners.containsKey(domainListener))

        return BehaviorRelay.createDefault(Unit).also { domainListeners[domainListener] = it }
    }

    @Synchronized
    fun removeListener(domainListener: DomainListener<*>) {
        check(domainListeners.containsKey(domainListener))

        domainListeners.remove(domainListener)
    }

    @Synchronized
    fun notify(notificationType: NotificationType) {
        fun Map.Entry<DomainListener<*>, *>.dataId() = key.dataId

        val eligibleListeners = when (notificationType) {
            NotificationType.All -> domainListeners.values
            is NotificationType.Skip ->
                domainListeners.filter { it.dataId() != notificationType.dataId }.map { it.value }
            is NotificationType.First -> domainListeners.entries
                    .sortedByDescending { it.dataId() == notificationType.dataId }
                    .map { it.value }
        }

        eligibleListeners.forEach { it.accept(Unit) }
    }

    sealed class NotificationType {

        object All : NotificationType()

        data class Skip(val dataId: DataId) : NotificationType()

        data class First(val dataId: DataId) : NotificationType()
    }
}