package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class SharedProjectsLoaderTest {

    private class TestSharedProjectsProvider : SharedProjectsProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<FactoryProvider.Database.Snapshot>>()

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<FactoryProvider.Database.Snapshot> {
            if (!sharedProjectObservables.containsKey(projectKey))
                sharedProjectObservables[projectKey] = PublishRelay.create()
            return sharedProjectObservables.getValue(projectKey)
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: ProjectLoaderTest.RxErrorChecker

    private lateinit var sharedProjectKeysRelay: BehaviorRelay<Set<ProjectKey.Shared>>
    private lateinit var sharedProjectsProvider: TestSharedProjectsProvider
    private lateinit var sharedProjectManager: AndroidSharedProjectManager
    private lateinit var sharedProjectsLoader: SharedProjectsLoader

    private lateinit var initialProjectsEmissionTester: ProjectLoaderTest.EmissionTester<SharedProjectsLoader.InitialProjectsEvent>
    private lateinit var addProjectEmissionTester: ProjectLoaderTest.EmissionTester<ProjectLoader.InitialProjectEvent<ProjectType.Shared>>
    private lateinit var addTaskEmissionTester: ProjectLoaderTest.EmissionTester<ProjectLoader.AddTaskEvent<ProjectType.Shared>>
    private lateinit var changeInstancesEmissionTester: ProjectLoaderTest.EmissionTester<ProjectLoader.ChangeInstancesEvent<ProjectType.Shared>>
    private lateinit var changeProjectEmissionTester: ProjectLoaderTest.EmissionTester<ProjectLoader.ChangeProjectEvent<ProjectType.Shared>>

    @Before
    fun before() {
        rxErrorChecker = ProjectLoaderTest.RxErrorChecker()

        sharedProjectKeysRelay = BehaviorRelay.create()
        sharedProjectsProvider = TestSharedProjectsProvider()
        sharedProjectManager = AndroidSharedProjectManager(listOf(), sharedProjectsProvider.projectProvider.database)

        sharedProjectsLoader = SharedProjectsLoader(
                sharedProjectKeysRelay,
                sharedProjectManager,
                compositeDisposable,
                sharedProjectsProvider
        )

        initialProjectsEmissionTester = ProjectLoaderTest.EmissionTester("initialProjects", compositeDisposable, sharedProjectsLoader.initialProjectsEvent)
        addProjectEmissionTester = ProjectLoaderTest.EmissionTester("addProject", compositeDisposable, sharedProjectsLoader.addProjectEvents)
        addTaskEmissionTester = ProjectLoaderTest.EmissionTester("addTask", compositeDisposable, sharedProjectsLoader.addTaskEvents)
        changeInstancesEmissionTester = ProjectLoaderTest.EmissionTester("changeInstances", compositeDisposable, sharedProjectsLoader.changeInstancesEvents)
        changeProjectEmissionTester = ProjectLoaderTest.EmissionTester("changeProject", compositeDisposable, sharedProjectsLoader.changeProjectEvents)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        initialProjectsEmissionTester.checkEmpty()
        addProjectEmissionTester.checkEmpty()
        addTaskEmissionTester.checkEmpty()
        changeInstancesEmissionTester.checkEmpty()
        changeProjectEmissionTester.checkEmpty()

        rxErrorChecker.check()
    }

    @Test
    fun testInitial() {
        assertNull(sharedProjectsLoader.initialProjectsEvent.tryGetCurrentValue())
    }
}