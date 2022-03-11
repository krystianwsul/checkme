package com.krystianwsul.checkme.firebase.database

import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay
import com.krystianwsul.checkme.domainmodel.getDomainScheduler
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.doAfterSubscribe
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.mindorks.scheduler.Priority
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

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
                        takeIf { isNotEmpty() }?.let {
                            val maxPriority = map { it.priority }.toSet().maxOrNull()!!

                            val currEntries = filter { it.priority == maxPriority }.also {
                                removeAll(it)
                            }

                            maxPriority to currEntries
                        }
                    }?.let { (priority, entries) ->
                        Maybe.just(entries).observeOn(getDomainScheduler(priority))
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
        maxOfOrNull { it.priority }
    }?.let(trigger::accept)

    fun <T : Any> enqueueSnapshot(databaseRead: DatabaseRead<T>, snapshot: Snapshot<T>): Single<Snapshot<T>> {
        val relay = PublishRelay.create<Snapshot<T>>()

        return relay.doAfterSubscribe {
            /*
            This doAfterSubscribe, plus the use of PublishRelay, is to ensure that the entry doesn't get dequeued before its
            listeners are ready.  That, in turn, guarantees that the logic after executing an entry will get run after the
            rest of the chain subscribing to the event.
             */
            val priority = databaseRead.priority

            synchronized { add(QueueEntry(priority, snapshot, relay)) }

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
        val priority: Priority,
        val snapshot: Snapshot<T>,
        val relay: Relay<Snapshot<T>>,
    ) {

        fun accept() = relay.accept(snapshot)
    }
}