package com.krystianwsul.checkme.firebase.loaders

import android.util.Log
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.common.firebase.DomainThreadChecker
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

class DatabaseRx<T : Any>(
    domainDisposable: CompositeDisposable,
    databaseObservable: Observable<T>,
    logDescription: String? = null, // todo scheduling
) {

    val disposable = CompositeDisposable().also { domainDisposable += it }

    val observable = databaseObservable
        .doOnSubscribe {
            logDescription?.let { Log.e("asdf", "magic DatabaseRx prePublish onSubscribe $it") }
        }
        .doOnNext {
            logDescription?.let { Log.e("asdf", "magic DatabaseRx prePublish onNext $it") }
        }
        .doFinally {
            logDescription?.let { Log.e("asdf", "magic DatabaseRx prePublish onFinally $it") }
        }
        .publish()

    private val cached = BehaviorRelay.create<T>()

    init {
        observable.doOnNext { DomainThreadChecker.instance.requireDomainThread() }
            .subscribe(cached::accept)
            .addTo(disposable)
    }

    val first = observable.firstOrError()
        .cache()
        .apply { disposable += subscribe() }

    val changes = observable.skip(1)
        .publish()
        .apply { disposable += connect() }

    init {
        logDescription?.let { Log.e("asdf", "magic DatabaseRx connecting $it") }
        disposable += observable.connect()
        logDescription?.let { Log.e("asdf", "magic DatabaseRx connected $it") }
    }

    fun latest() = cached.firstOrError()
}