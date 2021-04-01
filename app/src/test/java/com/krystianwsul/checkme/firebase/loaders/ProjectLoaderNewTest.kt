package com.krystianwsul.checkme.firebase.loaders

import android.util.Base64
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.PrivateTaskJson
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

@ExperimentalStdlibApi
class ProjectLoaderNewTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClassStatic() {
            Task.USE_ROOT_INSTANCES = true
        }
    }

    class TestProjectProvider : ProjectProvider {

        private val rootInstanceObservables = mutableMapOf<String, PublishRelay<IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>>>()

        override val database = object : ProjectProvider.Database() {

            override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>> {
                if (!rootInstanceObservables.containsKey(taskFirebaseKey))
                    rootInstanceObservables[taskFirebaseKey] = PublishRelay.create()
                return rootInstanceObservables.getValue(taskFirebaseKey)
            }

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }

        fun acceptInstance(
                projectId: String,
                taskId: String,
                map: Map<String, Map<String, InstanceJson>>
        ) {
            val key = "$projectId-$taskId"
            rootInstanceObservables.getValue(key).accept(ValueTestIndicatorSnapshot(map, key))
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var projectSnapshotRelay: BehaviorRelay<TypedSnapshot<PrivateProjectJson>>
    private lateinit var projectProvider: TestProjectProvider
    private lateinit var projectManager: AndroidPrivateProjectManager
    private lateinit var projectLoader: ProjectLoader<ProjectType.Private, PrivateProjectJson>

    private fun acceptProject(privateProjectJson: PrivateProjectJson) =
            projectSnapshotRelay.accept(ValueTestTypedSnapshot(privateProjectJson, projectKey.key))

    private lateinit var initialProjectEmissionChecker: EmissionChecker<ChangeWrapper<ProjectLoader.InitialProjectEvent<ProjectType.Private, PrivateProjectJson>>>
    private lateinit var addTaskEmissionChecker: EmissionChecker<ChangeWrapper<ProjectLoader.AddTaskEvent<ProjectType.Private>>>
    private lateinit var changeInstancesEmissionChecker: EmissionChecker<ProjectLoader.ChangeInstancesEvent<ProjectType.Private>>
    private lateinit var changeProjectEmissionChecker: EmissionChecker<ChangeWrapper<ProjectLoader.ChangeProjectEvent<ProjectType.Private>>>

    private val projectKey = ProjectKey.Private("userKey")

    @Before
    fun before() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns projectKey.key

        rxErrorChecker = RxErrorChecker()

        projectSnapshotRelay = BehaviorRelay.create()
        projectProvider = TestProjectProvider()
        projectManager = AndroidPrivateProjectManager(UserInfo("email", "name", "uid"), projectProvider.database)

        projectLoader = ProjectLoader.Impl(
                projectSnapshotRelay,
                compositeDisposable,
                projectProvider,
                projectManager,
        )

        initialProjectEmissionChecker = EmissionChecker("initialProject", compositeDisposable, projectLoader.initialProjectEvent)
        addTaskEmissionChecker = EmissionChecker("addTask", compositeDisposable, projectLoader.addTaskEvents)
        changeInstancesEmissionChecker = EmissionChecker("changeInstances", compositeDisposable, projectLoader.changeInstancesEvents)
        changeProjectEmissionChecker = EmissionChecker("changeProject", compositeDisposable, projectLoader.changeProjectEvents)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        initialProjectEmissionChecker.checkEmpty()
        addTaskEmissionChecker.checkEmpty()
        changeInstancesEmissionChecker.checkEmpty()
        changeProjectEmissionChecker.checkEmpty()

        rxErrorChecker.check()
    }

    @Test
    fun testInitial() {
        assertNull(projectLoader.initialProjectEvent.tryGetCurrentValue())
    }

    @Test
    fun testEmptyProject() {
        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson())
        }
    }

    @Test
    fun testSingleTask() {
        val taskId = "taskKey"

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        }
    }

    @Test
    fun testSingleTaskRepeat() {
        val taskId = "taskKey"

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task changed"))))
        }
    }

    @Test
    fun testSingleTaskAddTask() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to PrivateTaskJson("task1"))))

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())
        }

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                taskId1 to PrivateTaskJson("task1"),
                taskId2 to PrivateTaskJson("task2")
        )))

        addTaskEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        }
    }

    @Test
    fun testSingleTaskAddTaskEmitInstances() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to PrivateTaskJson("task1"))))

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())
        }

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                taskId1 to PrivateTaskJson("task1"),
                taskId2 to PrivateTaskJson("task2")
        )))

        addTaskEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        }

        changeInstancesEmissionChecker.checkOne {
            projectProvider.acceptInstance(projectKey.key, taskId1, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        }

        changeInstancesEmissionChecker.checkOne {
            projectProvider.acceptInstance(projectKey.key, taskId2, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        }
    }

    @Test
    fun testSingleTaskRemoveTask() {
        val taskId = "taskKey"

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson())
        }

        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
    }

    @Test
    fun testSingleTaskEmitInstancesChangeTaskEmitInstances() {
        val taskId = "taskKey"

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        }

        changeInstancesEmissionChecker.checkOne {
            projectProvider.acceptInstance(projectKey.key, taskId, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task change"))))
        }

        changeInstancesEmissionChecker.checkOne {
            projectProvider.acceptInstance(projectKey.key, taskId, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        }
    }

    @Test
    fun testChangeEmptyProject() {
        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson())
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(name = "asdf"))
        }
    }

    @Test
    fun testChangeEmptyTask() {
        val taskId = "taskKey"

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task change"))))
        }
    }

    @Test
    fun testChangeTwoEmptyTasks() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"


        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                taskId1 to PrivateTaskJson("task1"),
                taskId2 to PrivateTaskJson("task2")
        )))

        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())

        initialProjectEmissionChecker.checkRemote {
            projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                    taskId1 to PrivateTaskJson("task1 change"),
                    taskId2 to PrivateTaskJson("task2 change")
            )))
        }
    }

    @Test
    fun testLocalEvent() {
        ErrorLogger.instance = mockk(relaxed = true)

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson())
        }

        val name = "project"

        projectManager.value.single().name = name
        projectManager.save(mockk(relaxed = true))

        changeProjectEmissionChecker.checkLocal {
            acceptProject(PrivateProjectJson(name = name))
        }
    }
}