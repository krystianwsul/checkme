package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.domainmodel.FactoryProvider
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

class DatabaseRx(
        domainDisposable: CompositeDisposable,
        databaseObservable: Observable<FactoryProvider.Database.Snapshot>
) {

    val single: Single<FactoryProvider.Database.Snapshot>
    val changeObservable: Observable<FactoryProvider.Database.Snapshot>
    val latest: Single<FactoryProvider.Database.Snapshot>
    val disposable = CompositeDisposable()

    init {
        val observable = databaseObservable.publish()

        single = observable.firstOrError()
                .cache()
                .apply { domainDisposable += subscribe() }

        changeObservable = observable.skip(1)

        latest = observable.replay(1)
                .apply { disposable += connect() }
                .firstOrError()

        disposable += observable.connect()

        domainDisposable += disposable
    }
}