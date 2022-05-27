package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.firebase.TestUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.projects.ProjectsLoader
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.SharedOwnedProjectJson
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.mockk.mockk
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

        override fun getProjectObservable(projectKey: ProjectKey<ProjectType.Shared>): Observable<Snapshot<JsonWrapper>> {
            if (!sharedProjectObservables.containsKey(projectKey as ProjectKey.Shared))
                sharedProjectObservables[projectKey] = PublishRelay.create()
            return sharedProjectObservables.getValue(projectKey)
        }

        fun acceptProject(
            projectKey: ProjectKey.Shared,
            projectJson: SharedOwnedProjectJson,
        ) {
            sharedProjectObservables.getValue(projectKey).accept(
                Snapshot(
                    projectKey.key,
                    JsonWrapper(projectJson),
            ))
        }
    }

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var projectKeysRelay: PublishRelay<Set<ProjectKey.Shared>>
    private lateinit var sharedProjectsProvider: TestSharedProjectsProvider
    private lateinit var projectManager: AndroidSharedProjectManager

    private lateinit var sharedProjectsLoader: SharedProjectsLoader

    private lateinit var initialProjectsEmissionChecker: EmissionChecker<ProjectsLoader.InitialProjectsEvent<ProjectType.Shared, JsonWrapper, SharedOwnedProjectRecord>>
    private lateinit var addProjectEmissionChecker: EmissionChecker<ChangeWrapper<ProjectsLoader.AddProjectEvent<ProjectType.Shared, JsonWrapper, SharedOwnedProjectRecord>>>
    private lateinit var removeProjectsEmissionChecker: EmissionChecker<ProjectsLoader.RemoveProjectsEvent<ProjectType.Shared>>

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
            mockk(relaxed = true),
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
        initialProjectsEmissionChecker.checkOne { projectKeysRelay.accept(setOf()) }
    }

    @Test
    fun testInitialProject() {
        projectKeysRelay.accept(setOf(projectKey1))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedOwnedProjectJson())
        }
    }

    @Test
    fun testInitialEmptyAdd() {
        initialProjectsEmissionChecker.checkOne { projectKeysRelay.accept(setOf()) }

        projectKeysRelay.accept(setOf(projectKey1))

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey1, SharedOwnedProjectJson())
        }
    }

    @Test
    fun testInitialProjectAdd() {
        projectKeysRelay.accept(setOf(projectKey1))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedOwnedProjectJson())
        }

        projectKeysRelay.accept(setOf(projectKey1, projectKey2))

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey2, SharedOwnedProjectJson())
        }
    }

    @Test
    fun testInitialProjectRemove() {
        projectKeysRelay.accept(setOf(projectKey1))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedOwnedProjectJson())
        }

        removeProjectsEmissionChecker.checkOne { projectKeysRelay.accept(setOf()) }
    }

    @Test
    fun testInitialProjectSwap1() {
        projectKeysRelay.accept(setOf(projectKey1))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedOwnedProjectJson())
        }

        removeProjectsEmissionChecker.checkOne { projectKeysRelay.accept(setOf(projectKey2)) }

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey2, SharedOwnedProjectJson())
        }
    }

    @Test
    fun testInitialProjectSwap2() {
        projectKeysRelay.accept(setOf(projectKey1))

        initialProjectsEmissionChecker.checkOne {
            sharedProjectsProvider.acceptProject(projectKey1, SharedOwnedProjectJson())
        }

        removeProjectsEmissionChecker.checkOne { projectKeysRelay.accept(setOf(projectKey2)) }

        addProjectEmissionChecker.checkRemote {
            sharedProjectsProvider.acceptProject(projectKey2, SharedOwnedProjectJson())
        }
    }
}