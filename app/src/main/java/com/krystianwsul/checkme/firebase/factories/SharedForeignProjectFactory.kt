package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.firebase.models.project.SharedForeignProject
import com.krystianwsul.common.firebase.records.project.SharedForeignProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class SharedForeignProjectFactory(
    projectLoader: ProjectLoader<*, *, SharedForeignProjectRecord>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<SharedForeignProjectRecord>,
    domainDisposable: CompositeDisposable,
) : ForeignProjectFactory<ProjectType.Shared, SharedForeignProjectRecord>(
    projectLoader,
    initialProjectEvent,
    domainDisposable,
) {

    init {
        init()
    }

    override fun newProject(
        projectRecord: SharedForeignProjectRecord,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ) = SharedForeignProject(projectRecord, userCustomTimeProvider)
}