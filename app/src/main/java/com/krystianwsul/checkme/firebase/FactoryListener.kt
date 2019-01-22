package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

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
        userCallback: (domainFactory: W, user: U) -> Unit,
        logger: (String) -> Unit = { }) {

    val domainFactoryObservable: Observable<NullableWrapper<W>>

    init {
        val domainDisposable = CompositeDisposable()

        domainFactoryObservable = userInfoObservable.switchMapSingle {
            logger("userInfo begin $it")

            domainDisposable.clear()

            if (it.value != null) {
                val userInfo = it.value

                val taskEvents = getTaskEvents(userInfo).doOnNext { logger("taskEvents $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val friendObservable = getFriendObservable(userInfo).doOnNext { logger("friendObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val userObservable = getUserObservable(userInfo).doOnNext { logger("userObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val taskSingle = getTaskSingle(userInfo).doOnSuccess { logger("taskSingle $it") }.cache()
                val friendSingle = getFriendSingle(userInfo).doOnSuccess { logger("friendSingle $it") }.cache()
                val userSingle = getUserSingle(userInfo).doOnSuccess { logger("userSingle $it") }.cache()

                val domainFactorySingle = Singles.zip(taskSingle, friendSingle, userSingle) { tasks, friends, user -> initialCallback(userInfo, tasks, friends, user) }.cache()

                taskSingle.flatMapObservable { taskEvents }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> taskCallback(domainFactory, it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                friendSingle.flatMapObservable { friendObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> friendCallback(domainFactory, it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                userSingle.flatMapObservable { userObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> userCallback(domainFactory, it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                clearCallback()

                Single.just(NullableWrapper())
            }.apply {
                logger("userInfo end $it")
            }
        }
    }
}