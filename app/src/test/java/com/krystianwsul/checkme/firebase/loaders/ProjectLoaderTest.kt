package com.krystianwsul.checkme.firebase.loaders

import android.util.Base64
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.mockk.every
import io.mockk.mockkStatic
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.plusAssign
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class ProjectLoaderTest {

    class TestProjectProvider : ProjectProvider {

        private val rootInstanceObservables = mutableMapOf<String, PublishRelay<FactoryProvider.Database.Snapshot>>()

        override val database = object : ProjectProvider.Database() {

            override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<FactoryProvider.Database.Snapshot> {
                if (!rootInstanceObservables.containsKey(taskFirebaseKey))
                    rootInstanceObservables[taskFirebaseKey] = PublishRelay.create()
                return rootInstanceObservables.getValue(taskFirebaseKey)
            }

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
                TODO("Not yet implemented")
            }
        }

        fun acceptInstance(
                projectId: String,
                taskId: String,
                map: Map<String, Map<String, InstanceJson>>
        ) = rootInstanceObservables.getValue("$projectId-$taskId").accept(FactoryLoaderTest.ValueTestSnapshot(map))
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var projectSnapshotRelay: BehaviorRelay<FactoryProvider.Database.Snapshot>
    private lateinit var projectProvider: TestProjectProvider
    private lateinit var projectLoader: ProjectLoader<ProjectType.Private>

    private fun acceptProject(privateProjectJson: PrivateProjectJson) =
            projectSnapshotRelay.accept(FactoryLoaderTest.ValueTestSnapshot(privateProjectJson, projectKey.key))

    class EmissionTester<T : Any>(
            name: String,
            compositeDisposable: CompositeDisposable,
            source: Observable<T>
    ) {

        private val handlers = mutableListOf<(T) -> Unit>()

        constructor(
                name: String,
                compositeDisposable: CompositeDisposable,
                source: Single<T>
        ) : this(
                name,
                compositeDisposable,
                source.toObservable()
        )

        init {
            compositeDisposable += source.subscribe {
                try {
                    handlers.first().invoke(it)
                    handlers.removeFirst()
                } catch (exception: Exception) {
                    throw EmissionException(name, it, exception)
                }
            }
        }

        fun addHandler(handler: (T) -> Unit) {
            handlers += handler
        }

        fun checkEmpty() = assertTrue(handlers.isEmpty())

        fun checkNotEmpty() = assertTrue(handlers.isNotEmpty())
    }

    private class EmissionException(
            name: String,
            value: Any,
            exception: Exception
    ) : Exception("name: $name, value: $value", exception)

    private lateinit var initialProjectEmissionTester: EmissionTester<ProjectLoader.InitialProjectEvent<ProjectType.Private>>
    private lateinit var addTaskEmissionTester: EmissionTester<ProjectLoader.AddTaskEvent<ProjectType.Private>>
    private lateinit var changeInstancesEmissionTester: EmissionTester<ProjectLoader.ChangeInstancesEvent<ProjectType.Private>>
    private lateinit var changeProjectEmissionTester: EmissionTester<ProjectLoader.ChangeProjectEvent<ProjectType.Private>>

    private val projectKey = ProjectKey.Private("userKey")

    class RxErrorChecker {

        private val errors = mutableListOf<Throwable>()

        init {
            RxJavaPlugins.setErrorHandler {
                it.printStackTrace()
                errors.add(it)
            }
        }

        fun check() = assertTrue(errors.isEmpty())
    }

    @Before
    fun before() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns projectKey.key

        rxErrorChecker = RxErrorChecker()

        projectSnapshotRelay = BehaviorRelay.create()
        projectProvider = TestProjectProvider()

        projectLoader = ProjectLoader(
                projectSnapshotRelay,
                compositeDisposable,
                projectProvider,
                AndroidPrivateProjectManager(UserInfo("email", "name"), projectProvider.database)
        )

        initialProjectEmissionTester = EmissionTester("initialProject", compositeDisposable, projectLoader.initialProjectEvent)
        addTaskEmissionTester = EmissionTester("addTask", compositeDisposable, projectLoader.addTaskEvents)
        changeInstancesEmissionTester = EmissionTester("changeInstances", compositeDisposable, projectLoader.changeInstancesEvents)
        changeProjectEmissionTester = EmissionTester("changeProject", compositeDisposable, projectLoader.changeProjectEvents)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        initialProjectEmissionTester.checkEmpty()
        addTaskEmissionTester.checkEmpty()
        changeInstancesEmissionTester.checkEmpty()
        changeProjectEmissionTester.checkEmpty()

        rxErrorChecker.check()
    }

    @Test
    fun testInitial() {
        assertNull(projectLoader.initialProjectEvent.tryGetCurrentValue())
    }

    @Test
    fun testEmptyProject() {
        initialProjectEmissionTester.addHandler { }

        acceptProject(PrivateProjectJson())
    }

    @Test
    fun testSingleTask() {
        val taskId = "taskKey"

        initialProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
    }

    @Test
    fun testSingleTaskRepeat() {
        val taskId = "taskKey"

        initialProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        initialProjectEmissionTester.checkEmpty()

        changeProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task changed"))))
        changeProjectEmissionTester.checkEmpty()
    }

    @Test
    fun testSingleTaskAddTask() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to TaskJson("task1"))))
        initialProjectEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())
        initialProjectEmissionTester.checkEmpty()

        addTaskEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                        taskId1 to TaskJson("task1"),
                        taskId2 to TaskJson("task2")
        )))
        addTaskEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        addTaskEmissionTester.checkEmpty()
    }

    @Test
    fun testSingleTaskAddTaskEmitInstances() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId1 to TaskJson("task1"))))
        initialProjectEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf())
        initialProjectEmissionTester.checkEmpty()

        addTaskEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(
                        taskId1 to TaskJson("task1"),
                        taskId2 to TaskJson("task2")
        )))
        addTaskEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId2, mapOf())
        addTaskEmissionTester.checkEmpty()

        changeInstancesEmissionTester.addHandler { }
        projectProvider.acceptInstance(projectKey.key, taskId1, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionTester.checkEmpty()

        changeInstancesEmissionTester.addHandler { }
        projectProvider.acceptInstance(projectKey.key, taskId2, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionTester.checkEmpty()
    }

    @Test
    fun testSingleTaskRemoveTask() {
        val taskId = "taskKey"

        initialProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        initialProjectEmissionTester.checkEmpty()

        changeProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson())
        changeProjectEmissionTester.checkEmpty()

        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
    }

    @Test
    fun testSingleTaskEmitInstancesChangeTaskEmitInstances() {
        val taskId = "taskKey"

        initialProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task"))))
        initialProjectEmissionTester.checkNotEmpty()
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf())
        initialProjectEmissionTester.checkEmpty()

        changeInstancesEmissionTester.addHandler { }
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionTester.checkEmpty()

        changeProjectEmissionTester.addHandler { }
        acceptProject(PrivateProjectJson(tasks = mutableMapOf(taskId to TaskJson("task change"))))
        changeProjectEmissionTester.checkEmpty()

        changeInstancesEmissionTester.addHandler { }
        projectProvider.acceptInstance(projectKey.key, taskId, mapOf("2020-03-28" to mapOf("21:06" to InstanceJson())))
        changeInstancesEmissionTester.checkEmpty()
    }
}