package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.dependencies.RootTaskKeyStore
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.Before
import org.junit.Test

class RootTaskKeyStoreTest {

    companion object {

        private val projectKey1 = ProjectKey.Private("projectKey1")
        private val projectKey2 = ProjectKey.Private("projectKey2")

        private val rootTaskKey1 = TaskKey.Root("rootTaskKey1")
        private val rootTaskKey2 = TaskKey.Root("rootTaskKey2")
        private val rootTaskKey3 = TaskKey.Root("rootTaskKey3")
        private val rootTaskKey4 = TaskKey.Root("rootTaskKey4")
        private val rootTaskKey5 = TaskKey.Root("rootTaskKey5")
    }

    private lateinit var rootTaskKeyStore: RootTaskKeyStore
    private lateinit var testObserver: TestObserver<Set<TaskKey.Root>>

    @Before
    fun before() {
        rootTaskKeyStore = RootTaskKeyStore()
        testObserver = rootTaskKeyStore.rootTaskKeysObservable.test()
    }

    @Test
    fun testInitial() {
        testObserver.assertValue(emptySet())
    }

    @Test
    fun testAddProject() {
        rootTaskKeyStore.onProjectAddedOrUpdated(projectKey1, setOf(rootTaskKey1, rootTaskKey2))
        testObserver.assertValueAt(1, setOf(rootTaskKey1, rootTaskKey2))
    }

    @Test
    fun testAddSecondProject() {
        rootTaskKeyStore.onProjectAddedOrUpdated(projectKey1, setOf(rootTaskKey1, rootTaskKey2))
        testObserver.assertValueAt(1, setOf(rootTaskKey1, rootTaskKey2))

        rootTaskKeyStore.onProjectAddedOrUpdated(projectKey2, setOf(rootTaskKey3, rootTaskKey4))
        testObserver.assertValueAt(2, setOf(rootTaskKey1, rootTaskKey2, rootTaskKey3, rootTaskKey4))
    }

    @Test
    fun testRemoveProject() {
        testAddSecondProject()

        rootTaskKeyStore.onProjectsRemoved(setOf(projectKey2))
        testObserver.assertValueAt(3, setOf(rootTaskKey1, rootTaskKey2))
    }

    @Test
    fun tesUpdateProject() {
        testAddSecondProject()

        rootTaskKeyStore.onProjectAddedOrUpdated(projectKey2, setOf(rootTaskKey4, rootTaskKey5))
        testObserver.assertValueAt(3, setOf(rootTaskKey1, rootTaskKey2, rootTaskKey4, rootTaskKey5))
    }
}