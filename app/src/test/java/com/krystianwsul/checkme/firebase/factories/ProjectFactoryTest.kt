package com.krystianwsul.checkme.firebase.factories

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateForeignProjectJson
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedForeignProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedOwnedProjectJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@ExperimentalStdlibApi
class ProjectFactoryTest {

    class TestFactoryProvider : FactoryProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<Snapshot<JsonWrapper>>>()

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override val database = object : FactoryProvider.Database() {

            override fun getPrivateOwnedProjectObservable(projectKey: ProjectKey.Private): Observable<Snapshot<PrivateOwnedProjectJson>> {
                TODO("Not yet implemented")
            }

            override fun getSharedOwnedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot<JsonWrapper>> {
                if (!sharedProjectObservables.containsKey(projectKey))
                    sharedProjectObservables[projectKey] = PublishRelay.create()
                return sharedProjectObservables.getValue(projectKey)
            }

            override fun getPrivateForeignProjectObservable(projectKey: ProjectKey.Private): Observable<Snapshot<PrivateForeignProjectJson>> {
                TODO("Not yet implemented")
            }

            override fun getSharedForeignProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot<SharedForeignProjectJson>> {
                TODO("Not yet implemented")
            }

            override fun getUserObservable(userKey: UserKey): Observable<Snapshot<UserWrapper>> {
                TODO("Not yet implemented")
            }

            override fun getRootTaskObservable(taskKey: TaskKey.Root): Observable<Snapshot<RootTaskJson>> {
                TODO("Not yet implemented")
            }

            override fun getNewId(path: String) = Random.nextInt().toString()

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }

        override val nullableInstance: FactoryProvider.Domain
            get() = TODO("Not yet implemented")

        val shownFactory = mockk<Instance.ShownFactory>(relaxed = true)

        override fun newShownFactory(notificationStorage: FactoryProvider.NotificationStorage) = shownFactory

        override val domainUpdater: DomainUpdater
            get() = TODO("Not yet implemented")

        override val notificationStorageFactory = mockk<FactoryProvider.NotificationStorageFactory> {
            every { getNotificationStorage() } returns Single.just(mockk(relaxed = true))
        }

        override val uuid = "uuid"

        override fun newDomain(
            shownFactory: Instance.ShownFactory,
            myUserFactory: MyUserFactory,
            projectsFactory: ProjectsFactory,
            friendsFactory: FriendsFactory,
            deviceDbInfo: DeviceDbInfo,
            startTime: ExactTimeStamp.Local,
            readTime: ExactTimeStamp.Local,
            domainDisposable: CompositeDisposable,
            rootTasksFactory: RootTasksFactory,
            notificationStorage: FactoryProvider.NotificationStorage,
            domainListenerManager: DomainListenerManager,
        ): FactoryProvider.Domain {
            TODO("Not yet implemented")
        }

        fun acceptSharedProject(
            projectKey: ProjectKey.Shared,
            projectJson: SharedOwnedProjectJson
        ) {
            sharedProjectObservables.getValue(projectKey).accept(Snapshot(
                    projectKey.key,
                    JsonWrapper(projectJson),
            ))
        }
    }

    class TestProjectLoader(projectKey: ProjectKey.Private) :
        ProjectLoader<ProjectType.Private, PrivateOwnedProjectJson, PrivateOwnedProjectRecord> {

        private val userInfo = UserInfo("email", "name", "uid")

        override val projectManager = AndroidPrivateProjectManager(userInfo)

        private val projectRecord = PrivateOwnedProjectRecord(projectKey, PrivateOwnedProjectJson())

        private val event = ProjectLoader.InitialProjectEvent(projectManager, projectRecord, mockk())

        override val initialProjectEvent = Single.just(ChangeWrapper(ChangeType.REMOTE, event))

        override val changeProjectEvents =
            PublishRelay.create<ProjectLoader.ChangeProjectEvent<PrivateOwnedProjectRecord>>()
    }

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var factoryProvider: TestFactoryProvider
    private lateinit var projectLoader: TestProjectLoader

    private lateinit var projectFactory: PrivateProjectFactory

    private lateinit var changeTypesEmissionChecker: EmissionChecker<Unit>

    private val projectKey = ProjectKey.Private("projectKey")

    @Before
    fun before() {
        mockBase64()

        rxErrorChecker = RxErrorChecker()

        factoryProvider = TestFactoryProvider()
        projectLoader = TestProjectLoader(projectKey)

        val existingInstanceChangeManager = RootModelChangeManager()

        projectLoader.initialProjectEvent
            .subscribeBy {
                projectFactory = PrivateProjectFactory(
                    projectLoader,
                    it.data,
                    factoryProvider.shownFactory,
                    compositeDisposable,
                    mockk(relaxed = true),
                    existingInstanceChangeManager,
                ) { mockk() }

                    changeTypesEmissionChecker =
                        EmissionChecker("changeTypes", compositeDisposable, projectFactory.remoteChanges)
                }
                .addTo(compositeDisposable)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        changeTypesEmissionChecker.checkEmpty()

        rxErrorChecker.check()
    }

    @Test
    fun testInitial() {

    }
}