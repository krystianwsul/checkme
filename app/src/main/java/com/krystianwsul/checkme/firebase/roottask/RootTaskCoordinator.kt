package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.project.ProjectRecord
import io.reactivex.rxjava3.core.Completable

interface RootTaskCoordinator {

    fun getRootTasks(projectRecord: ProjectRecord<*>): Completable

    class Impl(private val rootTaskKeySource: RootTaskKeySource) : RootTaskCoordinator {

        override fun getRootTasks(projectRecord: ProjectRecord<*>): Completable {
            rootTaskKeySource.onProjectAddedOrUpdated(projectRecord.projectKey, projectRecord.rootTaskKeys)

            return Completable.complete() // todo task fetch return after tasks are loaded
        }
    }
}