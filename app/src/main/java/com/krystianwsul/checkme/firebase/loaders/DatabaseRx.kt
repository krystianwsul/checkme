package com.krystianwsul.checkme.firebase.loaders

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

class DatabaseRx(
        domainDisposable: CompositeDisposable,
        databaseObservable: Observable<Snapshot>
) {

    val disposable = CompositeDisposable().also { domainDisposable += it }

    val observable = databaseObservable.publish()!!

    private val cached = observable.replay(1).apply { disposable += connect() }

    val first = observable.firstOrError()
            .cache()
            .apply { disposable += subscribe() }!!

    val changes = observable.skip(1)
            .publish()
            .apply { disposable += connect() }!!

    init {
        disposable += observable.connect()
    }

    fun latest() = cached.firstOrError()!!
}