package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.utils.SingleParamSingleSource
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.Test

class RecursiveTaskRecordLoaderTest {

    companion object {

        val taskKey1 = TaskKey.Root("taskKey1")
    }

    private class TestTaskRecordLoader : RecursiveTaskRecordLoader.TaskRecordLoader {

        val singleParamSingleSource = SingleParamSingleSource<TaskKey.Root, RootTaskRecord>()

        override fun getTaskRecordSingle(taskKey: TaskKey.Root) = singleParamSingleSource.getSingle(taskKey)
    }

    private class TestRootTaskUserCustomTimeProviderSource : RootTaskUserCustomTimeProviderSource {

        val singleParamSingleSource = SingleParamSingleSource<RootTaskRecord, JsonTime.UserCustomTimeProvider>()

        override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord) =
                singleParamSingleSource.getSingle(rootTaskRecord)
    }

    private lateinit var taskRecordLoader: TestTaskRecordLoader
    private lateinit var rootTaskUserCustomTimeProviderSource: TestRootTaskUserCustomTimeProviderSource
    private lateinit var recursiveTaskRecordLoader: RecursiveTaskRecordLoader

    private lateinit var testObserver: TestObserver<Void>

    private fun initLoader(initialTaskRecord: RootTaskRecord) {
        taskRecordLoader = TestTaskRecordLoader()
        rootTaskUserCustomTimeProviderSource = TestRootTaskUserCustomTimeProviderSource()

        recursiveTaskRecordLoader = RecursiveTaskRecordLoader(
                initialTaskRecord,
                taskRecordLoader,
                rootTaskUserCustomTimeProviderSource,
        )

        testObserver = recursiveTaskRecordLoader.completable.test()
    }

    private fun mockTaskRecord(taskKey: TaskKey.Root) = mockk<RootTaskRecord> {
        every { this@mockk.taskKey } returns taskKey
        every { taskHierarchyRecords } returns mutableMapOf()

        every { rootTaskParentDelegate } returns mockk {
            every { rootTaskKeys } returns setOf()
        }
    }

    @Test
    fun testNoChildrenOrTimesCompletesImmediately() {
        val initialTaskRecord = mockTaskRecord(taskKey1)

        initLoader(initialTaskRecord)

        testObserver.assertNotComplete()

        rootTaskUserCustomTimeProviderSource.singleParamSingleSource.accept(initialTaskRecord, mockk())

        testObserver.assertComplete()
    }
}