package com.krystianwsul.checkme.firebase.loaders

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

class DatabaseRx(
        domainDisposable: CompositeDisposable,
        databaseObservable: Observable<FactoryProvider.Database.Snapshot>
) {

    val observable: Observable<FactoryProvider.Database.Snapshot>
    val first: Single<FactoryProvider.Database.Snapshot>
    val changes: Observable<FactoryProvider.Database.Snapshot>
    val disposable = CompositeDisposable()

    init {
        observable = databaseObservable.publish()

        first = observable.firstOrError()
                .cache()
                .apply { domainDisposable += subscribe() }

        changes = observable.skip(1).apply { domainDisposable += subscribe() }

        disposable += observable.connect()

        domainDisposable += disposable
    }
}