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
        private val projectKey3 = ProjectKey.Private("projectKey3")

        private val rootTaskKey1 = TaskKey.Root("rootTaskKey1")
        private val rootTaskKey2 = TaskKey.Root("rootTaskKey2")
        private val rootTaskKey3 = TaskKey.Root("rootTaskKey3")
        private val rootTaskKey4 = TaskKey.Root("rootTaskKey4")
        private val rootTaskKey5 = TaskKey.Root("rootTaskKey5")
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var rootTaskKeySource: RootTaskKeySource
    private lateinit var testObserver: TestObserver<Set<TaskKey.Root>>

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
        testObserver.assertValue(setOf(rootTaskKey1, rootTaskKey2))
    }

    @Test
    fun testAddOverlappingProjects() {
        rootTaskKeySource.onProjectAddedOrUpdated(projectKey1, setOf(rootTaskKey1, rootTaskKey2))
        testObserver.assertValue(setOf(rootTaskKey1, rootTaskKey2))

        rootTaskKeySource.onProjectAddedOrUpdated(projectKey2, setOf(rootTaskKey2, rootTaskKey3))
        testObserver.assertValueAt(1, setOf(rootTaskKey1, rootTaskKey2, rootTaskKey3))
    }

    @Test
    fun testRemoveProject() {
        testAddOverlappingProjects()

        rootTaskKeySource.onProjectsRemoved(setOf(projectKey2))
        testObserver.assertValueAt(2, setOf(rootTaskKey1, rootTaskKey2))
    }

    @Test
    fun tesUpdateProject() {
        testAddOverlappingProjects()

        rootTaskKeySource.onProjectAddedOrUpdated(projectKey2, setOf(rootTaskKey3, rootTaskKey4))
        testObserver.assertValueAt(2, setOf(rootTaskKey1, rootTaskKey2, rootTaskKey3, rootTaskKey4))
    }
}