package com.krystianwsul.checkme.firebase.loaders

import android.util.Base64
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.firebase.TestUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalStdlibApi
class ProjectLoaderTest {

    class TestProjectProvider : ProjectProvider {

        override val database = object : DatabaseWrapper() {

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }
    }

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val compositeDisposable = CompositeDisposable()

    private lateinit var projectSnapshotRelay: BehaviorRelay<Snapshot<PrivateOwnedProjectJson>>
    private lateinit var projectManager: AndroidPrivateProjectManager
    private lateinit var projectLoader: ProjectLoader<ProjectType.Private, PrivateOwnedProjectJson, PrivateOwnedProjectRecord>

    private fun acceptProject(privateProjectJson: PrivateOwnedProjectJson) =
        projectSnapshotRelay.accept(Snapshot(projectKey.key, privateProjectJson))

    private lateinit var initialProjectEmissionChecker: EmissionChecker<ChangeWrapper<ProjectLoader.InitialProjectEvent<PrivateOwnedProjectRecord>>>
    private lateinit var changeProjectEmissionChecker: EmissionChecker<ProjectLoader.ChangeProjectEvent<PrivateOwnedProjectRecord>>

    private val projectKey = ProjectKey.Private("userKey")

    @Before
    fun before() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns projectKey.key

        projectSnapshotRelay = BehaviorRelay.create()

        val userInfo = UserInfo("email", "name", "uid")

        projectManager = AndroidPrivateProjectManager(userInfo)

        projectLoader = ProjectLoader.Impl(
            userInfo.key.toPrivateProjectKey(),
            projectSnapshotRelay,
            compositeDisposable,
            projectManager,
            null,
            TestUserCustomTimeProviderSource(),
            mockk(relaxed = true),
        )

        initialProjectEmissionChecker =
            EmissionChecker("initialProject", compositeDisposable, projectLoader.initialProjectEvent)

        changeProjectEmissionChecker =
            EmissionChecker("changeProject", compositeDisposable, projectLoader.changeProjectEvents)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        initialProjectEmissionChecker.checkEmpty()
        changeProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testInitial() {
        assertNull(projectLoader.initialProjectEvent.tryGetCurrentValue())
    }

    @Test
    fun testEmptyProject() {
        initialProjectEmissionChecker.checkRemote { acceptProject(PrivateOwnedProjectJson()) }
    }

    @Test
    fun testSingleTask() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }
    }

    @Test
    fun testSingleTaskRepeat() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkOne {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task changed"))))
        }
    }

    @Test
    fun testSingleTaskAddTask() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId1 to PrivateTaskJson("task1"))))
        }

        changeProjectEmissionChecker.checkOne {
            acceptProject(
                PrivateOwnedProjectJson(
                    tasks = mutableMapOf(
                        taskId1 to PrivateTaskJson("task1"),
                        taskId2 to PrivateTaskJson("task2")
                    )
                )
            )
        }
    }

    @Test
    fun testSingleTaskAddTaskEmitInstances() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId1 to PrivateTaskJson("task1"))))
        }

        changeProjectEmissionChecker.checkOne {
            acceptProject(
                PrivateOwnedProjectJson(
                    tasks = mutableMapOf(
                        taskId1 to PrivateTaskJson("task1"),
                        taskId2 to PrivateTaskJson("task2"),
                    )
                )
            )
        }
    }

    @Test
    fun testSingleTaskRemoveTask() {
        val taskId = "taskKey"


        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkOne { acceptProject(PrivateOwnedProjectJson()) }
    }

    @Test
    fun testSingleTaskEmitInstancesChangeTaskEmitInstances() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkOne {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task change"))))
        }
    }

    @Test
    fun testChangeEmptyProject() {
        initialProjectEmissionChecker.checkRemote { acceptProject(PrivateOwnedProjectJson()) }

        changeProjectEmissionChecker.checkOne { acceptProject(PrivateOwnedProjectJson(defaultTimesCreated = true)) }
    }

    @Test
    fun testChangeEmptyTask() {
        val taskId = "taskKey"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task"))))
        }

        changeProjectEmissionChecker.checkOne {
            acceptProject(PrivateOwnedProjectJson(tasks = mutableMapOf(taskId to PrivateTaskJson("task change"))))
        }
    }

    @Test
    fun testChangeTwoEmptyTasks() {
        val taskId1 = "taskKey1"
        val taskId2 = "taskKey2"

        initialProjectEmissionChecker.checkRemote {
            acceptProject(
                PrivateOwnedProjectJson(
                    tasks = mutableMapOf(
                        taskId1 to PrivateTaskJson("task1"),
                        taskId2 to PrivateTaskJson("task2"),
                    )
                )
            )
        }

        changeProjectEmissionChecker.checkOne {
            acceptProject(
                PrivateOwnedProjectJson(
                    tasks = mutableMapOf(
                        taskId1 to PrivateTaskJson("task1 change"),
                        taskId2 to PrivateTaskJson("task2 change"),
                    )
                )
            )
        }
    }

    @Test
    fun testLocalEvent() {
        ErrorLogger.instance = mockk(relaxed = true)

        initialProjectEmissionChecker.checkRemote { acceptProject(PrivateOwnedProjectJson()) }

        projectManager.value.single().defaultTimesCreated = true
        projectManager.save(mockk(relaxed = true))

        // doesn't emit ChangeType.LOCAL
        acceptProject(PrivateOwnedProjectJson(defaultTimesCreated = true))
    }
}