package com.krystianwsul.checkme.utils

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

fun <T : Any, U : Any> Single<T>.mapWith(other: U) = map { it to other }

fun <T : Any> Single<T>.doOnSuccessOrDispose(action: () -> Unit) = doOnSuccess { action() }.doOnDispose { action() }

fun <T : Any> Observable<T>.doAfterSubscribe(action: () -> Unit): Observable<T> {
    return mergeWith(Completable.fromAction(action))
}

fun <T : Any> Observable<T>.partition(predicate: (T) -> Boolean): Pair<Observable<T>, Observable<T>> {
    val shared = share()

    return shared.filter(predicate) to shared.filter { !predicate(it) }
}