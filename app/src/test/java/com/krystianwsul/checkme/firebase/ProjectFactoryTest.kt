package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.mockk.mockk
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class ProjectFactoryTest {

    private class TestFactoryProvider : FactoryProvider {

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override val database = mockk<FactoryProvider.Database>()

        override val nullableInstance: FactoryProvider.Domain?
            get() = TODO("Not yet implemented")

        override val preferences: FactoryProvider.Preferences
            get() = TODO("Not yet implemented")

        override val shownFactory = mockk<Instance.ShownFactory>()

        override fun newDomain(localFactory: FactoryProvider.Local, remoteUserFactory: RemoteUserFactory, projectsFactory: ProjectsFactory, deviceDbInfo: DeviceDbInfo, startTime: ExactTimeStamp, readTime: ExactTimeStamp, friendSnapshot: Snapshot): FactoryProvider.Domain {
            TODO("Not yet implemented")
        }
    }

    private val projectKey = ProjectKey.Private("projectKey")

    private class TestProjectLoader(projectKey: ProjectKey.Private) : ProjectLoader<ProjectType.Private> {

        private val userInfo = UserInfo("email", "name")

        val projectManager = AndroidPrivateProjectManager(userInfo, mockk(relaxed = true))

        val projectRecord = PrivateProjectRecord(
                mockk(),
                projectKey,
                PrivateProjectJson()
        )

        override val initialProjectEvent = Single.just(ChangeWrapper(
                ChangeType.REMOTE,
                ProjectLoader.InitialProjectEvent(
                        projectManager,
                        projectRecord,
                        mapOf()
                )
        ))

        override val addTaskEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.AddTaskEvent<ProjectType.Private>>>()

        override val changeInstancesEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.ChangeInstancesEvent<ProjectType.Private>>>()

        override val changeProjectEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.ChangeProjectEvent<ProjectType.Private>>>()
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var factoryProvider: FactoryProvider
    private lateinit var projectLoader: TestProjectLoader

    private lateinit var projectFactory: PrivateProjectFactory

    private lateinit var changeTypesEmissionChecker: EmissionChecker<ChangeType>

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
                    )

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
        changeTypesEmissionChecker.addHandler { } // todo instances figure out local/remote after ProjectsFactoryTest
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
        changeTypesEmissionChecker.checkEmpty()
    }
}