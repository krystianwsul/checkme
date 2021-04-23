package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.project.ProjectRecord
import io.reactivex.rxjava3.core.Completable

interface ProjectToRootTaskCoordinator {

    fun getRootTasks(projectRecord: ProjectRecord<*>): Completable

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            private val rootTaskFactory: RootTaskFactory,
    ) : ProjectToRootTaskCoordinator {

        override fun getRootTasks(projectRecord: ProjectRecord<*>): Completable {
            rootTaskKeySource.onProjectAddedOrUpdated(
                    projectRecord.projectKey,
                    projectRecord.rootTaskParentDelegate.rootTaskKeys,
            )

            return Completable.complete() // todo task fetch return after tasks are loaded

            /**
             * This has to complete when the whole tree of task models has finished loading.  The ChangeType.Remote
             * that will emit after the project model is initialized will be the point at which root tasks are
             * first accessed (even if the objects are present earlier, they won't be queried), so all tasks have to
             * be initialized and ready to go (recursively) before this completes.
             */
        }
    }
}