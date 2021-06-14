package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.domainmodel.extensions.updateDeviceDbInfo
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.AndroidRootTasksManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.roottask.*
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

class FactoryLoader(
    localFactory: FactoryProvider.Local,
    userInfoObservable: Observable<NullableWrapper<UserInfo>>,
    factoryProvider: FactoryProvider,
    tokenObservable: Observable<NullableWrapper<String>>,
) {

    val domainFactoryObservable: Observable<NullableWrapper<FactoryProvider.Domain>>

    init {
        val domainDisposable = CompositeDisposable()

        fun <T> Single<T>.cacheImmediate() = cacheImmediate(domainDisposable)

        domainFactoryObservable = userInfoObservable.observeOnDomain().switchMapSingle {
            domainDisposable.clear()

            if (it.value != null) {
                val userInfo = it.value

                val deviceInfoObservable = tokenObservable.observeOnDomain()
                    .map { DeviceInfo(userInfo, it.value) }
                    .replay(1)
                    .apply { domainDisposable += connect() }

                deviceInfoObservable.firstOrError().flatMap {
                    fun getDeviceInfo() = deviceInfoObservable.getCurrentValue()
                    fun getDeviceDbInfo() = DeviceDbInfo(getDeviceInfo(), localFactory.uuid)

                    val userDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getUserObservable(getDeviceInfo().key),
                    )

                    val privateProjectKey = getDeviceInfo().key.toPrivateProjectKey()

                    val privateProjectDatabaseRx = DatabaseRx(
                        domainDisposable,
                        factoryProvider.database.getPrivateProjectObservable(privateProjectKey),
                    )

                    val privateProjectManager = AndroidPrivateProjectManager(userInfo, factoryProvider.database)

                    val userFactorySingle = userDatabaseRx.first
                        .map { MyUserFactory(it, getDeviceDbInfo(), factoryProvider.database) }
                        .cacheImmediate()

                    val userKeyStore = UserKeyStore(
                        userFactorySingle.flatMapObservable { it.friendKeysObservable },
                        domainDisposable,
                    )

                    val friendsLoader = FriendsLoader(userKeyStore, domainDisposable, factoryProvider.friendsProvider)

                    val friendsFactorySingle = friendsLoader.initialFriendsEvent
                        .map {
                            FriendsFactory(
                                friendsLoader,
                                it,
                                domainDisposable,
                                factoryProvider.database,
                            )
                        }
                        .cacheImmediate()

                    val userCustomTimeProviderSource = UserCustomTimeProviderSource.Impl(
                        userInfo.key,
                        userFactorySingle,
                        friendsLoader,
                        friendsFactorySingle,
                    )

                    val rootTaskKeySource = RootTaskKeySource()

                    val rootTaskManager = AndroidRootTasksManager(factoryProvider.database)

                    val loadDependencyTrackerManager = LoadDependencyTrackerManager()

                    val rootTasksLoader = RootTasksLoader(
                        rootTaskKeySource,
                        factoryProvider.database,
                        domainDisposable,
                        rootTaskManager,
                        loadDependencyTrackerManager,
                    )

                    val recordRootTaskDependencyStateContainer = RootTaskDependencyStateContainer.Impl()

                    val taskRecordLoader =
                        TaskRecordsLoadedTracker.Impl(
                            rootTasksLoader,
                            recordRootTaskDependencyStateContainer,
                            domainDisposable
                        )

                    val rootTaskToRootTaskCoordinator = RootTaskDependencyCoordinator.Impl(
                        rootTaskKeySource,
                        rootTasksLoader,
                        userCustomTimeProviderSource,
                        taskRecordLoader,
                        recordRootTaskDependencyStateContainer,
                    )

                    // this is hacky as fuck, but I'll take my chances
                    lateinit var projectsFactorySingle: Single<ProjectsFactory>

                    val modelRootTaskDependencyStateContainer = RootTaskDependencyStateContainer.Impl()

                    val rootTasksFactory = RootTasksFactory(
                        rootTasksLoader,
                        userKeyStore,
                        rootTaskToRootTaskCoordinator,
                        domainDisposable,
                        rootTaskKeySource,
                        loadDependencyTrackerManager,
                        modelRootTaskDependencyStateContainer,
                    ) { projectsFactorySingle.getCurrentValue() }

                    val projectToRootTaskCoordinator = ProjectToRootTaskCoordinator.Impl(
                        rootTaskKeySource,
                        rootTasksFactory,
                        modelRootTaskDependencyStateContainer,
                    )

                    val privateProjectLoader = ProjectLoader.Impl(
                        privateProjectDatabaseRx.observable,
                        domainDisposable,
                        privateProjectManager,
                        null,
                        userCustomTimeProviderSource,
                        projectToRootTaskCoordinator,
                        loadDependencyTrackerManager,
                    )

                    val startTime = ExactTimeStamp.Local.now

                    val sharedProjectManager = AndroidSharedProjectManager(factoryProvider.database)

                    val sharedProjectsLoader = SharedProjectsLoader.Impl(
                        userFactorySingle.flatMapObservable { it.sharedProjectKeysObservable },
                        sharedProjectManager,
                        domainDisposable,
                        factoryProvider.sharedProjectsProvider,
                        userCustomTimeProviderSource,
                        userKeyStore,
                        projectToRootTaskCoordinator,
                        rootTaskKeySource,
                        loadDependencyTrackerManager,
                    )

                    projectsFactorySingle = Single.zip(
                        privateProjectLoader.initialProjectEvent.map {
                            check(it.changeType == ChangeType.REMOTE)

                            it.data
                        },
                        sharedProjectsLoader.initialProjectsEvent,
                    ) { initialPrivateProjectEvent, initialSharedProjectsEvent ->
                        ProjectsFactory(
                            localFactory,
                            privateProjectLoader,
                            initialPrivateProjectEvent,
                            sharedProjectsLoader,
                            initialSharedProjectsEvent,
                            ExactTimeStamp.Local.now,
                            factoryProvider,
                            domainDisposable,
                            rootTasksFactory,
                            ::getDeviceDbInfo,
                        )
                    }.cacheImmediate()

                    val domainFactorySingle = Single.zip(
                        userFactorySingle,
                        projectsFactorySingle,
                        friendsFactorySingle,
                        factoryProvider.notificationStorageFactory.getNotificationStorage(),
                    ) { remoteUserFactory, projectsFactory, friendsFactory, notificationStorage ->
                        factoryProvider.newDomain(
                            localFactory,
                            remoteUserFactory,
                            projectsFactory,
                            friendsFactory,
                            getDeviceDbInfo(),
                            startTime,
                            ExactTimeStamp.Local.now,
                            domainDisposable,
                            rootTasksFactory,
                            notificationStorage,
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

                    // ignore all change events that come in before the DomainFactory is initialized
                    domainFactorySingle.flatMapObservable { domainFactory ->
                        changeTypeSource.changeTypes.map { domainFactory to it }
                    }
                        .subscribe { (domainFactory, changeType) ->
                            domainFactory.onChangeTypeEvent(changeType, ExactTimeStamp.Local.now)
                        }
                        .addTo(domainDisposable)

                    tokenObservable.flatMapCompletable {
                        factoryProvider.domainUpdater.updateDeviceDbInfo(
                            DeviceDbInfo(DeviceInfo(userInfo, it.value), localFactory.uuid)
                        )
                    }
                        .subscribe()
                        .addTo(domainDisposable)

                    domainFactorySingle.map(::NullableWrapper)
                }
            } else {
                factoryProvider.nullableInstance
                    ?.clearUserInfo()
                    ?.subscribe()

                Single.just(NullableWrapper())
            }
        }
    }
}