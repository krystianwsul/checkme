package com.krystianwsul.checkme.firebase.dependencies

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.kotlin.withLatestFrom

class RequestMerger<OUTPUT : Any>(
    triggerSource: TriggerSource,
    store1: RequestKeyStore<*, OUTPUT>,
    store2: RequestKeyStore<*, OUTPUT>,
) {

    /*
     Further optimization ideas:
     1. DatabaseResultQueue emits events with specific record types that were processed.  Individual stores expose which
     event type they need.  Same for DomainUpdater.
     2. Model this whole thing as more of a function call.  Or, go back and think hard about when the stores emit events,
     and strategically slim them down with distinctUntilChanged or something.
     */

    val outputObservable: Observable<Set<OUTPUT>> = triggerSource.trigger
        .withLatestFrom(listOf(store1, store2).map { it.requestedOutputKeysObservable }.combineLatest { it })
        .map { Wrapper(it.second) }
        .distinctUntilChanged()
        .map { it.value.flatten().flatten().toSet() }
        .startWithItem(emptySet()) // because some stuff depends on an initial empty value
        .distinctUntilChanged()

    /*
    This wrapper ensures that the first distinctUntilChanged just makes sure that there's a new set of data after the
    onDequeued triggers the rx chain; not that its contents are actually different.  Essentially, it's ensuring that each
    event set from the stores is processed only once.
     */
    private class Wrapper<OUTPUT : Any>(val value: List<Collection<Set<OUTPUT>>>)

    interface TriggerSource {

        val trigger: Observable<Unit>
    }
}