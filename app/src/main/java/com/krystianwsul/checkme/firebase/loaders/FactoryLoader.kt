package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseEvent
import com.krystianwsul.checkme.firebase.factories.FriendFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.getCurrentValue
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.UserKey
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.*

class FactoryLoader(
        localFactory: FactoryProvider.Local,
        userInfoObservable: Observable<NullableWrapper<UserInfo>>,
        factoryProvider: FactoryProvider,
        tokenObservable: Observable<NullableWrapper<String>>
) {

    val domainFactoryObservable: Observable<NullableWrapper<FactoryProvider.Domain>>

    init {
        val domainDisposable = CompositeDisposable()

        fun <T> Single<T>.cacheImmediate() = cacheImmediate(domainDisposable)
        fun <T> Observable<T>.publishImmediate() = publishImmediate(domainDisposable)

        domainFactoryObservable = userInfoObservable.switchMapSingle {
            domainDisposable.clear()

            if (it.value != null) {
                val userInfo = it.value

                val deviceInfoObservable = tokenObservable.map { DeviceInfo(userInfo, it.value) }
                        .replay(1)
                        .apply { domainDisposable += connect() }

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

                val privateProjectManager = AndroidPrivateProjectManager(userInfo, factoryProvider.database)

                val privateProjectLoader = ProjectLoader.Impl(
                        privateProjectDatabaseRx.observable,
                        domainDisposable,
                        factoryProvider.projectProvider,
                        privateProjectManager
                )

                val startTime = ExactTimeStamp.now

                val userFactorySingle = userDatabaseRx.first
                        .map { MyUserFactory(it, getDeviceDbInfo(), factoryProvider) }
                        .cacheImmediate()

                val sharedProjectManager = AndroidSharedProjectManager(factoryProvider.database)

                val sharedProjectsLoader = SharedProjectsLoader.Impl(
                        userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable },
                        sharedProjectManager,
                        domainDisposable,
                        factoryProvider.sharedProjectsProvider
                )

                val projectsFactorySingle = Singles.zip(
                        privateProjectLoader.initialProjectEvent,
                        sharedProjectsLoader.initialProjectsEvent
                ) { (changeType, initialPrivateProjectEvent), initialSharedProjectsEvent ->
                    check(changeType == ChangeType.REMOTE)

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
                }.cacheImmediate()

                val friendKeysObservable = userFactorySingle.flatMapObservable { it.friendKeysObservable }
                        .scan(Pair(setOf<UserKey>(), setOf<UserKey>())) { old, new -> Pair(old.second, new) }
                        .publishImmediate()

                val friendEvents = friendKeysObservable.scan<Pair<Set<UserKey>, Map<UserKey, Observable<Snapshot>>>>(Pair(setOf(), mapOf())) { (_, oldMap), (oldFriendKeys, newFriendKeys) ->
                    val newMap = oldMap.toMutableMap()

                    val removedKeys = oldFriendKeys - newFriendKeys
                    removedKeys.forEach { newMap.remove(it) }

                    val addedKeys = newFriendKeys - oldFriendKeys
                    addedKeys.forEach {
                        check(!newMap.containsKey(it))

                        newMap[it] = factoryProvider.database
                                .getUserObservable(it)
                                .publishImmediate()
                    }

                    Pair(removedKeys, newMap)
                }
                        .switchMap { (removedKeys, addChangeObservables) ->
                            val removedEvents = Observable.fromIterable(removedKeys.map { DatabaseEvent.Remove(it.key) })

                            val addChangeEvents = addChangeObservables.values
                                    .map { it.map(DatabaseEvent::AddChange) }
                                    .merge()

                            listOf(removedEvents, addChangeEvents).merge()
                        }
                        .publishImmediate()

                val friendSingle = friendKeysObservable.firstOrError()
                        .flatMap { (old, new) ->
                            check(old.isEmpty())

                            new.map { factoryProvider.database.getUserSingle(it) }.zipSingle()
                        }
                        .cacheImmediate()

                val friendFactorySingle = friendSingle.map { FriendFactory(it) }

                val domainFactorySingle = Singles.zip(
                        userFactorySingle,
                        projectsFactorySingle,
                        friendFactorySingle
                ) { remoteUserFactory, projectsFactory, friendFactory ->
                    factoryProvider.newDomain(
                            localFactory,
                            remoteUserFactory,
                            projectsFactory,
                            friendFactory,
                            getDeviceDbInfo(),
                            startTime,
                            ExactTimeStamp.now
                    )
                }.cacheImmediate()

                domainDisposable += Observables.combineLatest(
                        projectsFactorySingle.flatMapObservable { it.changeTypes },
                        domainFactorySingle.toObservable()
                ).subscribe { (changeType, domainFactory) -> domainFactory.onProjectsInstancesChange(changeType, ExactTimeStamp.now) }

                friendSingle.flatMapObservable { friendEvents }
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateFriendRecords(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                userDatabaseRx.changes
                        .subscribe {
                            domainFactorySingle.subscribe { domainFactory -> domainFactory.updateUserRecord(it) }.addTo(domainDisposable)
                        }
                        .addTo(domainDisposable)

                domainDisposable += tokenObservable.subscribe { tokenWrapper ->
                    DomainFactory.addFirebaseListener { domainFactory ->
                        domainFactory.updateDeviceDbInfo(
                                DeviceDbInfo(DeviceInfo(userInfo, tokenWrapper.value), localFactory.uuid),
                                SaveService.Source.GUI
                        )
                    }
                }

                domainFactorySingle.map { NullableWrapper(it) }
            } else {
                factoryProvider.nullableInstance?.clearUserInfo()

                Single.just(NullableWrapper())
            }
        }
    }
}