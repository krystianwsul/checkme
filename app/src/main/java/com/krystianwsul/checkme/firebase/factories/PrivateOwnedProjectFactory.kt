package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.project.PrivateOwnedProject
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PrivateOwnedProjectFactory(
    projectLoader: ProjectLoader<ProjectType.Private, PrivateOwnedProjectJson, PrivateOwnedProjectRecord>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<PrivateOwnedProjectRecord>,
    domainDisposable: CompositeDisposable,
    private val rootTaskProvider: OwnedProject.RootTaskProvider,
    rootModelChangeManager: RootModelChangeManager,
) : OwnedProjectFactory<ProjectType.Private, PrivateOwnedProjectJson, PrivateOwnedProjectRecord>(
    projectLoader,
    initialProjectEvent,
    domainDisposable,
    rootModelChangeManager,
) {

    init {
        init()
    }

    override fun newProject(
        projectRecord: PrivateOwnedProjectRecord,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ) = PrivateOwnedProject(projectRecord, userCustomTimeProvider, rootTaskProvider, rootModelChangeManager)
}