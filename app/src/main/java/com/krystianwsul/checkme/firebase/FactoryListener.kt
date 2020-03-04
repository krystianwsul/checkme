package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.ChildEvent
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

class FactoryListener(
        deviceInfoObservable: Observable<NullableWrapper<DeviceInfo>>,
        getUserSingle: (DeviceInfo) -> Single<DataSnapshot>,
        getUserObservable: (DeviceInfo) -> Observable<DataSnapshot>,
        userFactoryCallback: (userInfo: DeviceInfo, user: DataSnapshot) -> RemoteUserFactory,
        getPrivateProjectSingle: (DeviceInfo) -> Single<DataSnapshot>,
        getSharedProjectSingle: (DeviceInfo, Set<ProjectKey.Shared>) -> Single<DataSnapshot>,
        getPrivateProjectObservable: (DeviceInfo) -> Observable<DataSnapshot>,
        getSharedProjectEvents: (DeviceInfo, Set<ProjectKey.Shared>) -> Observable<ChildEvent>,
        projectFactoryCallback: (deviceInfo: DeviceInfo, userFactory: RemoteUserFactory, privateProject: DataSnapshot, sharedProjects: DataSnapshot) -> RemoteProjectFactory,
        getFriendSingle: (DeviceInfo) -> Single<DataSnapshot>,
        getFriendObservable: (DeviceInfo) -> Observable<DataSnapshot>,
        initialCallback: (startTime: ExactTimeStamp, userInfo: DeviceInfo, userFactory: RemoteUserFactory, projectFactory: RemoteProjectFactory, friends: DataSnapshot) -> DomainFactory,
        clearCallback: () -> Unit,
        privateProjectCallback: (domainFactory: DomainFactory, privateProject: DataSnapshot) -> Unit,
        sharedProjectCallback: (domainFactory: DomainFactory, sharedProjects: ChildEvent) -> Unit,
        friendCallback: (domainFactory: DomainFactory, friends: DataSnapshot) -> Unit,
        userCallback: (domainFactory: DomainFactory, user: DataSnapshot) -> Unit,
        logger: (String) -> Unit = { }
) {

    val domainFactoryObservable: Observable<NullableWrapper<DomainFactory>>

    init {
        val domainDisposable = CompositeDisposable()

        domainFactoryObservable = deviceInfoObservable.switchMapSingle {
            logger("userInfo begin $it")

            domainDisposable.clear()

            if (it.value != null) {
                val userInfo = it.value

                val userObservable = getUserObservable(userInfo).doOnNext { logger("userObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val privateProjectObservable = getPrivateProjectObservable(userInfo).doOnNext { logger("privateProjectObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val friendObservable = getFriendObservable(userInfo).doOnNext { logger("friendObservable $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val userSingle = getUserSingle(userInfo).doOnSuccess { logger("userSingle $it") }.cache()
                val privateProjectSingle = getPrivateProjectSingle(userInfo).doOnSuccess { logger("privateProjectSingle $it") }.cache()
                val friendSingle = getFriendSingle(userInfo).doOnSuccess { logger("friendSingle $it") }.cache()

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userSingle.map { userFactoryCallback(userInfo, it) }

                val sharedProjectKeysObservable = userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable }
                        .publish()
                        .apply { domainDisposable += connect() }

                val sharedProjectSingle = sharedProjectKeysObservable.firstOrError()
                        .flatMap { getSharedProjectSingle(userInfo, it) }
                        .doOnSuccess { logger("sharedProjectSingle $it") }
                        .cache()

                val sharedProjectEvents = sharedProjectKeysObservable.switchMap { getSharedProjectEvents(userInfo, it) }.doOnNext { logger("sharedProjectEvents $it") }
                        .publish()
                        .apply { domainDisposable += connect() }

                val projectFactorySingle = Singles.zip(
                        userFactorySingle,
                        privateProjectSingle,
                        sharedProjectSingle
                ) { userFactory, privateProject, sharedProjects ->
                    projectFactoryCallback(userInfo, userFactory, privateProject, sharedProjects)
                }

                val domainFactorySingle = Singles.zip(
                        userFactorySingle,
                        projectFactorySingle,
                        friendSingle
                ) { userFactory, projectFactory, friends ->
                    initialCallback(startTime, userInfo, userFactory, projectFactory, friends)
                }.cache()

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