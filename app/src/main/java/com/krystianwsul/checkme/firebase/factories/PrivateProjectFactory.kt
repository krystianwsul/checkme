package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.records.project.OwnedProjectRecord
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PrivateProjectFactory(
    projectLoader: ProjectLoader<ProjectType.Private, PrivateOwnedProjectJson>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Private, PrivateOwnedProjectJson>,
    shownFactory: Instance.ShownFactory,
    domainDisposable: CompositeDisposable,
    rootTaskProvider: Project.RootTaskProvider,
    rootModelChangeManager: RootModelChangeManager,
    deviceDbInfo: () -> DeviceDbInfo,
) : ProjectFactory<ProjectType.Private, PrivateOwnedProjectJson>(
    projectLoader,
    initialProjectEvent,
    shownFactory,
    domainDisposable,
    rootTaskProvider,
    rootModelChangeManager,
    deviceDbInfo,
) {

    override fun newProject(
        projectRecord: OwnedProjectRecord<ProjectType.Private>,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        rootTaskProvider: Project.RootTaskProvider,
        rootModelChangeManager: RootModelChangeManager,
    ) = PrivateProject(
        projectRecord as PrivateOwnedProjectRecord,
        userCustomTimeProvider,
        rootTaskProvider,
        rootModelChangeManager,
    )
}