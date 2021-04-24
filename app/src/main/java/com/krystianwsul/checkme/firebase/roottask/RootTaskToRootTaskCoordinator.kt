package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable

interface RootTaskToRootTaskCoordinator {

    fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable

    class Impl(private val rootTaskKeySource: RootTaskKeySource) : RootTaskToRootTaskCoordinator {

        override fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable {
            val allRelatedTasks: Set<TaskKey.Root> = rootTaskRecord.rootTaskParentDelegate.rootTaskKeys +
                    rootTaskRecord.taskHierarchyRecords.map { TaskKey.Root(it.value.parentTaskId) }

            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, allRelatedTasks)

            /**
             * todo task fetch return after tasks are loaded.
             *
             * Note: this needs to block not only until the immediate children are loaded, but until the entire graph
             * is complete.  Yeah, figuring that out is gonna be fun.
             */

            /**
             * Ho hum.  We need a way to determine which task records have been loaded.  Ultimately, this delay is so
             * that by the time a project is initialized, all the tasks, and all the custom times they use, are all
             * initialized.  That part's something we can check at the TaskFactory level, like how we check for custom
             * times.  (Since the task model objects should be ready before the project model is constructed.)
             *
             * But it's trickier for individual tasks.  Here, we need to wait for all child task records to be ready,
             * and all of their custom times to be ready.
             */

            return Completable.complete()
        }
    }
}