package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test

class RootTaskKeySourceTest {

    companion object {

        private val projectKey1 = ProjectKey.Private("projectKey1")
        private val projectKey2 = ProjectKey.Private("projectKey2")

        private val rootTaskKey1 = TaskKey.Root("rootTaskKey1")
        private val rootTaskKey2 = TaskKey.Root("rootTaskKey2")
        private val rootTaskKey3 = TaskKey.Root("rootTaskKey3")
        private val rootTaskKey4 = TaskKey.Root("rootTaskKey4")
        private val rootTaskKey5 = TaskKey.Root("rootTaskKey5")
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var rootTaskKeySource: RootTaskKeySource
    private lateinit var testObserver: TestObserver<Map<TaskKey.Root, ProjectKey<*>>>

    @Before
    fun before() {
        rootTaskKeySource = RootTaskKeySource(domainDisposable)
        testObserver = rootTaskKeySource.rootTaskKeysObservable.test()
    }

    @After
    fun after() {
        domainDisposable.clear()
    }

    @Test
    fun testInitial() {
        testObserver.assertEmpty()
    }

    @Test
    fun testAddProject() {
        rootTaskKeySource.onProjectAddedOrUpdated(projectKey1, setOf(rootTaskKey1, rootTaskKey2))
        testObserver.assertValue(mapOf(rootTaskKey1 to projectKey1, rootTaskKey2 to projectKey1))
    }

    @Test
    fun testAddSecondProject() {
        rootTaskKeySource.onProjectAddedOrUpdated(projectKey1, setOf(rootTaskKey1, rootTaskKey2))
        testObserver.assertValue(mapOf(rootTaskKey1 to projectKey1, rootTaskKey2 to projectKey1))

        rootTaskKeySource.onProjectAddedOrUpdated(projectKey2, setOf(rootTaskKey3, rootTaskKey4))
        testObserver.assertValueAt(
                1,
                mapOf(
                        rootTaskKey1 to projectKey1,
                        rootTaskKey2 to projectKey1,
                        rootTaskKey3 to projectKey2,
                        rootTaskKey4 to projectKey2,
                ),
        )
    }

    @Test
    fun testRemoveProject() {
        testAddSecondProject()

        rootTaskKeySource.onProjectsRemoved(setOf(projectKey2))
        testObserver.assertValueAt(2, mapOf(rootTaskKey1 to projectKey1, rootTaskKey2 to projectKey1))
    }

    @Test
    fun tesUpdateProject() {
        testAddSecondProject()

        rootTaskKeySource.onProjectAddedOrUpdated(projectKey2, setOf(rootTaskKey4, rootTaskKey5))
        testObserver.assertValueAt(
                2,
                mapOf(
                        rootTaskKey1 to projectKey1,
                        rootTaskKey2 to projectKey1,
                        rootTaskKey4 to projectKey2,
                        rootTaskKey5 to projectKey2,
                ),
        )
    }
}