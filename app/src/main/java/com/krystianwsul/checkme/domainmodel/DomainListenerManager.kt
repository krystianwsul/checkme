package com.krystianwsul.checkme.domainmodel

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.DomainListener
import com.krystianwsul.common.utils.singleOrEmpty
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

        domainListeners.entries
            .sortedByDescending { it.dataId() in notificationType.dataIds }
            .forEach { it.value.accept(Unit) }
    }

    sealed class NotificationType {

        companion object {

            fun merge(notificationTypes: List<NotificationType>): NotificationType? {
                if (notificationTypes.size < 2) return notificationTypes.singleOrEmpty()

                return First(notificationTypes.map { it.dataIds }.flatten().toSet())
            }
        }

        abstract val dataIds: Set<DataId>

        object All : NotificationType() {

            override val dataIds = emptySet<DataId>()
        }

        data class First(override val dataIds: Set<DataId>) : NotificationType() {

            constructor(dataId: DataId) : this(setOf(dataId))
        }
    }
}