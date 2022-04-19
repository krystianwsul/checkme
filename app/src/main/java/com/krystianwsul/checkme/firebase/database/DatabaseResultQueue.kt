package com.krystianwsul.checkme.firebase.database

import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay
import com.krystianwsul.checkme.domainmodel.getDomainScheduler
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.doAfterSubscribe
import com.krystianwsul.common.firebase.DomainThreadChecker
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

object DatabaseResultQueue {

    private val entries = mutableListOf<QueueEntry<*>>()

    private val trigger = PublishRelay.create<DatabaseReadPriority>()

    private fun <U> synchronized(action: MutableList<QueueEntry<*>>.() -> U) = synchronized(entries) { entries.action() }

    val onDequeued = PublishRelay.create<Unit>()

    init {
        trigger.toFlowable(BackpressureStrategy.LATEST)
            .flatMapMaybe(
                {
                    synchronized {
                        takeIf { isNotEmpty() }?.let {
                            val entriesAndPriorities = it.map { it to it.databaseRead.priority }

                            val maxPriority = entriesAndPriorities.map { it.second }
                                .toSet()
                                .maxOrNull()!!

                            val currEntries = entriesAndPriorities.filter { it.second == maxPriority }.also {
                                removeAll(it.map { it.first })
                            }

                            maxPriority to currEntries.map { it.first }
                        }
                    }?.let { (priority, entries) ->
                        Maybe.just(entries).observeOn(getDomainScheduler(priority.schedulerPriority))
                    } ?: Maybe.empty()
                },
                false,
                1,
            )
            .doOnNext {
                DomainThreadChecker.instance.requireDomainThread()

                it.forEach { it.accept() }

                onDequeued.accept(Unit)
            }
            .subscribe { enqueueTrigger() }
    }

    private fun enqueueTrigger() = synchronized {
        maxOfOrNull { it.databaseRead.priority }
    }?.let(trigger::accept)

    fun <T : Any> enqueueSnapshot(databaseRead: DatabaseRead<T>, snapshot: Snapshot<T>): Single<Snapshot<T>> {
        val relay = PublishRelay.create<Snapshot<T>>()

        return relay.doAfterSubscribe {
            /*
            This doAfterSubscribe, plus the use of PublishRelay, is to ensure that the entry doesn't get dequeued before its
            listeners are ready.  That, in turn, guarantees that the logic after executing an entry will get run after the
            rest of the chain subscribing to the event.
             */

            synchronized { add(QueueEntry(databaseRead, snapshot, relay)) }

            Completable.fromCallable { enqueueTrigger() }
                .subscribeOn(Schedulers.trampoline())
                .subscribe()
        }.firstOrError()
    }

    /*
    todo this should have a custom single; one that knows it's been unsubscribed from, to get pre-emptively removed
    from the queue.  (Or just do this logic in the chain in enqueueSnapshot.)  ATM it's not costly to do the emission, but it
    would make the priority calculation more accurate.
     */
    private class QueueEntry<T : Any>(
        val databaseRead: DatabaseRead<T>,
        val snapshot: Snapshot<T>,
        val relay: Relay<Snapshot<T>>,
    ) {

        fun accept() = relay.accept(snapshot)
    }

}