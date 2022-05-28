package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

class SharedProjectFactory(
    projectLoader: ProjectLoader<ProjectType.Shared, JsonWrapper, SharedOwnedProjectRecord>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<SharedOwnedProjectRecord>,
    shownFactory: Instance.ShownFactory,
    domainDisposable: CompositeDisposable,
    rootTaskProvider: OwnedProject.RootTaskProvider,
    rootModelChangeManager: RootModelChangeManager,
    deviceDbInfo: () -> DeviceDbInfo,
) : ProjectFactory<ProjectType.Shared, JsonWrapper, SharedOwnedProjectRecord>(
    projectLoader,
    initialProjectEvent,
    shownFactory,
    domainDisposable,
    rootTaskProvider,
    rootModelChangeManager,
    deviceDbInfo,
) {

    override fun newProject(
        projectRecord: ProjectRecord<ProjectType.Shared>,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        rootTaskProvider: OwnedProject.RootTaskProvider,
        rootModelChangeManager: RootModelChangeManager,
    ) = SharedOwnedProject(
        projectRecord as SharedOwnedProjectRecord,
        userCustomTimeProvider,
        rootTaskProvider,
        rootModelChangeManager,
    ).apply {
        fixNotificationShown(shownFactory, ExactTimeStamp.Local.now)
        updateDeviceDbInfo(deviceDbInfo())
    }
}