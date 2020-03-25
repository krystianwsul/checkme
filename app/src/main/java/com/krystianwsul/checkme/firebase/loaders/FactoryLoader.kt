package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.domainmodel.FactoryProvider
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
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
                        AndroidDatabaseWrapper.getUserObservable(deviceInfo.key)
                )

                val privateProjectKey = deviceDbInfo.key.toPrivateProjectKey()

                val privateProjectDatabaseRx = DatabaseRx(
                        domainDisposable,
                        AndroidDatabaseWrapper.getPrivateProjectObservable(privateProjectKey)
                )

                fun <T> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }
                fun <T> Observable<T>.publishImmediate() = publish().apply { domainDisposable += connect() }

                val privateProjectManagerSingle = privateProjectDatabaseRx.single
                        .map { AndroidPrivateProjectManager(deviceDbInfo.userInfo, it, ExactTimeStamp.now) }
                        .cacheImmediate()

                val friendDatabaseRx = DatabaseRx(
                        domainDisposable,
                        AndroidDatabaseWrapper.getFriendObservable(deviceInfo.key)
                )

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userDatabaseRx.single
                        .map { RemoteUserFactory(localFactory.uuid, it, deviceInfo) }
                        .cacheImmediate()

                val sharedProjectDatabaseRx = userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable }
                        .processChanges {
                            DatabaseRx(
                                    domainDisposable,
                                    AndroidDatabaseWrapper.getSharedProjectObservable(it)
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
                        .map { AndroidSharedProjectManager(it) }
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

                val rootInstanceDatabaseRx = taskRecordObservable.processChanges { _, taskRecord ->
                    Pair(
                            taskRecord,
                            DatabaseRx(
                                    domainDisposable,
                                    AndroidDatabaseWrapper.getRootInstanceObservable(taskRecord.rootInstanceKey)
                            )
                    )
                }
                        .doOnNext {
                            it.removedEntries
                                    .map { it.value.second }
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
                                        val snapshotInfoSingle = rootInstanceDatabaseRx.firstOrError().flatMap {
                                            it.newMap
                                                    .filterKeys { it.projectKey == projectId }
                                                    .values
                                                    .map { (taskRecord, databaseRx) ->
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
                                    .map { it.second.single.map { dataSnapshot -> Pair(it.first, dataSnapshot) } }
                                    .zipSingle()
                        }
                        .map {
                            it.map { (taskRecord, dataSnapshot) ->
                                taskRecord.taskKey to AndroidRootInstanceManager(
                                        taskRecord,
                                        dataSnapshot.toSnapshotInfos()
                                )
                            }.toMap()
                        }
                        .cacheImmediate()

                val rootInstanceEvents = rootInstanceDatabaseRx.switchMap {
                    it.newMap
                            .map { (taskKey, pair) ->
                                pair.second
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
                            ExactTimeStamp.now
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

                privateProjectDatabaseRx.changeObservable
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updatePrivateProjectRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                domainDisposable += sharedProjectEvents.subscribe {
                    domainFactorySingle.subscribe { domainFactory -> domainFactory.updateSharedProjectRecords(it) }.addTo(domainDisposable)
                }

                friendDatabaseRx.changeObservable
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateFriendRecords(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                userDatabaseRx.changeObservable
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

    private class DatabaseRx(
            domainDisposable: CompositeDisposable,
            databaseObservable: Observable<DataSnapshot>
    ) {

        val single: Single<DataSnapshot>
        val changeObservable: Observable<DataSnapshot>
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

    private class MapChanges<T, U>(
            val removedEntries: Map<T, U> = mapOf(),
            val addedEntries: Map<T, U> = mapOf(),
            val unchangedEntries: Map<T, U> = mapOf(),
            val oldMap: Map<T, U> = mapOf(),
            val newMap: Map<T, U> = mapOf()
    )

    private val typeToken = object : GenericTypeIndicator<Map<String, Map<String, InstanceJson>>>() {}

    private fun DataSnapshot.toSnapshotInfos() = getValue(typeToken)?.map { (dateString, timeMap) ->
        timeMap.map { (timeString, instanceJson) ->
            AndroidRootInstanceManager.SnapshotInfo(dateString, timeString, instanceJson)
        }
    }
            ?.flatten()
            ?: listOf()
}