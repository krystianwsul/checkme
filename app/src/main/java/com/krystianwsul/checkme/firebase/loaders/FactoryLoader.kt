package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.domainmodel.UserScope
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.database.DatabaseResultQueue
import com.krystianwsul.checkme.firebase.dependencies.RequestMerger
import com.krystianwsul.checkme.firebase.dependencies.RootTaskKeyStore
import com.krystianwsul.checkme.firebase.dependencies.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.OwnedProjectsFactory
import com.krystianwsul.checkme.firebase.foreignProjects.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootTasksManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.roottask.RootTaskDependencyCoordinator
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksLoader
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class FactoryLoader(
    userInfoObservable: Observable<NullableWrapper<UserInfo>>,
    factoryProvider: FactoryProvider,
    tokenObservable: Observable<NullableWrapper<String>>,
) {

    val userScopeObservable: Observable<NullableWrapper<UserScope>>

    init {
        val domainDisposable = CompositeDisposable()

        fun <T : Any> Single<T>.cacheImmediate() = cacheImmediate(domainDisposable)

        userScopeObservable = userInfoObservable.observeOnDomain()
            .switchMapSingle {
                domainDisposable.clear()

                if (it.value != null) {
                    val userInfo = it.value

                    val deviceDbInfoObservable = tokenObservable.observeOnDomain()
                        .map { DeviceDbInfo(DeviceInfo(userInfo, it.value), factoryProvider.uuid) }
                        .replay(1)
                        .apply { domainDisposable += connect() }

                    deviceDbInfoObservable.firstOrError().flatMap {
                        fun getDeviceDbInfo() = deviceDbInfoObservable.getCurrentValue()

                        val userDatabaseRx = DatabaseRx(
                            domainDisposable,
                            factoryProvider.database.getUserObservable(getDeviceDbInfo().key),
                        )

                        val privateProjectKey = getDeviceDbInfo().key.toPrivateProjectKey()

                        val privateProjectDatabaseRx = DatabaseRx(
                            domainDisposable,
                            factoryProvider.database.getPrivateOwnedProjectObservable(privateProjectKey),
                        )

                        val privateProjectManager = AndroidPrivateProjectManager(userInfo)

                        val rootModelChangeManager = RootModelChangeManager()

                        val userFactorySingle = userDatabaseRx.first
                            .map { MyUserFactory(it, getDeviceDbInfo(), factoryProvider.database, rootModelChangeManager) }
                            .cacheImmediate()

                        val triggerSource = object : RequestMerger.TriggerSource {

                            override val trigger = listOf(DatabaseResultQueue.onDequeued, DomainUpdater.onUpdated).merge()
                        }

                        val userKeyStore = UserKeyStore(
                            userFactorySingle.flatMapObservable { it.friendKeysObservable },
                            domainDisposable,
                            triggerSource,
                        )

                        val friendsLoader = FriendsLoader(userKeyStore, domainDisposable, factoryProvider.friendsProvider)

                        val friendsFactorySingle = friendsLoader.initialFriendsEvent
                            .map {
                                FriendsFactory(
                                    friendsLoader,
                                    it,
                                    domainDisposable,
                                    factoryProvider.database,
                                    rootModelChangeManager,
                                )
                            }
                            .cacheImmediate()

                        val userCustomTimeProviderSource = UserCustomTimeProviderSource.Impl(
                            userInfo.key,
                            userFactorySingle,
                            friendsLoader,
                            friendsFactorySingle,
                        )

                        val rootTaskKeySource = RootTaskKeyStore(triggerSource)

                        val rootTaskManager = AndroidRootTasksManager(factoryProvider.database)

                        val rootTasksLoader = RootTasksLoader(
                            rootTaskKeySource,
                            factoryProvider.database,
                            domainDisposable,
                            rootTaskManager,
                        )

                        val rootTaskToRootTaskCoordinator = RootTaskDependencyCoordinator.Impl(
                            rootTaskKeySource,
                            userCustomTimeProviderSource,
                        )

                        // this is hacky as fuck, but I'll take my chances
                        lateinit var projectsFactorySingle: Single<OwnedProjectsFactory>

                        /*
                        todo I should really add a class like RootTasksStorage or something, that both rootTasksFactory
                        and ForeignProjectCoordinator depend on.
                         */
                        lateinit var rootTasksFactory: RootTasksFactory

                        val foreignProjectCoordinator =
                            ForeignProjectCoordinator.Impl(privateProjectKey, userFactorySingle, domainDisposable) {
                                rootTasksFactory.rootTasks.values
                            }

                        val foreignProjectsLoader = ForeignProjectsLoader(
                            foreignProjectCoordinator.foreignProjectKeysObservable,
                            ForeignProjectsManager(),
                            domainDisposable,
                            ForeignProjectsProvider(factoryProvider),
                            userCustomTimeProviderSource,
                        )

                        val foreignProjectsFactory = ForeignProjectsFactory(foreignProjectsLoader, domainDisposable)

                        rootTasksFactory = RootTasksFactory(
                            rootTasksLoader,
                            userKeyStore,
                            rootTaskToRootTaskCoordinator,
                            domainDisposable,
                            rootTaskKeySource,
                            rootModelChangeManager,
                            foreignProjectCoordinator,
                            foreignProjectsFactory,
                        ) { projectsFactorySingle.getCurrentValue() }

                        RootTasksFactory.instanceRelay.accept(NullableWrapper(rootTasksFactory))

                        val privateProjectLoader = ProjectLoader.Impl(
                            privateProjectKey,
                            privateProjectDatabaseRx.observable,
                            domainDisposable,
                            privateProjectManager,
                            null,
                            userCustomTimeProviderSource,
                        ) {
                            rootTaskKeySource.onProjectAddedOrUpdated(it.projectKey, it.rootTaskParentDelegate.rootTaskKeys)
                        }

                        val startTime = ExactTimeStamp.Local.now

                        val sharedProjectManager = AndroidSharedProjectManager(factoryProvider.database)

                        val sharedProjectsLoader = SharedProjectsLoader.Impl(
                            userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable },
                            sharedProjectManager,
                            domainDisposable,
                            factoryProvider.sharedProjectsProvider,
                            userCustomTimeProviderSource,
                            userKeyStore,
                            rootTaskKeySource,
                        )

                        val notificationStorageSingle = factoryProvider.notificationStorageFactory
                            .getNotificationStorage()
                            .cacheImmediate(domainDisposable)

                        val shownFactorySingle =
                            notificationStorageSingle.map(factoryProvider::newShownFactory).cacheImmediate(domainDisposable)

                        projectsFactorySingle = Single.zip(
                            privateProjectLoader.initialProjectEvent.map {
                                check(it.changeType == ChangeType.REMOTE)

                                it.data
                            },
                            sharedProjectsLoader.initialProjectsEvent,
                            shownFactorySingle,
                        ) { initialPrivateProjectEvent, initialSharedProjectsEvent, shownFactory ->
                            OwnedProjectsFactory(
                                privateProjectLoader,
                                initialPrivateProjectEvent,
                                sharedProjectsLoader,
                                initialSharedProjectsEvent,
                                ExactTimeStamp.Local.now,
                                shownFactory,
                                domainDisposable,
                                rootTasksFactory,
                                rootModelChangeManager,
                                ::getDeviceDbInfo,
                            )
                        }.cacheImmediate()

                        val changeTypeSource = ChangeTypeSource(
                            projectsFactorySingle,
                            friendsFactorySingle,
                            userDatabaseRx,
                            userFactorySingle,
                            rootTasksFactory,
                            domainDisposable,
                        )

                        val userScopeSingle = userFactorySingle
                            .map {
                                UserScope(
                                    factoryProvider,
                                    rootTasksFactory,
                                    changeTypeSource,
                                    it,
                                    projectsFactorySingle,
                                    friendsFactorySingle,
                                    notificationStorageSingle,
                                    shownFactorySingle,
                                    tokenObservable,
                                    startTime,
                                    domainDisposable,
                                    foreignProjectsFactory,
                                    ::getDeviceDbInfo,
                                )
                            }
                            .cacheImmediate()

                        userScopeSingle.map(::NullableWrapper)
                    }
                } else {
                    factoryProvider.nullableInstance
                        ?.clearUserInfo()
                        ?.subscribe()

                    RootTasksFactory.instanceRelay.accept(NullableWrapper())

                    Single.just(NullableWrapper())
                }
            }
            .replay(1)
            .apply { connect() }
    }

    val domainFactoryObservable: Observable<NullableWrapper<FactoryProvider.Domain>> = userScopeObservable.switchMapSingle {
        it.value
            ?.domainFactorySingle
            ?.map(::NullableWrapper)
            ?: Single.just(NullableWrapper())
    }
}