package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo

class FactoryListener<T, U, V, W>(
        userInfoObservable: Observable<NullableWrapper<T>>,
        getTaskSingle: (T) -> Single<U>,
        getFriendSingle: (T) -> Single<U>,
        getUserSingle: (T) -> Single<U>,
        getTaskEvents: (T) -> Observable<V>,
        getFriendObservable: (T) -> Observable<U>,
        getUserObservable: (T) -> Observable<U>,
        initialCallback: (userInfo: T, tasks: U, friends: U, user: U) -> W,
        clearCallback: () -> Unit,
        taskCallback: (domainFactory: W, tasks: V) -> Unit,
        friendCallback: (domainFactory: W, friends: U) -> Unit,
        userCallback: (domainFactory: W, user: U) -> Unit) {

    val domainFactoryObservable: Observable<NullableWrapper<W>>

    init {
        val domainDisposable = CompositeDisposable()

        domainFactoryObservable = userInfoObservable.switchMapSingle {
            domainDisposable.clear()

            if (it.value != null) {
                val userInfo = it.value

                val taskSingle = getTaskSingle(userInfo)
                val friendSingle = getFriendSingle(userInfo)
                val userSingle = getUserSingle(userInfo)

                val domainFactorySingle = Singles.zip(taskSingle, friendSingle, userSingle) { tasks, friends, user -> initialCallback(userInfo, tasks, friends, user) }

                taskSingle.flatMapObservable { getTaskEvents(userInfo) }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> taskCallback(domainFactory, it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                friendSingle.flatMapObservable { getFriendObservable(userInfo) }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> friendCallback(domainFactory, it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                userSingle.flatMapObservable { getUserObservable(userInfo) }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> userCallback(domainFactory, it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                clearCallback()

                Single.just(NullableWrapper())
            }
        }
    }
}