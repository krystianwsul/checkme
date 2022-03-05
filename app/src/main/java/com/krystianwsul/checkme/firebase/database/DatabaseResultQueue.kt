package com.krystianwsul.checkme.firebase.database

import android.util.Log
import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay
import com.krystianwsul.checkme.domainmodel.getDomainScheduler
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.internal.CustomPriorityScheduler
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

object DatabaseResultQueue {

    private val entries = mutableListOf<QueueEntry<*>>()

    private val trigger = PublishRelay.create<Priority>()

    private fun <U> synchronized(action: MutableList<QueueEntry<*>>.() -> U) = synchronized(entries) { entries.action() }

    val onDequeued = PublishRelay.create<Unit>()

    init {
        trigger.toFlowable(BackpressureStrategy.LATEST)
            .flatMapMaybe(
                {
                    synchronized {
                        Log.e("asdf", "magic queue size: " + size) // todo scheduling

                        takeIf { isNotEmpty() }?.let {
                            val maxPriority = map { it.priority }.toSet().maxOrNull()!!

                            val currEntries = filter { it.priority == maxPriority }.also {
                                removeAll(it)
                            }

                            maxPriority to currEntries
                        }
                    }?.let { (priority, entries) ->
                        Log.e("asdf", "magic queue took ${entries.size} with priority $priority") // todo scheduling

                        Maybe.just(entries).observeOn(getDomainScheduler(priority))
                    } ?: Maybe.empty()
                },
                false,
                1,
            )
            .doOnNext {
                DomainThreadChecker.instance.requireDomainThread()

                Log.e(
                    "asdf",
                    "magic queue accepting ${it.size} with priority " + CustomPriorityScheduler.currentPriority.get()
                ) // todo scheduling

                it.forEach { it.accept() }

                Log.e("asdf", "magic queue accept done") // todo scheduling
                onDequeued.accept(Unit)
            }
            .subscribe {
                Log.e("asdf", "magic queue enqueueTrigger in subscribe") // todo scheduling
                enqueueTrigger()
            }
    }

    private fun enqueueTrigger() = synchronized {
        maxOfOrNull { it.priority }
    }?.let(trigger::accept)

    fun <T : Any> enqueueSnapshot(databaseRead: DatabaseRead<T>, snapshot: Snapshot<T>): Single<Snapshot<T>> {
        val relay = PublishRelay.create<Snapshot<T>>()

        return relay.firstOrError()
            .doOnSubscribe {
                /*
                This doOnSubscribe, plus the use of PublishRelay, is to ensure that the entry doesn't get dequeued before its
                listeners are ready.  That, in turn, guarantees that the logic after executing an entry will get run after
                the rest of the chain subscribing to the event.
                 */
                val priority = databaseRead.priority

                synchronized { add(QueueEntry(priority, snapshot, relay)) }

                enqueueTrigger()
            }
    }

    /*
    todo this should have a custom single; one that knows it's been unsubscribed from, to get pre-emptively removed
    from the queue.  ATM it's not costly to do the emission, but it would make the priority calculation more accurate.
     */
    private class QueueEntry<T : Any>(
        val priority: Priority,
        val snapshot: Snapshot<T>,
        val relay: Relay<Snapshot<T>>,
    ) {

        fun accept() = relay.accept(snapshot)
    }
}