package com.krystianwsul.checkme.firebase.factories

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.PrivateTaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.random.Random

@ExperimentalStdlibApi
class ProjectFactoryOldTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClassStatic() {
            Task.USE_ROOT_INSTANCES = false
        }
    }

    class TestFactoryProvider : FactoryProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<TypedSnapshot<JsonWrapper>>>()

        override val projectProvider = ProjectLoaderOldTest.TestProjectProvider()

        override val database = object : FactoryProvider.Database() {

            override fun getPrivateProjectObservable(key: ProjectKey.Private): Observable<TypedSnapshot<PrivateProjectJson>> {
                TODO("Not yet implemented")
            }

            override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>> {
                TODO("Not yet implemented")
            }

            override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<TypedSnapshot<JsonWrapper>> {
                if (!sharedProjectObservables.containsKey(projectKey))
                    sharedProjectObservables[projectKey] = PublishRelay.create()
                return sharedProjectObservables.getValue(projectKey)
            }

            override fun getUserObservable(userKey: UserKey): Observable<TypedSnapshot<UserWrapper>> {
                TODO("Not yet implemented")
            }

            override fun getNewId(path: String) = Random.nextInt().toString()

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }

        override val nullableInstance: FactoryProvider.Domain
            get() = TODO("Not yet implemented")

        override val shownFactory = mockk<Instance.ShownFactory>(relaxed = true)

        override val domainUpdater: DomainUpdater
            get() = TODO("Not yet implemented")

        override fun newDomain(
                localFactory: FactoryProvider.Local,
                myUserFactory: MyUserFactory,
                projectsFactory: ProjectsFactory,
                friendsFactory: FriendsFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp.Local,
                readTime: ExactTimeStamp.Local,
                domainDisposable: CompositeDisposable,
        ): FactoryProvider.Domain {
            TODO("Not yet implemented")
        }

        fun acceptSharedProject(
                projectKey: ProjectKey.Shared,
                projectJson: SharedProjectJson
        ) {
            sharedProjectObservables.getValue(projectKey).accept(ValueTestTypedSnapshot(
                    JsonWrapper(projectJson),
                    projectKey.key,
            ))
        }
    }

    class TestProjectLoader(projectKey: ProjectKey.Private) : ProjectLoader<ProjectType.Private, PrivateProjectJson> {

        private val userInfo = UserInfo("email", "name", "uid")

        override val projectManager = AndroidPrivateProjectManager(userInfo, mockk(relaxed = true))

        val projectRecord = PrivateProjectRecord(mockk(), projectKey, PrivateProjectJson())

        private val event = ProjectLoader.InitialProjectEvent(projectManager, projectRecord, mapOf())

        override val initialProjectEvent = Single.just(ChangeWrapper(ChangeType.REMOTE, event))!!

        override val addTaskEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.AddTaskEvent<ProjectType.Private>>>()!!

        override val changeInstancesEvents = Observable.never<ProjectLoader.ChangeInstancesEvent<ProjectType.Private>>()!!

        override val changeProjectEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.ChangeProjectEvent<ProjectType.Private>>>()!!
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var factoryProvider: FactoryProvider
    private lateinit var projectLoader: TestProjectLoader

    private lateinit var projectFactory: PrivateProjectFactory

    private lateinit var changeTypesEmissionChecker: EmissionChecker<ChangeType>

    private val projectKey = ProjectKey.Private("projectKey")
    private val taskKey = TaskKey(projectKey, "taskKey")

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
                            PrivateTaskRecord(
                                    taskKey.taskId,
                                    projectLoader.projectRecord,
                                    PrivateTaskJson("task")
                            ),
                            EmptyTestIndicatorSnapshot(),
                    )
            ))
        }
    }
}