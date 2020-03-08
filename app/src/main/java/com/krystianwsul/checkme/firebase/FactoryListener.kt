package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.merge
import io.reactivex.rxkotlin.plusAssign

class FactoryListener(
        localFactory: LocalFactory,
        deviceInfoObservable: Observable<NullableWrapper<DeviceInfo>>
) {

    val domainFactoryObservable: Observable<NullableWrapper<DomainFactory>>

    init {
        val domainDisposable = CompositeDisposable()

        domainFactoryObservable = deviceInfoObservable.switchMapSingle {
            domainDisposable.clear()

            if (it.value != null) {
                val deviceInfo = it.value
                val deviceDbInfo = DeviceDbInfo(deviceInfo, localFactory.uuid)

                val userObservable = AndroidDatabaseWrapper.getUserObservable(deviceInfo.key)
                        .publish()
                        .apply { domainDisposable += connect() }

                val privateProjectObservable = AndroidDatabaseWrapper.getPrivateProjectObservable(deviceInfo.key.toPrivateProjectKey())
                        .publish()
                        .apply { domainDisposable += connect() }

                val friendObservable = AndroidDatabaseWrapper.getFriendObservable(deviceInfo.key)
                        .publish()
                        .apply { domainDisposable += connect() }

                val userSingle = AndroidDatabaseWrapper.getUserSingle(deviceInfo.key).cache()
                val privateProjectSingle = AndroidDatabaseWrapper.getPrivateProjectSingle(deviceInfo.key.toPrivateProjectKey()).cache()
                val friendSingle = AndroidDatabaseWrapper.getFriendSingle(deviceInfo.key).cache()

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userSingle.map { RemoteUserFactory(localFactory.uuid, it, deviceInfo) }

                val sharedProjectKeysObservable = userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable }
                        .scan(Pair(setOf<ProjectKey.Shared>(), setOf<ProjectKey.Shared>())) { old, new -> Pair(old.second, new) }
                        .publish()
                        .apply { domainDisposable += connect() }

                val sharedProjectSingle = sharedProjectKeysObservable.firstOrError()
                        .flatMap { (old, new) ->
                            check(old.isEmpty())

                            new.map { AndroidDatabaseWrapper.getSharedProjectSingle(it) }.zipSingle()
                        }
                        .cache()

                val sharedProjectEvents = sharedProjectKeysObservable.switchMap { (oldProjectIds, newProjectIds) ->
                            val removedIds = oldProjectIds - newProjectIds

                            val removedEvents = Observable.fromIterable(removedIds.map { RemoteProjectFactory.Event.Remove(it) })

                            val addChangeEvents = newProjectIds.map {
                                AndroidDatabaseWrapper.getSharedProjectObservable(it).map { RemoteProjectFactory.Event.AddChange(it) }
                            }.merge()

                            listOf(removedEvents, addChangeEvents).merge()
                        }
                        .publish()
                        .apply { domainDisposable += connect() }

                val projectFactorySingle = Singles.zip(
                        privateProjectSingle,
                        sharedProjectSingle
                ) { privateProject, sharedProjects ->
                    RemoteProjectFactory(
                            deviceDbInfo,
                            localFactory,
                            sharedProjects,
                            privateProject,
                            ExactTimeStamp.now
                    )
                }

                val domainFactorySingle = Singles.zip(
                        userFactorySingle,
                        projectFactorySingle,
                        friendSingle
                ) { remoteUserFactory, remoteProjectFactory, friends ->
                    DomainFactory(
                            localFactory,
                            remoteUserFactory,
                            remoteProjectFactory,
                            deviceDbInfo,
                            startTime,
                            ExactTimeStamp.now,
                            friends
                    )
                }.cache()

                privateProjectSingle.flatMapObservable { privateProjectObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updatePrivateProjectRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                sharedProjectSingle.flatMapObservable { sharedProjectEvents }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateSharedProjectRecords(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                friendSingle.flatMapObservable { friendObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.setFriendRecords(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                userSingle.flatMapObservable { userObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateUserRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                DomainFactory.nullableInstance?.clearUserInfo()

                Single.just(NullableWrapper())
            }
        }
    }
}