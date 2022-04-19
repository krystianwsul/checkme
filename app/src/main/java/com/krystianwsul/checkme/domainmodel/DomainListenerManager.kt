package com.krystianwsul.checkme.domainmodel

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapper
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.DomainListener
import com.krystianwsul.common.utils.singleOrEmpty
import io.reactivex.rxjava3.core.Observable

class DomainListenerManager {

    private val entries = LinkedHashSet<Entry>()

    @Synchronized
    fun addListener(domainListener: DomainListener<*>): Observable<Unit> {
        val entry = Entry(domainListener)

        check(entry !in entries)

        entries += entry

        return entry.relay
    }

    @Synchronized
    fun removeListener(domainListener: DomainListener<*>) {
        val entry = entries.single { it.domainListener == domainListener }

        entries -= entry
    }

    @Synchronized
    fun notify(notificationType: NotificationType) {
        entries.sortedByDescending { it.domainListener.dataId in notificationType.dataIds }.forEach { it.relay.accept(Unit) }
    }

    @Synchronized
    fun getTaskPriorityMapper(): TaskPriorityMapper? {
        return entries.lastOrNull()
            ?.domainListener
            ?.taskPriorityMapper
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

    private data class Entry(val domainListener: DomainListener<*>) {

        val relay = BehaviorRelay.createDefault(Unit)
    }
}