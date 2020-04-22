package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.random.Random

@ExperimentalStdlibApi
class ProjectFactoryTest {

    class TestFactoryProvider : FactoryProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<Snapshot>>()

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override val database = object : FactoryProvider.Database() {

            override fun getUserSingle(userKey: UserKey): Single<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getPrivateProjectObservable(key: ProjectKey.Private): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot> {
                if (!sharedProjectObservables.containsKey(projectKey))
                    sharedProjectObservables[projectKey] = PublishRelay.create()
                return sharedProjectObservables.getValue(projectKey)
            }

            override fun getUserObservable(userKey: UserKey): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getNewId(path: String) = Random.nextInt().toString()

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }

        override val nullableInstance: FactoryProvider.Domain?
            get() = TODO("Not yet implemented")

        override val preferences: FactoryProvider.Preferences
            get() = TODO("Not yet implemented")

        override val shownFactory = mockk<Instance.ShownFactory>(relaxed = true)

        override fun newDomain(localFactory: FactoryProvider.Local, remoteUserFactory: RemoteUserFactory, projectsFactory: ProjectsFactory, remoteFriendFactory: RemoteFriendFactory, deviceDbInfo: DeviceDbInfo, startTime: ExactTimeStamp, readTime: ExactTimeStamp): FactoryProvider.Domain {
            TODO("Not yet implemented")
        }

        fun acceptSharedProject(
                projectKey: ProjectKey.Shared,
                projectJson: SharedProjectJson
        ) {
            sharedProjectObservables.getValue(projectKey).accept(ValueTestSnapshot(
                    JsonWrapper(projectJson),
                    projectKey.key
            ))
        }
    }

    class TestProjectLoader(projectKey: ProjectKey.Private) : ProjectLoader<ProjectType.Private> {

        private val userInfo = UserInfo("email", "name")

        override val projectManager = AndroidPrivateProjectManager(userInfo, mockk(relaxed = true))

        val projectRecord = PrivateProjectRecord(mockk(), projectKey, PrivateProjectJson())

        private val event = ProjectLoader.InitialProjectEvent(projectManager, projectRecord, mapOf())

        override val initialProjectEvent = Single.just(ChangeWrapper(ChangeType.REMOTE, event))

        override val addTaskEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.AddTaskEvent<ProjectType.Private>>>()

        override val changeInstancesEvents = PublishRelay.create<ProjectLoader.ChangeInstancesEvent<ProjectType.Private>>()

        override val changeProjectEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.ChangeProjectEvent<ProjectType.Private>>>()
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var factoryProvider: FactoryProvider
    private lateinit var projectLoader: TestProjectLoader

    private lateinit var projectFactory: PrivateProjectFactory

    private lateinit var changeTypesEmissionChecker: EmissionChecker<ChangeType>

    private val projectKey = ProjectKey.Private("projectKey")
    private val taskKey = TaskKey(projectKey, "taskKey")

    @BeforeClass
    fun beforeClass() {
        Task.USE_ROOT_INSTANCES = true
    }

    @Before
    fun before() {
        mockBase64()

        rxErrorChecker = RxErrorChecker()

        factoryProvider = TestFactoryProvider()
        projectLoader = TestProjectLoader(projectKey)

        projectLoader.initialProjectEvent
                .subscribeBy {
                    projectFactory = PrivateProjectFactory(
                            projectLoader,
                            it.data,
                            factoryProvider,
                            compositeDisposable
                    ) { mockk() }

                    changeTypesEmissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectFactory.changeTypes)
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

    @Test
    fun testAddTask() {
        changeTypesEmissionChecker.checkOne {
            projectLoader.addTaskEvents.accept(ChangeWrapper(
                    ChangeType.REMOTE,
                    ProjectLoader.AddTaskEvent(
                            projectLoader.projectRecord,
                            TaskRecord(
                                    taskKey.taskId,
                                    projectLoader.projectRecord,
                                    TaskJson("task")
                            ),
                            listOf()
                    )
            ))
        }
    }
}