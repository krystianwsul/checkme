package com.krystianwsul.checkme.firebase

import com.krystianwsul.common.firebase.records.project.ProjectRecord
import io.reactivex.rxjava3.core.Completable

interface RootTaskCoordinator {

    fun getRootTasks(projectRecord: ProjectRecord<*>): Completable
}