package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.DatabaseEvent
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
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.*

class FactoryLoader(
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

                val sharedProjectEvents = sharedProjectDatabaseRx.skip(1)
                        .switchMap {
                            val removedEvents = Observable.fromIterable(
                                    it.removedEntries
                                            .keys
                                            .map { DatabaseEvent.Remove(it.key) }
                            )

                            val addChangeEvents = it.newMap.values
                                    .map { it.changeObservable.map { DatabaseEvent.AddChange(it) } }
                                    .merge()

                            listOf(removedEvents, addChangeEvents).merge()
                        }
                        .publishImmediate()

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
                        .publishImmediate()

                fun DataSnapshot.toSnapshotInfos() = children.map { // todo instances use class for parsing
                    val dateString = it.key!!

                    it.children.map {
                        AndroidRootInstanceManager.SnapshotInfo(dateString, it.key!!, it)
                    }
                }.flatten()

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

                val rootInstanceTaskEvents = rootInstanceDatabaseRx.skip(1)
                        .switchMap {
                            val removeTaskEvents = it.removedEntries
                                    .keys
                                    .map { Observable.just(ProjectFactory.InstanceEvent.RemoveTask(it) as ProjectFactory.InstanceEvent) }

                            val addTaskEvents = it.addedEntries
                                    .values
                                    .map { (taskRecord, databaseRx) ->
                                        databaseRx.single
                                                .map<ProjectFactory.InstanceEvent> {
                                                    ProjectFactory.InstanceEvent.AddTask(taskRecord, it.toSnapshotInfos())
                                                }
                                                .toObservable()
                                    }

                            listOf(removeTaskEvents, addTaskEvents).flatten().merge()
                        }

                val rootInstanceInstanceEvents = rootInstanceDatabaseRx.skip(1)
                        .switchMap {
                            it.newMap
                                    .map { (taskKey, pair) ->
                                        pair.second
                                                .changeObservable
                                                .map<ProjectFactory.InstanceEvent> {
                                                    ProjectFactory.InstanceEvent.Instances(taskKey, it.toSnapshotInfos())
                                                }
                                    }
                                    .merge()
                        }

                val rootInstanceEvents = listOf(rootInstanceTaskEvents, rootInstanceInstanceEvents).merge()

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

                rootInstanceEvents.switchMapSingle {
                    domainFactorySingle.map { domainFactory -> Pair(domainFactory, it) }
                }
                        .subscribe { (domainFactory, instanceEvent) -> domainFactory.updateInstanceRecords(instanceEvent) }
                        .addTo(domainDisposable)

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                DomainFactory.nullableInstance?.clearUserInfo()

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
}