package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.ProjectFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*

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

                val privateProjectManagerSingle = privateProjectDatabaseRx.first
                        .map { AndroidPrivateProjectManager(deviceDbInfo.userInfo, it, ExactTimeStamp.now, factoryProvider) }
                        .cacheImmediate()

                val friendDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getFriendObservable(deviceInfo.key)
                )

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userDatabaseRx.first
                        .map { RemoteUserFactory(localFactory.uuid, it, deviceInfo, factoryProvider) }
                        .cacheImmediate()

                val sharedProjectDatabaseRx = userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable }
                        .processChangesSet(
                                {
                                    DatabaseRx(
                                            domainDisposable,
                                            factoryProvider.database.getSharedProjectObservable(it)
                                    )
                                },
                                { it.disposable.dispose() }
                        )
                        .publishImmediate()

                val sharedProjectManagerSingle = sharedProjectDatabaseRx.firstOrError()
                        .flatMap {
                            it.second
                                    .newMap
                                    .map { it.value.first }
                                    .zipSingle()
                        }
                        .map { AndroidSharedProjectManager(it, factoryProvider) }
                        .cacheImmediate()

                val taskRecordObservable = Observables.combineLatest(
                        privateProjectManagerSingle.flatMapObservable { it.privateProjectObservable },
                        sharedProjectManagerSingle.flatMapObservable { it.sharedProjectObservable }
                )
                        .map { (privateProjectRecord, sharedProjectRecords) ->
                            listOf(
                                    listOf(privateProjectRecord.taskRecords),
                                    sharedProjectRecords.map { it.value.taskRecords }
                            ).flatten()
                                    .map { it.values }
                                    .flatten()
                                    .map { it.taskKey to it }
                                    .toMap()
                        }
                        .publishImmediate()

                val rootInstanceDatabaseRx = taskRecordObservable.processChangesMap(
                        { _, taskRecord ->
                            Pair(
                                    taskRecord,
                                    DatabaseRx(
                                            domainDisposable,
                                            factoryProvider.database.getRootInstanceObservable(taskRecord.rootInstanceKey)
                                    )
                            )
                        },
                        {
                            it.second
                                    .disposable
                                    .dispose()
                        }
                )
                        .replay(1)
                        .apply { domainDisposable += connect() }

                val sharedProjectEvents = sharedProjectDatabaseRx.skip(1)
                        .switchMap {
                            val removeEvents = Observable.fromIterable(
                                    it.second
                                            .removedEntries
                                            .keys
                                            .map { ProjectFactory.SharedProjectEvent.Remove(it.key) }
                            )

                            val addEvents = it.second
                                    .addedEntries
                                    .map { (projectId, databaseRx) ->
                                        val snapshotInfoSingle = rootInstanceDatabaseRx.firstOrError().flatMap {
                                            it.second
                                                    .newMap
                                                    .filterKeys { it.projectKey == projectId }
                                                    .values
                                                    .map { (taskRecord, databaseRx) ->
                                                        databaseRx.first.map { taskRecord.taskKey to it.toSnapshotInfos() }
                                                    }
                                                    .zipSingle()
                                        }

                                        Singles.zip(
                                                databaseRx.first,
                                                snapshotInfoSingle
                                        ).flatMapMaybe { (dataSnapshot, snapshotInfos) ->
                                            Maybe.fromCallable<ProjectFactory.SharedProjectEvent.Add> {
                                                dataSnapshot.takeIf { it.exists() }?.let {
                                                    ProjectFactory.SharedProjectEvent.Add(dataSnapshot, snapshotInfos.toMap())
                                                }
                                            }
                                        }.toObservable()
                                    }
                                    .merge()

                            val changeEvents = it.second
                                    .newMap
                                    .values
                                    .map {
                                        it.changes
                                                .filter { it.exists() }
                                                .map { ProjectFactory.SharedProjectEvent.Change(it) }
                                    }
                                    .merge()

                            listOf(removeEvents, addEvents, changeEvents).merge()
                        }
                        .publishImmediate()

                val rootInstanceManagerSingle = rootInstanceDatabaseRx.firstOrError()
                        .flatMap {
                            it.second
                                    .newMap
                                    .values
                                    .map { it.second.first.map { dataSnapshot -> Pair(it.first, dataSnapshot) } }
                                    .zipSingle()
                        }
                        .map {
                            it.map { (taskRecord, dataSnapshot) ->
                                taskRecord.taskKey to AndroidRootInstanceManager(
                                        taskRecord,
                                        dataSnapshot.toSnapshotInfos(),
                                        factoryProvider
                                )
                            }.toMap()
                        }
                        .cacheImmediate()

                val rootInstanceEvents = rootInstanceDatabaseRx.switchMap {
                    it.second
                            .newMap
                            .map { (taskKey, pair) ->
                                pair.second
                                        .changes
                                        .map { ProjectFactory.InstanceEvent(taskKey, it.toSnapshotInfos()) }
                            }
                            .merge()
                }

                val projectFactorySingle = Singles.zip(
                        privateProjectManagerSingle,
                        sharedProjectManagerSingle,
                        rootInstanceManagerSingle
                ) { privateProjectManager, sharedProjectManager, rootInstanceManagers ->
                    ProjectFactory(
                            deviceDbInfo,
                            localFactory,
                            privateProjectManager,
                            sharedProjectManager,
                            rootInstanceManagers.toMutableMap(),
                            ExactTimeStamp.now,
                            factoryProvider
                    )
                }

                val domainFactorySingle = Singles.zip(
                        userFactorySingle,
                        projectFactorySingle,
                        friendDatabaseRx.first
                ) { remoteUserFactory, remoteProjectFactory, friends ->
                    factoryProvider.newDomain(
                            localFactory,
                            remoteUserFactory,
                            remoteProjectFactory,
                            deviceDbInfo,
                            startTime,
                            ExactTimeStamp.now,
                            friends
                    )
                }.cache()

                privateProjectDatabaseRx.changes
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updatePrivateProjectRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                domainDisposable += sharedProjectEvents.subscribe {
                    domainFactorySingle.subscribe { domainFactory -> domainFactory.updateSharedProjectRecords(it) }.addTo(domainDisposable)
                }

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

                rootInstanceEvents.switchMapSingle { domainFactorySingle.map { domainFactory -> Pair(domainFactory, it) } }
                        .subscribe { (domainFactory, instanceEvent) ->
                            domainFactory.updateInstanceRecords(instanceEvent)
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