package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.utils.SingleParamSingleSource
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Test

class RecursiveTaskRecordLoaderTest {

    companion object {

        val taskKey1 = TaskKey.Root("taskKey1")
        val taskKey2 = TaskKey.Root("taskKey2")
        val taskKey3 = TaskKey.Root("taskKey3")
        val taskKey4 = TaskKey.Root("taskKey4")
        val taskKey5 = TaskKey.Root("taskKey5")
    }

    private class TestTaskRecordLoader : RecursiveTaskRecordLoader.TaskRecordLoader {

        val singleParamSingleSource = SingleParamSingleSource<TaskKey.Root, RootTaskRecord>()

        override fun getTaskRecordSingle(taskKey: TaskKey.Root) = singleParamSingleSource.getSingle(taskKey)

        override fun tryGetTaskRecord(taskKey: TaskKey.Root): RootTaskRecord? {
            TODO("Not yet implemented")
        }
    }

    private class TestUserCustomTimeProviderSource : UserCustomTimeProviderSource {

        val singleParamSingleSource = SingleParamSingleSource<RootTaskRecord, JsonTime.UserCustomTimeProvider>()

        override fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>): Single<JsonTime.UserCustomTimeProvider> {
            TODO("Not yet implemented")
        }

        override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) =
                singleParamSingleSource.getSingle(rootTaskRecord)

        override fun hasCustomTimes(rootTaskRecord: RootTaskRecord): Boolean {
            TODO("Not yet implemented")
        }

        override fun getTimeChangeObservable(): Observable<Unit> {
            TODO("Not yet implemented")
        }
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var taskRecordLoader: TestTaskRecordLoader
    private lateinit var rootTaskUserCustomTimeProviderSource: TestUserCustomTimeProviderSource
    private lateinit var recursiveTaskRecordLoader: RecursiveTaskRecordLoader

    private lateinit var testObserver: TestObserver<Void>

    private fun initLoader(initialTaskRecord: RootTaskRecord) {
        taskRecordLoader = TestTaskRecordLoader()
        rootTaskUserCustomTimeProviderSource = TestUserCustomTimeProviderSource()

        recursiveTaskRecordLoader = RecursiveTaskRecordLoader(
                initialTaskRecord,
                taskRecordLoader,
                rootTaskUserCustomTimeProviderSource,
                domainDisposable,
        )

        testObserver = recursiveTaskRecordLoader.completable.test()
    }

    private fun mockTaskRecord(
            taskKey: TaskKey.Root,
            dependentTaskKeys: Set<TaskKey.Root> = emptySet(),
    ) = mockk<RootTaskRecord> {
        every { this@mockk.taskKey } returns taskKey

        every { getDependentTaskKeys() } returns dependentTaskKeys
    }

    private fun acceptTime(taskRecord: RootTaskRecord) =
            rootTaskUserCustomTimeProviderSource.singleParamSingleSource.accept(taskRecord, mockk())

    private fun acceptTask(taskKey: TaskKey.Root, taskRecord: RootTaskRecord) =
            taskRecordLoader.singleParamSingleSource.accept(taskKey, taskRecord)

    @After
    fun after() {
        domainDisposable.dispose()
    }

    @Test
    fun testNoChildrenCompletesImmediately() {
        val initialTaskRecord = mockTaskRecord(taskKey1)

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertComplete()
    }

    @Test
    fun testOneImmediateChild() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2)

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertComplete()
    }

    @Test
    fun testOneImmediateChildTimesLast() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2)

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertComplete()
    }

    @Test
    fun testTwoImmediateChildren() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2, taskKey3))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2)

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertNotComplete()

        val taskRecord3 = mockTaskRecord(taskKey3)

        acceptTask(taskKey3, taskRecord3)
        testObserver.assertNotComplete()

        acceptTime(taskRecord3)
        testObserver.assertComplete()
    }

    @Test
    fun testTwoImmediateChildrenTimesLast() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2, taskKey3))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2)

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        val taskRecord3 = mockTaskRecord(taskKey3)

        acceptTask(taskKey3, taskRecord3)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord3)
        testObserver.assertComplete()
    }

    @Test
    fun testChildAndParent() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2, taskKey3))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2)

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertNotComplete()

        val taskRecord3 = mockTaskRecord(taskKey3)

        acceptTask(taskKey3, taskRecord3)
        testObserver.assertNotComplete()

        acceptTime(taskRecord3)
        testObserver.assertComplete()
    }

    @Test
    fun testOneChildOneSubchild() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2, dependentTaskKeys = setOf(taskKey3))

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertNotComplete()

        val taskRecord3 = mockTaskRecord(taskKey3)

        acceptTask(taskKey3, taskRecord3)
        testObserver.assertNotComplete()

        acceptTime(taskRecord3)
        testObserver.assertComplete()
    }

    @Test
    fun testTwoChildrenOneSubchild() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2, taskKey3))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        // feed first child
        val taskRecord2 = mockTaskRecord(taskKey2, dependentTaskKeys = setOf(taskKey4))

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertNotComplete()

        // feed second child
        val taskRecord3 = mockTaskRecord(taskKey3, dependentTaskKeys = setOf(taskKey5))

        acceptTask(taskKey3, taskRecord3)
        testObserver.assertNotComplete()

        acceptTime(taskRecord3)
        testObserver.assertNotComplete()

        // feed first subchild
        val taskRecord4 = mockTaskRecord(taskKey4)

        acceptTask(taskKey4, taskRecord4)
        testObserver.assertNotComplete()

        acceptTime(taskRecord4)
        testObserver.assertNotComplete()

        // feed second subchild
        val taskRecord5 = mockTaskRecord(taskKey5)

        acceptTask(taskKey5, taskRecord5)
        testObserver.assertNotComplete()

        acceptTime(taskRecord5)
        testObserver.assertComplete()
    }

    @Test
    fun testOneChildLoop() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2, dependentTaskKeys = setOf(taskKey1))

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertComplete()
    }

    @Test
    fun testOneChildLoopParent() {
        val initialTaskRecord = mockTaskRecord(taskKey1, dependentTaskKeys = setOf(taskKey2))

        initLoader(initialTaskRecord)
        testObserver.assertNotComplete()

        acceptTime(initialTaskRecord)
        testObserver.assertNotComplete()

        val taskRecord2 = mockTaskRecord(taskKey2, dependentTaskKeys = setOf(taskKey1))

        acceptTask(taskKey2, taskRecord2)
        testObserver.assertNotComplete()

        acceptTime(taskRecord2)
        testObserver.assertComplete()
    }
}