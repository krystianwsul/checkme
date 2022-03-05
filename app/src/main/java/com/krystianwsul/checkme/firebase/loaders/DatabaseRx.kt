package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.common.firebase.DomainThreadChecker
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

class DatabaseRx<T : Any>(domainDisposable: CompositeDisposable, databaseObservable: Observable<T>) {

    val disposable = CompositeDisposable().also { domainDisposable += it }

    val observable = databaseObservable.publish()!!

    private val cached = BehaviorRelay.create<T>()

    init {
        disposable += observable.doOnNext {
            DomainThreadChecker.instance.requireDomainThread()
        }.subscribe(cached::accept)
    }

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