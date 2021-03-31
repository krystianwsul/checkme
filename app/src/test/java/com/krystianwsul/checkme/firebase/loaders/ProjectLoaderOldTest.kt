package com.krystianwsul.checkme.firebase.loaders

import android.util.Base64
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
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
class ProjectLoaderOldTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClassStatic() {
            Task.USE_ROOT_INSTANCES = false
        }
    }

    class TestProjectProvider : ProjectProvider {

        override val database = object : ProjectProvider.Database() {

            override fun getRootInstanceObservable(taskFirebaseKey: String) = Observable.just<Snapshot>(EmptyTestSnapshot())

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var projectSnapshotRelay: BehaviorRelay<Snapshot>
    private lateinit var projectProvider: TestProjectProvider
    private lateinit var projectManager: AndroidPrivateProjectManager
    private lateinit var projectLoader: ProjectLoader<ProjectType.Private>

    private fun acceptProject(privateProjectJson: PrivateProjectJson) =
            projectSnapshotRelay.accept(ValueTestSnapshot(privateProjectJson, projectKey.key))

    private lateinit var initialProjectEmissionChecker: EmissionChecker<ChangeWrapper<ProjectLoader.InitialProjectEvent<ProjectType.Private>>>
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
                projectManager
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

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }
    }

    @Test
    fun testSingleTaskRepeat() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task changed"))))
        }
    }

    @Test
    fun testSingleTaskAddTask() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to PrivateTaskJson("task1"))))
        }

        addTaskEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                    taskId1 to PrivateTaskJson("task1"),
                    taskId2 to PrivateTaskJson("task2")
            )))
        }
    }

    @Test
    fun testSingleTaskAddTaskEmitInstances() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to PrivateTaskJson("task1"))))
        }

        addTaskEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                    taskId1 to PrivateTaskJson("task1"),
                    taskId2 to PrivateTaskJson("task2")
            )))
        }
    }

    @Test
    fun testSingleTaskRemoveTask() {
        val taskId = "taskKey"


        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson())
        }
    }

    @Test
    fun testSingleTaskEmitInstancesChangeTaskEmitInstances() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task change"))))
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

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task change"))))
        }
    }

    @Test
    fun testChangeTwoEmptyTasks() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                    taskId1 to PrivateTaskJson("task1"),
                    taskId2 to PrivateTaskJson("task2")
            )))
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