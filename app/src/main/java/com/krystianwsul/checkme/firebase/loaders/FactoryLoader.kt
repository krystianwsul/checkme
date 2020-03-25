package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.domainmodel.FactoryProvider
import com.krystianwsul.checkme.firebase.ProjectFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.merge
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

                val privateProjectManagerSingle = privateProjectDatabaseRx.single
                        .map { AndroidPrivateProjectManager(deviceDbInfo.userInfo, it, ExactTimeStamp.now, factoryProvider) }
                        .cacheImmediate()

                val friendDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getFriendObservable(deviceInfo.key)
                )

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userDatabaseRx.single
                        .map { RemoteUserFactory(localFactory.uuid, it, deviceInfo, factoryProvider) }
                        .cacheImmediate()

                val sharedProjectDatabaseRx = userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable }
                        .processChanges {
                            DatabaseRx(
                                    domainDisposable,
                                    factoryProvider.database.getSharedProjectObservable(it)
                            )
                        }
                        .doOnNext {
                            it.removedEntries
                                    .values
                                    .dispose()
                        }
                        .publishImmediate()

                val sharedProjectManagerSingle = sharedProjectDatabaseRx.firstOrError()
                        .flatMap {
                            it.newMap
                                    .map { it.value.single }
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
                                    listOf(privateProjectRecord),
                                    sharedProjectRecords.values
                            ).flatten()
                                    .map { projectRecord ->
                                        projectRecord.taskRecords
                                                .values
                                                .map { projectRecord to it }
                                    }
                                    .flatten()
                                    .map { it.second.taskKey to it }
                                    .toMap()
                        }
                        .publishImmediate()

                val rootInstanceDatabaseRx = taskRecordObservable.processChanges { _, (projectRecord, taskRecord) ->
                    Triple(
                            projectRecord,
                            taskRecord,
                            DatabaseRx(
                                    domainDisposable,
                                    factoryProvider.database.getRootInstanceObservable(taskRecord.rootInstanceKey)
                            )
                    )
                }
                        .doOnNext {
                            it.removedEntries
                                    .map { it.value.third }
                                    .dispose()
                        }
                        .replay(1)
                        .apply { domainDisposable += connect() }

                val sharedProjectEvents = sharedProjectDatabaseRx.skip(1)
                        .switchMap {
                            val removeEvents = Observable.fromIterable(
                                    it.removedEntries
                                            .keys
                                            .map { ProjectFactory.SharedProjectEvent.Remove(it.key) }
                            )

                            val addEvents = it.addedEntries
                                    .map { (projectId, databaseRx) ->
                                        // todo maybe I can just use the project record directly
                                        val snapshotInfoSingle = rootInstanceDatabaseRx.firstOrError().flatMap {
                                            it.newMap
                                                    .filterKeys { it.projectKey == projectId }
                                                    .values
                                                    .map { (_, taskRecord, databaseRx) ->
                                                        databaseRx.single.map { taskRecord.taskKey to it.toSnapshotInfos() }
                                                    }
                                                    .zipSingle()
                                        }

                                        Singles.zip(
                                                databaseRx.single,
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

                            val changeEvents = it.newMap
                                    .values
                                    .map {
                                        it.changeObservable
                                                .filter { it.exists() }
                                                .map { ProjectFactory.SharedProjectEvent.Change(it) }
                                    }
                                    .merge()

                            listOf(removeEvents, addEvents, changeEvents).merge()
                        }
                        .publishImmediate()

                val rootInstanceManagerSingle = rootInstanceDatabaseRx.firstOrError()
                        .flatMap {
                            it.newMap
                                    .values
                                    .map {
                                        it.third
                                                .single
                                                .map { dataSnapshot -> Pair(it.second, dataSnapshot) }
                                    }
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
                    it.newMap
                            .map { (taskKey, pair) ->
                                pair.third
                                        .changeObservable
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
                        friendDatabaseRx.single
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

                fun <T> Observable<T>.subscribeDomain(action: FactoryProvider.Domain.(T) -> Unit) {
                    domainDisposable += switchMapSingle {
                        domainFactorySingle.map { domainFactory -> Pair(domainFactory, it) }
                    }.subscribe { it.first.action(it.second) }
                }

                privateProjectDatabaseRx.changeObservable.subscribeDomain { updatePrivateProjectRecord(it) }

                sharedProjectEvents.subscribeDomain { updateSharedProjectRecords(it) }

                friendDatabaseRx.changeObservable.subscribeDomain { updateFriendRecords(it) }

                userDatabaseRx.changeObservable.subscribeDomain { updateUserRecord(it) }

                rootInstanceEvents.subscribeDomain { updateInstanceRecords(it) }

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                factoryProvider.nullableInstance?.clearUserInfo()

                Single.just(NullableWrapper())
            }
        }
    }

    private class DatabaseRx(
            domainDisposable: CompositeDisposable,
            databaseObservable: Observable<FactoryProvider.Database.Snapshot>
    ) {

        val single: Single<FactoryProvider.Database.Snapshot>
        val changeObservable: Observable<FactoryProvider.Database.Snapshot>
        val disposable: Disposable

        init {
            val observable = databaseObservable.publish()
            disposable = observable.connect()
            domainDisposable += disposable

            single = observable.firstOrError()
                    .cache()
                    .apply { domainDisposable += subscribe() }

            changeObservable = observable.skip(1)
        }
    }

    private fun <T, U, V> Observable<T>.processChanges(
            keyGetter: (T) -> Set<U>,
            adder: (T, U) -> V
    ): Observable<MapChanges<U, V>> = scan(MapChanges<U, V>()) { oldMapChanges, newData ->
        val oldMap = oldMapChanges.newMap
        val newKeys = keyGetter(newData)

        val removedKeys = oldMap.keys - newKeys
        val addedKeys = newKeys - oldMap.keys
        val unchangedKeys = newKeys - addedKeys

        val newMap = oldMap.toMutableMap().apply {
            addedKeys.forEach { put(it, adder(newData, it)) }
        }

        fun Set<U>.entries(map: Map<U, V>) = map { it to map.getValue(it) }.toMap()

        MapChanges(
                removedKeys.entries(oldMap),
                addedKeys.entries(newMap),
                unchangedKeys.entries(newMap),
                oldMap,
                newMap
        )
    }.skip(1)

    private fun <T, U> Observable<Set<T>>.processChanges(adder: (T) -> U) = processChanges(
            { it },
            { _, key -> adder(key) }
    )

    private fun <T, U, V> Observable<Map<T, U>>.processChanges(adder: (T, U) -> V) = processChanges(
            { it.keys },
            { newData, key -> adder(key, newData.getValue(key)) }
    )

    private fun Collection<DatabaseRx>.dispose() = forEach {
        it.disposable.dispose()
    }

    @Suppress("unused")
    private class MapChanges<T, U>(
            val removedEntries: Map<T, U> = mapOf(),
            val addedEntries: Map<T, U> = mapOf(),
            val unchangedEntries: Map<T, U> = mapOf(),
            val oldMap: Map<T, U> = mapOf(),
            val newMap: Map<T, U> = mapOf()
    )

    private val typeToken = object : FactoryProvider.Database.TypeIndicator<Map<String, Map<String, InstanceJson>>>() {}

    private fun FactoryProvider.Database.Snapshot.toSnapshotInfos() = getValue(typeToken)?.map { (dateString, timeMap) ->
        timeMap.map { (timeString, instanceJson) ->
            AndroidRootInstanceManager.SnapshotInfo(dateString, timeString, instanceJson)
        }
    }
            ?.flatten()
            ?: listOf()
}