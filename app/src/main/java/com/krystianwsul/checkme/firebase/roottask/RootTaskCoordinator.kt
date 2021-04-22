package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.project.ProjectRecord
import io.reactivex.rxjava3.core.Completable

interface RootTaskCoordinator {

    fun getRootTasks(projectRecord: ProjectRecord<*>): Completable

    class Impl : RootTaskCoordinator {

        override fun getRootTasks(projectRecord: ProjectRecord<*>): Completable {
            return Completable.complete() // todo task fetch
        }
    }
}