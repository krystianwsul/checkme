package com.krystianwsul.checkme.firebase

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*

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

                val privateProjectManagerSingle = AndroidDatabaseWrapper.getPrivateProjectSingle(deviceInfo.key.toPrivateProjectKey())
                        .map { AndroidPrivateProjectManager(deviceDbInfo.userInfo, it, ExactTimeStamp.now) }
                        .cache()

                val friendSingle = AndroidDatabaseWrapper.getFriendSingle(deviceInfo.key).cache()

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userSingle.map { RemoteUserFactory(localFactory.uuid, it, deviceInfo) }.cache()

                val sharedProjectKeysObservable = userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable }
                        .scan(Pair(setOf<ProjectKey.Shared>(), setOf<ProjectKey.Shared>())) { old, new -> Pair(old.second, new) }
                        .publish()
                        .apply { domainDisposable += connect() }

                val sharedProjectEvents = sharedProjectKeysObservable.scan<Pair<Set<ProjectKey.Shared>, Map<ProjectKey.Shared, Observable<DataSnapshot>>>>(Pair(setOf(), mapOf())) { (_, oldMap), (oldProjectIds, newProjectIds) ->
                            val newMap = oldMap.toMutableMap()

                            val removedIds = oldProjectIds - newProjectIds
                            removedIds.forEach { newMap.remove(it) }

                            val addedIds = newProjectIds - oldProjectIds
                            addedIds.forEach {
                                check(!newMap.containsKey(it))

                                newMap[it] = AndroidDatabaseWrapper.getSharedProjectObservable(it)
                                        .publish()
                                        .apply { connect() }
                            }

                            Pair(removedIds, newMap)
                        }
                        .switchMap { (removedIds, addChangeObservables) ->
                            val removedEvents = Observable.fromIterable(removedIds.map { DatabaseEvent.Remove(it.key) })

                            val addChangeEvents = addChangeObservables.values
                                    .map { it.map(DatabaseEvent::AddChange) }
                                    .merge()

                            listOf(removedEvents, addChangeEvents).merge()
                        }
                        .publish()
                        .apply { domainDisposable += connect() }

                val sharedProjectManagerSingle = sharedProjectKeysObservable.firstOrError()
                        .flatMap { (old, new) ->
                            check(old.isEmpty())

                            new.takeIf { it.isNotEmpty() }
                                    ?.map { AndroidDatabaseWrapper.getSharedProjectSingle(it) }
                                    ?.zipSingle()
                                    ?: Single.just(listOf())
                        }
                        .map { AndroidSharedProjectManager(it) }
                        .cache()

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
                        }
                        .publish()
                        .apply { domainDisposable += connect() }

                val rootInstanceManagerSingle = taskRecordObservable.firstOrError()
                        .flatMap {
                            it.map { taskRecord ->
                                AndroidDatabaseWrapper.getRootInstanceSingle(taskRecord.rootInstanceKey).map { taskRecord to it }
                            }.zipSingle()
                        }
                        .map {
                            it.map { (taskRecord, dataSnapshot) ->
                                val snapshotInfos = dataSnapshot.children
                                        .map {
                                            val dateString = it.key!!

                                            it.children.map {
                                                AndroidRootInstanceManager.SnapshotInfo(dateString, it.key!!, it)
                                            }
                                        }
                                        .flatten()

                                taskRecord.taskKey to AndroidRootInstanceManager(taskRecord, snapshotInfos)
                            }.toMap()
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
                            rootInstanceManagers,
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

                privateProjectManagerSingle.flatMapObservable { privateProjectObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updatePrivateProjectRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                sharedProjectManagerSingle.flatMapObservable { sharedProjectEvents }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateSharedProjectRecords(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                friendSingle.flatMapObservable { friendObservable }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateFriendRecords(it) }.addTo(domainDisposable)
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