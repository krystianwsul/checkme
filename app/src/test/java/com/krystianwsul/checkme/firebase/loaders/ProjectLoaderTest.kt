package com.krystianwsul.checkme.firebase.loaders

import android.util.Base64
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class ProjectLoaderTest {

    class TestProjectProvider : ProjectProvider {

        private val rootInstanceObservables = mutableMapOf<String, PublishRelay<Snapshot>>()

        override val database = object : ProjectProvider.Database() {

            override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot> {
                if (!rootInstanceObservables.containsKey(taskFirebaseKey))
                    rootInstanceObservables[taskFirebaseKey] = PublishRelay.create()
                return rootInstanceObservables.getValue(taskFirebaseKey)
            }

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }

        fun acceptInstance(
                projectId: String,
                taskId: String,
                map: Map<String, Map<String, InstanceJson>>
        ) {
            val key = "$projectId-$taskId"
            rootInstanceObservables.getValue(key).accept(ValueTestSnapshot(map, key))
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
    private lateinit var changeInstancesEmissionChecker: EmissionChecker<ChangeWrapper<ProjectLoader.ChangeInstancesEvent<ProjectType.Private>>>
    private lateinit var changeProjectEmissionChecker: EmissionChecker<ChangeWrapper<ProjectLoader.ChangeProjectEvent<ProjectType.Private>>>

    private val projectKey = ProjectKey.Private("userKey")

    @Before
    fun before() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns projectKey.key

        rxErrorChecker = RxErrorChecker()

        projectSnapshotRelay = BehaviorRelay.create()
        projectProvider = TestProjectProvider()
        projectManager = AndroidPrivateProjectManager(UserInfo("email", "name"), projectProvider.database)

        projectLoader = ProjectLoader(
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
        initialProjectEmissionChecker.checkRemote()

        acceptProject(PrivateProjectJson())
    }

    @Test
    fun testSingleTask() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
    }

    @Test
    fun testSingleTaskRepeat() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        initialProjectEmissionChecker.checkEmpty()

        changeProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task changed"))))
        changeProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testSingleTaskAddTask() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to TaskJson("task1"))))
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())
        initialProjectEmissionChecker.checkEmpty()

        addTaskEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                        taskId1 to TaskJson("task1"),
                        taskId2 to TaskJson("task2")
        )))
        addTaskEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        addTaskEmissionChecker.checkEmpty()
    }

    @Test
    fun testSingleTaskAddTaskEmitInstances() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to TaskJson("task1"))))
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())
        initialProjectEmissionChecker.checkEmpty()

        addTaskEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                        taskId1 to TaskJson("task1"),
                        taskId2 to TaskJson("task2")
        )))
        addTaskEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        addTaskEmissionChecker.checkEmpty()

        changeInstancesEmissionChecker.checkRemote()
        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionChecker.checkEmpty()

        changeInstancesEmissionChecker.checkRemote()
        projectProvider.acceptInstance(projectKey.key, taskId2, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionChecker.checkEmpty()
    }

    @Test
    fun testSingleTaskRemoveTask() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        initialProjectEmissionChecker.checkEmpty()

        changeProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson())
        changeProjectEmissionChecker.checkEmpty()

        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
    }

    @Test
    fun testSingleTaskEmitInstancesChangeTaskEmitInstances() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        initialProjectEmissionChecker.checkEmpty()

        changeInstancesEmissionChecker.checkRemote()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionChecker.checkEmpty()

        changeProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task change"))))
        changeProjectEmissionChecker.checkEmpty()

        changeInstancesEmissionChecker.checkRemote()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionChecker.checkEmpty()
    }

    @Test
    fun testChangeEmptyProject() {
        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson())
        initialProjectEmissionChecker.checkEmpty()

        changeProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(name = "asdf"))
        changeProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testChangeEmptyTask() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        initialProjectEmissionChecker.checkEmpty()

        changeProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task change"))))
        changeProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testChangeTwoEmptyTasks() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote()

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                taskId1 to TaskJson("task1"),
                taskId2 to TaskJson("task2")
        )))

        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())
        initialProjectEmissionChecker.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        initialProjectEmissionChecker.checkEmpty()

        changeProjectEmissionChecker.checkRemote()

        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                taskId1 to TaskJson("task1 change"),
                taskId2 to TaskJson("task2 change")
        )))

        changeProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testLocalEvent() {
        ErrorLogger.instance = mockk(relaxed = true)

        initialProjectEmissionChecker.checkRemote()
        acceptProject(PrivateProjectJson())
        initialProjectEmissionChecker.checkEmpty()

        val name = "project"

        projectManager.privateProjectRecords.first().name = name
        projectManager.save(mockk(relaxed = true))

        changeProjectEmissionChecker.checkLocal()
        acceptProject(PrivateProjectJson(name = name))
        changeProjectEmissionChecker.checkEmpty()
    }
}