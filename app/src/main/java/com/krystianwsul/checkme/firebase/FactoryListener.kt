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
        getPrivateProjectSingle: (T) -> Single<U>,
        getSharedProjectSingle: (T) -> Single<U>,
        getFriendSingle: (T) -> Single<U>,
        getUserSingle: (T) -> Single<U>,
        getPrivateProjectObservable: (T) -> Observable<U>,
        getSharedProjectEvents: (T) -> Observable<V>,
        getFriendObservable: (T) -> Observable<U>,
        getUserObservable: (T) -> Observable<U>,
        initialCallback: (userInfo: T, privateProject: U, sharedProjects: U, friends: U, user: U) -> W,
        clearCallback: () -> Unit,
        privateProjectCallback: (domainFactory: W, privateProject: U) -> Unit,
        sharedProjectCallback: (domainFactory: W, sharedProjects: V) -> Unit,
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

                val privateProjectObservable = getPrivateProjectObservable(userInfo).doOnNext { logger("privateProjectObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val sharedProjectEvents = getSharedProjectEvents(userInfo).doOnNext { logger("sharedProjectEvents $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val friendObservable = getFriendObservable(userInfo).doOnNext { logger("friendObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val userObservable = getUserObservable(userInfo).doOnNext { logger("userObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val privateProjectSingle = getPrivateProjectSingle(userInfo).doOnSuccess { logger("privateProjectSingle $it") }.cache()
                val sharedProjectSingle = getSharedProjectSingle(userInfo).doOnSuccess { logger("sharedProjectSingle $it") }.cache()
                val friendSingle = getFriendSingle(userInfo).doOnSuccess { logger("friendSingle $it") }.cache()
                val userSingle = getUserSingle(userInfo).doOnSuccess { logger("userSingle $it") }.cache()

                val domainFactorySingle = Singles.zip(privateProjectSingle, sharedProjectSingle, friendSingle, userSingle) { privateProject, sharedProjects, friends, user -> initialCallback(userInfo, privateProject, sharedProjects, friends, user) }.cache()

                privateProjectSingle.flatMapObservable { privateProjectObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> privateProjectCallback(domainFactory, it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                sharedProjectSingle.flatMapObservable { sharedProjectEvents }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> sharedProjectCallback(domainFactory, it) }.addTo(domainDisposable)
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