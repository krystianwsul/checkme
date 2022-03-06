package com.krystianwsul.checkme.firebase.dependencies

import com.krystianwsul.checkme.firebase.database.DatabaseResultQueue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.kotlin.withLatestFrom

class RequestMerger<OUTPUT : Any>(store1: RequestKeyStore<*, OUTPUT>, store2: RequestKeyStore<*, OUTPUT>) {

    /*
     Further optimization ideas:
     1. DatabaseResultQueue emits events with specific record types that were processed.  Individual stores expose which
     event type they need.
     2. Model this whole thing as more of a function call.  Or, go back and think hard about when the stores emit events,
     and strategically slim them down with distinctUntilChanged or something.
     */

    val outputObservable: Observable<Set<OUTPUT>> = DatabaseResultQueue.onDequeued
        .withLatestFrom(listOf(store1, store2).map { it.requestedOutputKeysObservable }.combineLatest { it })
        .map { Wrapper(it.second) }
        .distinctUntilChanged()
        .map { it.value.flatten().flatten().toSet() }
        .distinctUntilChanged()

    /*
    This wrapper ensures that the first distinctUntilChanged just makes sure that there's a new set of data after the
    onDequeued triggers the rx chain; not that its contents are actually different.  Essentially, it's ensuring that each
    event set from the stores is processed only once.
     */
    private class Wrapper<OUTPUT : Any>(val value: List<Collection<Set<OUTPUT>>>)
}