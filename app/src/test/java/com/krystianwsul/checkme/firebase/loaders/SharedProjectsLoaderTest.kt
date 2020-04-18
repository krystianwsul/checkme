package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class SharedProjectsLoaderTest {

    private class TestSharedProjectsProvider : SharedProjectsProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<Snapshot>>()

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot> {
            if (!sharedProjectObservables.containsKey(projectKey))
                sharedProjectObservables[projectKey] = PublishRelay.create()
            return sharedProjectObservables.getValue(projectKey)
        }

        fun acceptProject(
                projectKey: ProjectKey.Shared,
                projectJson: SharedProjectJson
        ) {
            sharedProjectObservables.getValue(projectKey).accept(ValueTestSnapshot(
                    JsonWrapper(projectJson),
                    projectKey.key
            ))
        }
    }

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

        sharedProjectsLoader = SharedProjectsLoader(
                projectKeysRelay,
                projectManager,
                compositeDisposable,
                sharedProjectsProvider
        )

        initialProjectsEmissionChecker = EmissionChecker("initialProjects", compositeDisposable, sharedProjectsLoader.initialProjectsEvent)
        addProjectEmissionChecker = EmissionChecker("addProject", compositeDisposable, sharedProjectsLoader.addProjectEvents)
        removeProjectsEmissionChecker = EmissionChecker("removeProjects", compositeDisposable, sharedProjectsLoader.removeProjectEvents)
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
    private val projectKey3 = ProjectKey.Shared("projectKey3")
    private val projectKey4 = ProjectKey.Shared("projectKey4")

    @Test
    fun testInitialEmpty() {
        initialProjectsEmissionChecker.addHandler { }
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))
        initialProjectsEmissionChecker.checkEmpty()
    }

    @Test
    fun testInitialProject() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.addHandler { }
        sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        initialProjectsEmissionChecker.checkEmpty()
    }

    @Test
    fun testInitialEmptyAdd() {
        initialProjectsEmissionChecker.addHandler { }
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))
        initialProjectsEmissionChecker.checkEmpty()

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        addProjectEmissionChecker.checkRemote()
        sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        addProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testInitialProjectAdd() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.addHandler { }
        sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        initialProjectsEmissionChecker.checkEmpty()

        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1, projectKey2)))

        addProjectEmissionChecker.checkRemote()
        sharedProjectsProvider.acceptProject(projectKey2, SharedProjectJson())
        addProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testInitialProjectRemove() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.addHandler { }
        sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        initialProjectsEmissionChecker.checkEmpty()

        removeProjectsEmissionChecker.checkLocal()
        projectKeysRelay.accept(ChangeWrapper(ChangeType.LOCAL, setOf()))
        removeProjectsEmissionChecker.checkEmpty()
    }

    @Test
    fun testInitialProjectSwap1() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.addHandler { }
        sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        initialProjectsEmissionChecker.checkEmpty()

        removeProjectsEmissionChecker.checkLocal()
        projectKeysRelay.accept(ChangeWrapper(ChangeType.LOCAL, setOf(projectKey2)))
        removeProjectsEmissionChecker.checkEmpty()

        addProjectEmissionChecker.checkRemote()
        sharedProjectsProvider.acceptProject(projectKey2, SharedProjectJson())
        addProjectEmissionChecker.checkEmpty()
    }

    @Test
    fun testInitialProjectSwap2() {
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey1)))

        initialProjectsEmissionChecker.addHandler { }
        sharedProjectsProvider.acceptProject(projectKey1, SharedProjectJson())
        initialProjectsEmissionChecker.checkEmpty()

        removeProjectsEmissionChecker.checkRemote()
        projectKeysRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(projectKey2)))
        removeProjectsEmissionChecker.checkEmpty()

        addProjectEmissionChecker.checkRemote()
        sharedProjectsProvider.acceptProject(projectKey2, SharedProjectJson())
        addProjectEmissionChecker.checkEmpty()
    }
}