package com.krystianwsul.checkme.firebase.dependencies

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.combineLatest

class RequestMerger<OUTPUT : Any>(store1: RequestKeyStore<*, OUTPUT>, store2: RequestKeyStore<*, OUTPUT>) {

    val outputObservable: Observable<Set<OUTPUT>> =
        listOf(store1, store2).map { it.requestedOutputKeysObservable }.combineLatest { it.flatten().toSet() }
}