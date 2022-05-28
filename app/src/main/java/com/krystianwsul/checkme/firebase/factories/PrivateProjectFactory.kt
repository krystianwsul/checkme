package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.project.PrivateOwnedProject
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PrivateProjectFactory(
    projectLoader: ProjectLoader<ProjectType.Private, PrivateOwnedProjectJson, PrivateOwnedProjectRecord>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<PrivateOwnedProjectRecord>,
    shownFactory: Instance.ShownFactory,
    domainDisposable: CompositeDisposable,
    rootTaskProvider: OwnedProject.RootTaskProvider,
    rootModelChangeManager: RootModelChangeManager,
    deviceDbInfo: () -> DeviceDbInfo,
) : ProjectFactory<ProjectType.Private, PrivateOwnedProjectJson, PrivateOwnedProjectRecord>(
    projectLoader,
    initialProjectEvent,
    shownFactory,
    domainDisposable,
    rootTaskProvider,
    rootModelChangeManager,
    deviceDbInfo,
) {

    override fun newProject(
        projectRecord: ProjectRecord<ProjectType.Private>,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        rootTaskProvider: OwnedProject.RootTaskProvider,
        rootModelChangeManager: RootModelChangeManager,
    ) = PrivateOwnedProject(
        projectRecord as PrivateOwnedProjectRecord,
        userCustomTimeProvider,
        rootTaskProvider,
        rootModelChangeManager,
    )
}