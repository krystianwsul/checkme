package com.krystianwsul.checkme.firebase.dependencies

import com.krystianwsul.checkme.firebase.database.DatabaseResultQueue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.kotlin.withLatestFrom

class RequestMerger<OUTPUT : Any>(store1: RequestKeyStore<*, OUTPUT>, store2: RequestKeyStore<*, OUTPUT>) {

    /*
    todo queue
    What I would really like here is that an observable that keeps the latest value from the outputKeys observables,
    and emits it when onDequeued has an event.  But only once.  Instead, I'm using distinctUntilChanged, which has *got* to
    be more expensive, but baby steps.
     */

    // todo queue user event occasionally doesn't come through

    /*
    todo queue:

     use non-data class as wrapper to optimize first debounce.  Then, benchmark to see performance on pixel 4

     Further optimization ideas:
     1. DatabaseResultQueue emits events with specific record types that were processed.  Individual stores expose which
     event type they need.
     2. Model this whole thing as more of a function call.  Or, go back and think hard about when the stores emit events,
     and strategically slim them down with distinctUntilChanged or something.
     */

    val outputObservable: Observable<Set<OUTPUT>> = DatabaseResultQueue.onDequeued
        .withLatestFrom(listOf(store1, store2).map { it.requestedOutputKeysObservable }.combineLatest { it })
        .map { it.second }
        .distinctUntilChanged()
        .map { it.flatten().flatten().toSet() }
        .distinctUntilChanged()
}