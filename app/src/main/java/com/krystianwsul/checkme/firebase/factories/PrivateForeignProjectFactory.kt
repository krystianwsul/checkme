package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.firebase.json.projects.PrivateForeignProjectJson
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.PrivateForeignProject
import com.krystianwsul.common.firebase.records.project.PrivateForeignProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PrivateForeignProjectFactory(
    projectLoader: ProjectLoader<ProjectType.Private, PrivateForeignProjectJson, PrivateForeignProjectRecord>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<PrivateForeignProjectRecord>,
    domainDisposable: CompositeDisposable,
    rootModelChangeManager: RootModelChangeManager,
) : ForeignProjectFactory<ProjectType.Private, PrivateForeignProjectJson, PrivateForeignProjectRecord>(
    projectLoader,
    initialProjectEvent,
    domainDisposable,
    rootModelChangeManager,
) {

    init {
        init()
    }

    override fun newProject(
        projectRecord: PrivateForeignProjectRecord,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ) = PrivateForeignProject(projectRecord, userCustomTimeProvider, rootModelChangeManager)
}