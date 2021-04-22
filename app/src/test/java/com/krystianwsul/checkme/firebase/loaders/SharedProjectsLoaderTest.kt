package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.firebase.TestUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.roottask.RootTaskCoordinator
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalStdlibApi
class SharedProjectsLoaderTest {

    class TestProjectProvider : ProjectProvider {

        override val database = object : DatabaseWrapper() {

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }
    }

    private class TestSharedProjectsProvider : SharedProjectsProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<Snapshot<JsonWrapper>>>()

        override val projectProvider = TestProjectProvider()

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot<JsonWrapper>> {
            if (!sharedProjectObservables.containsKey(projectKey))
                sharedProjectObservables[projectKey] = PublishRelay.create()
            return sharedProjectObservables.getValue(projectKey)
        }

        fun acceptProject(
                projectKey: ProjectKey.Shared,
                projectJson: SharedProjectJson,
        ) {
            sharedProjectObservables.getValue(projectKey).accept(Snapshot(
                    projectKey.key,
                    JsonWrapper(projectJson),
            ))
        }
    }

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var projectKeysRelay: PublishRelay<ChangeWrapper<Set<ProjectKey.Shared>>>
    private lateinit var sharedProjectsProvider: TestSharedProjectsProvider
    private lateinit var projectManager: AndroidSharedProjectManager

    private lateinit var sharedProjectsLoader: SharedProjectsLoader

    private lateinit var initialProjectsEmissionChecker: EmissionChecker<SharedProjectsLoader.InitialProjectsEvent>
    private lateinit var addProjectEmissionChecker: EmissionChecker<ChangeWrapper<SharedProjectsLoader.AddProjectEvent>>
    private lateinit var removeProjectsEmissionChecker: EmissionChecker<ChangeWrapper<SharedProjectsLoader.RemoveProjectsEvent>>

    @Before
    fun before() {
        rxErrorChecker = RxErrorChecker()

        projectKeysRelay = PublishRelay.create()
        sharedProjectsProvider = TestSharedProjectsProvider()

        projectManager = AndroidSharedProjectManager(sharedProjectsProvider.projectProvider.database)

        sharedProjectsLoader = SharedProjectsLoader.Impl(
                projectKeysRelay,
                projectManager,
                compositeDisposable,
                sharedProjectsProvider,
                TestUserCustomTimeProviderSource(),
                mockk(relaxed = true),
                object : RootTaskCoordinator {

                    override fun getRootTasks(projectRecord: ProjectRecord<*>) = Completable.complete() // todo task tests
                },
                mockk(relaxed = true), // todo task tests
        )

        initialProjectsEmissionChecker =
                EmissionChecker("initialProjects", compositeDisposable, sharedProjectsLoader.initialProjectsEvent)

        addProjectEmissionChecker =
                EmissionChecker("addProject", compositeDisposable, sharedProjectsLoader.addProjectEvents)

        removeProjectsEmissionChecker =
                EmissionChecker("removeProjects", compositeDisposable, sharedProjectsLoader.removeProjectEvents)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        initialProjectsEmissionChecker.checkEmpty()
        addProjectEmissionChecker.checkEmpty()
        removeProjectsEmissionChecker.checkEmpty()

        rxErrorChecker.check()
    }

    private val projectKey1 = ProjectKey.Shared("projectKey1")
    private val projectKey2 = ProjectKey.Shared("projectKey2")

    @Test
    fun testInitialEmpty() {
        initialProjectsEmissionChecker.checkOne {
            projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))
        }
    }

    @Test
    fun testInitialProject() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        }
    }

    @Test
    fun testInitialEmptyAdd() {
        initialProjectsEmissionChecker.checkOne {
            projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))
        }

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        }
    }

    @Test
    fun testInitialProjectAdd() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        }

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1, projectKey2)))

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey2, SharedProjectJson())
        }
    }

    @Test
    fun testInitialProjectRemove() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        }

        removeProjectsEmissionChecker.checkRemote {
            projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))
        }
    }

    @Test
    fun testInitialProjectSwap1() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        }

        removeProjectsEmissionChecker.checkRemote {
            projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey2)))
        }

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey2, SharedProjectJson())
        }
    }

    @Test
    fun testInitialProjectSwap2() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        }

        removeProjectsEmissionChecker.checkRemote {
            projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey2)))
        }

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey2, SharedProjectJson())
        }
    }
}