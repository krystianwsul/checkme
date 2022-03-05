package com.krystianwsul.checkme.firebase.database

import android.util.Log
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.getDomainScheduler
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.internal.CustomPriorityScheduler
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

object DatabaseResultQueue {

    private val entries = mutableListOf<QueueEntry<*>>()

    private val trigger = PublishRelay.create<Priority>()

    private fun <U> synchronized(action: MutableList<QueueEntry<*>>.() -> U) = synchronized(entries) { entries.action() }

    init {
        trigger.toFlowable(BackpressureStrategy.LATEST)
            .flatMapMaybe(
                {
                    synchronized {
                        Log.e("asdf", "magic queue size: " + size)

                        takeIf { isNotEmpty() }?.let {
                            val maxPriority = map { it.priority }.toSet().maxOrNull()!!

                            val currEntries = filter { it.priority == maxPriority }.also {
                                removeAll(it)
                            }

                            maxPriority to currEntries
                        }
                    }?.let { (priority, entries) ->
                        Log.e("asdf", "magic queue took ${entries.size} with priority $priority")

                        Maybe.just(entries).observeOn(getDomainScheduler(priority))
                    } ?: Maybe.empty()
                },
                false,
                1,
            )
            .doOnNext {
                Log.e(
                    "asdf",
                    "magic queue accepting ${it.size} with priority " + CustomPriorityScheduler.currentPriority.get()
                )

                it.forEach { it.accept() }

                Log.e("asdf", "magic queue accept done")
            }
            .subscribe {
                Log.e("asdf", "magic queue enqueueTrigger in subscribe")
                enqueueTrigger()
            }
    }

    private fun enqueueTrigger() = synchronized {
        maxOfOrNull { it.priority }
    }?.let(trigger::accept)

    fun <T : Any> enqueueSnapshot(databaseRead: DatabaseRead<T>, snapshot: Snapshot<T>): Single<Snapshot<T>> {
        val behaviorRelay = BehaviorRelay.create<Snapshot<T>>()

        synchronized { add(QueueEntry(databaseRead.priority, snapshot, behaviorRelay)) }

        enqueueTrigger()

        return behaviorRelay.firstOrError()
    }

    /*
    todo this should have a custom single; one that knows it's been unsubscribed from, to get pre-emptively removed
    from the queue
     */
    private class QueueEntry<T : Any>(
        val priority: Priority,
        val snapshot: Snapshot<T>,
        val relay: BehaviorRelay<Snapshot<T>>,
    ) {

        fun accept() = relay.accept(snapshot)
    }
}