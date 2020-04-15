package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.ProjectsFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.getCurrentValue
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

class FactoryLoader(
        localFactory: FactoryProvider.Local,
        userInfoObservable: Observable<NullableWrapper<UserInfo>>,
        factoryProvider: FactoryProvider,
        tokenObservable: Observable<NullableWrapper<String>>
) {

    val domainFactoryObservable: Observable<NullableWrapper<FactoryProvider.Domain>>

    init {
        val domainDisposable = CompositeDisposable()

        fun <T> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }
        fun <T> Observable<T>.publishImmediate() = publish().apply { domainDisposable += connect() }

        domainFactoryObservable = userInfoObservable.switchMapSingle {
            domainDisposable.clear()

            if (it.value != null) {
                val userInfo = it.value

                val deviceInfoObservable = tokenObservable.map { DeviceInfo(userInfo, it.value) }
                        .replay(1)
                        .publishImmediate()

                fun getDeviceInfo() = deviceInfoObservable.getCurrentValue()
                fun getDeviceDbInfo() = DeviceDbInfo(getDeviceInfo(), localFactory.uuid)

                val userDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getUserObservable(getDeviceInfo().key)
                )

                val privateProjectKey = getDeviceInfo().key.toPrivateProjectKey()

                val privateProjectDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getPrivateProjectObservable(privateProjectKey)
                )

                val privateProjectManager = AndroidPrivateProjectManager(
                        userInfo,
                        factoryProvider.database
                )

                val privateProjectLoader = ProjectLoader(
                        privateProjectDatabaseRx.observable,
                        domainDisposable,
                        factoryProvider.projectProvider,
                        privateProjectManager
                )

                val friendDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getFriendObservable(getDeviceInfo().key)
                )

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userDatabaseRx.first
                        .map { RemoteUserFactory(it, getDeviceDbInfo(), factoryProvider) }
                        .cacheImmediate()

                val sharedProjectManager = AndroidSharedProjectManager(factoryProvider.database)

                val sharedProjectsLoader = SharedProjectsLoader(
                        userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable },
                        sharedProjectManager,
                        domainDisposable,
                        factoryProvider.sharedProjectsProvider
                )

                val projectsFactorySingle = Singles.zip(
                        privateProjectLoader.initialProjectEvent,
                        sharedProjectsLoader.initialProjectsEvent
                ) { (changeType, initialPrivateProjectEvent), initialSharedProjectsEvent ->
                    check(changeType == ChangeType.LOCAL)

                    ProjectsFactory(
                            localFactory,
                            privateProjectLoader,
                            initialPrivateProjectEvent,
                            sharedProjectsLoader,
                            initialSharedProjectsEvent,
                            ExactTimeStamp.now,
                            factoryProvider,
                            domainDisposable,
                            ::getDeviceDbInfo
                    )
                }

                val domainFactorySingle = Singles.zip(
                        userFactorySingle,
                        projectsFactorySingle,
                        friendDatabaseRx.first
                ) { remoteUserFactory, projectsFactory, friends ->
                    factoryProvider.newDomain(
                            localFactory,
                            remoteUserFactory,
                            projectsFactory,
                            getDeviceDbInfo(),
                            startTime,
                            ExactTimeStamp.now,
                            friends
                    )
                }.cache()

                /* todo instances feed local/remote private/shared project events into domainFactory
                privateProjectDatabaseRx.changes
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updatePrivateProjectRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                domainDisposable += sharedProjectEvents.subscribe {
                    domainFactorySingle.subscribe { domainFactory -> domainFactory.updateSharedProjectRecords(it) }.addTo(domainDisposable)
                }

                rootInstanceEvents.switchMapSingle { domainFactorySingle.map { domainFactory -> Pair(domainFactory, it) } }
                        .subscribe { (domainFactory, instanceEvent) ->
                            domainFactory.updateInstanceRecords(instanceEvent)
                        }
                        .addTo(domainDisposable)

                 */

                friendDatabaseRx.changes
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateFriendRecords(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                userDatabaseRx.changes
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateUserRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                tokenObservable.flatMapSingle { tokenWrapper -> domainFactorySingle.map { Pair(tokenWrapper, it) } }
                        .subscribe { (tokenWrapper, domainFactory) ->
                            domainFactory.updateDeviceDbInfo(
                                    DeviceDbInfo(DeviceInfo(userInfo, tokenWrapper.value), localFactory.uuid),
                                    SaveService.Source.GUI
                            )
                        }
                        .addTo(domainDisposable)

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                factoryProvider.nullableInstance?.clearUserInfo()

                Single.just(NullableWrapper())
            }
        }
    }
}