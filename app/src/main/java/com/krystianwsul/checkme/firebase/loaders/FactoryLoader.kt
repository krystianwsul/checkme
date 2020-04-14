package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.ProjectsFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

class FactoryLoader(
        localFactory: FactoryProvider.Local,
        deviceInfoObservable: Observable<NullableWrapper<DeviceInfo>>,
        factoryProvider: FactoryProvider
) {

    val domainFactoryObservable: Observable<NullableWrapper<FactoryProvider.Domain>>

    init {
        val domainDisposable = CompositeDisposable()

        domainFactoryObservable = deviceInfoObservable.switchMapSingle {
            domainDisposable.clear()

            if (it.value != null) {
                val deviceInfo = it.value
                val deviceDbInfo = DeviceDbInfo(deviceInfo, localFactory.uuid)

                val userDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getUserObservable(deviceInfo.key)
                )

                val privateProjectKey = deviceDbInfo.key.toPrivateProjectKey()

                val privateProjectDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getPrivateProjectObservable(privateProjectKey)
                )

                fun <T> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }
                fun <T> Observable<T>.publishImmediate() = publish().apply { domainDisposable += connect() }

                val privateProjectManager = AndroidPrivateProjectManager(
                        deviceDbInfo.userInfo,
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
                        factoryProvider.database.getFriendObservable(deviceInfo.key)
                )

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userDatabaseRx.first
                        .map { RemoteUserFactory(localFactory.uuid, it, deviceInfo, factoryProvider) }
                        .cacheImmediate()

                val sharedProjectManager = AndroidSharedProjectManager(
                        listOf(),
                        factoryProvider.database
                )

                val sharedProjectsLoader = SharedProjectsLoader(
                        userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable },
                        sharedProjectManager,
                        domainDisposable,
                        factoryProvider.sharedProjectsProvider
                )

                val projectsFactorySingle = Singles.zip(
                        privateProjectLoader.initialProjectEvent,
                        sharedProjectsLoader.initialProjectsEvent
                ) { initialPrivateProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private>, initialSharedProjectsEvent: SharedProjectsLoader.InitialProjectsEvent ->
                    ProjectsFactory(
                            localFactory,
                            privateProjectLoader,
                            initialPrivateProjectEvent,
                            sharedProjectsLoader,
                            initialSharedProjectsEvent,
                            ExactTimeStamp.now,
                            factoryProvider,
                            domainDisposable,
                            { deviceDbInfo } // todo instances deviceDbInfo source
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
                            deviceDbInfo,
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

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                factoryProvider.nullableInstance?.clearUserInfo()

                Single.just(NullableWrapper())
            }
        }
    }
}